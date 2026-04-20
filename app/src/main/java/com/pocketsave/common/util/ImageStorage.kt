package com.pocketsave.common.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * File-backed replacement for the iOS `@Attribute(.externalStorage) image: Data?`
 * storage. Bitmaps are downsampled + written to app-private storage; the DB
 * holds a `file://…` string per [ItemEntity.imageUri] / `CartItemEntity.shoppingOnlyImageUri`.
 *
 * Keeping the bytes off-row matches the iOS approach (external storage) and
 * keeps Room-serialised rows small enough to copy cheaply in snapshots.
 */
class ImageStorage(private val context: Context) {

    private val rootDir: File by lazy {
        File(context.filesDir, DIRECTORY_NAME).apply { if (!exists()) mkdirs() }
    }

    private val captureDir: File by lazy {
        File(context.cacheDir, CAPTURE_DIRECTORY_NAME).apply { if (!exists()) mkdirs() }
    }

    /**
     * Copies the image behind [source] into app-private storage, downsampled to
     * [maxDimension] on the long edge to avoid persisting huge bitmaps. Returns
     * the stored file's URI as a String suitable for [android.net.Uri.parse].
     *
     * Runs on [Dispatchers.IO] so large bitmap decode + JPEG compression never
     * blocks the main thread. Any failure (OOM, IO, SecurityException on
     * cross-process content URIs) is swallowed into a null return so callers
     * can surface a user-visible error rather than a silent coroutine death.
     */
    suspend fun saveFromUri(source: Uri, maxDimension: Int = DEFAULT_MAX_DIMENSION): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver: ContentResolver = context.contentResolver
                val bitmap = decodeDownsampled(resolver, source, maxDimension)
                    ?: return@runCatching null
                writeBitmap(bitmap)
            }.getOrNull()
        }

    /**
     * Decodes [source], lifts the foreground subject via ML Kit Subject
     * Segmentation, and persists the sticker (transparent background +
     * feathered edge + white halo outline) as a PNG. Falls back to the plain
     * JPEG path from [saveFromUri] if segmentation fails or the device can't
     * reach Play Services to download the model — callers never have to
     * juggle "sticker or original," they just get the best available image.
     *
     * Pipeline matches the iOS `SubjectMaskProcessor.preparePreviewAssets`
     * sequence: decode → EXIF rotate → downsample → segment → crop to
     * subject bounds → feather → outline → PNG. JPEG's 4:2:0 chroma would
     * band the sticker's halo so we switch to PNG for this path only.
     */
    suspend fun saveSubjectSticker(
        source: Uri,
        maxDimension: Int = DEFAULT_MAX_DIMENSION,
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver: ContentResolver = context.contentResolver
            val bitmap = decodeDownsampled(resolver, source, maxDimension)
                ?: return@runCatching null
            val sticker = SubjectSegmenter.extract(bitmap)
            if (sticker == null) {
                // Fallback to JPEG save of the plain decoded bitmap so the
                // user still gets *something* when segmentation isn't
                // available (model not downloaded yet, flat image, etc.).
                val uri = writeBitmap(bitmap)
                bitmap.recycle()
                return@runCatching uri
            }
            bitmap.recycle()
            val uri = writeBitmapPng(sticker)
            sticker.recycle()
            uri
        }.getOrNull()
    }

    /**
     * Allocates a fresh capture target in app-private cache storage and returns
     * both the file (for later deletion) and a content:// Uri exposed via the
     * app's FileProvider so [ActivityResultContracts.TakePicture] can hand the
     * URI to the system camera app.
     */
    fun createCaptureFile(): CaptureTarget {
        val file = File(captureDir, "${UUID.randomUUID()}.jpg")
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        return CaptureTarget(file = file, uri = uri)
    }

    fun deleteByUri(uriString: String?) {
        if (uriString.isNullOrBlank()) return
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return
        val path = uri.path ?: return
        val file = File(path)
        if (file.exists() && file.parentFile == rootDir) file.delete()
    }

    /** Temp capture files live in cacheDir/captures/; delete once copied. */
    fun deleteCaptureFile(file: File?) {
        if (file != null && file.exists() && file.parentFile == captureDir) file.delete()
    }

    /**
     * Wipes every persisted image and pending capture. Used by the Reset-App
     * flow so the filesystem footprint matches the freshly-cleared database.
     * IO-bound, so callers dispatch to the appropriate scope.
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        rootDir.listFiles()?.forEach { runCatching { it.delete() } }
        captureDir.listFiles()?.forEach { runCatching { it.delete() } }
    }

    data class CaptureTarget(val file: File, val uri: Uri)

    private fun writeBitmap(bitmap: Bitmap): String {
        val file = File(rootDir, "${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        return Uri.fromFile(file).toString()
    }

    // PNG path — preserves alpha, needed for subject-segmented stickers. We
    // don't pass a quality value because PNG is lossless; the arg is ignored.
    private fun writeBitmapPng(bitmap: Bitmap): String {
        val file = File(rootDir, "${UUID.randomUUID()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return Uri.fromFile(file).toString()
    }

    private fun decodeDownsampled(
        resolver: ContentResolver,
        source: Uri,
        maxDimension: Int,
    ): Bitmap? {
        // Pass 1: fill outWidth/outHeight without allocating pixels.
        // `decodeStream` with inJustDecodeBounds always returns a null Bitmap by
        // design — we only care whether the stream opened and whether bounds
        // came back populated.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(source) ?: return null
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }

        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) return null

        val sampleSize = computeInSampleSize(width, height, maxDimension)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val raw = resolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return null

        val rotated = applyExifRotation(resolver, source, raw)
        return scaleToMax(rotated, maxDimension)
    }

    private fun applyExifRotation(resolver: ContentResolver, source: Uri, bitmap: Bitmap): Bitmap {
        val rotation = runCatching {
            resolver.openInputStream(source)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)

        if (rotation == 0f) return bitmap
        val matrix = android.graphics.Matrix().apply { postRotate(rotation) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun scaleToMax(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longEdge = maxOf(bitmap.width, bitmap.height)
        if (longEdge <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / longEdge
        val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun computeInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        var w = width
        var h = height
        while (w / 2 >= maxDimension && h / 2 >= maxDimension) {
            w /= 2
            h /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    companion object {
        private const val DIRECTORY_NAME = "item_images"
        private const val CAPTURE_DIRECTORY_NAME = "captures"
        private const val JPEG_QUALITY = 88
        private const val DEFAULT_MAX_DIMENSION = 1024
    }
}
