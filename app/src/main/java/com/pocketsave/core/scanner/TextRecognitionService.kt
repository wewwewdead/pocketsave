package com.pocketsave.core.scanner

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pocketsave.core.scanner.model.ScannedTextBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Thin wrapper around ML Kit Text Recognition (Latin script).
 *
 * The iOS equivalent is the `VNRecognizeTextRequest` that
 * `ProductScannerSheet` runs; both produce blocks with a transcript + bounding
 * box. We return block-level rectangles normalised against the source image
 * (matches iOS `Vision` output in `0..1` space) so the parser's scoring
 * heuristics stay untouched.
 *
 * Memory strategy: the OCR entry points downsample aggressively before handing
 * the bitmap to ML Kit. Product labels remain legible well below the full
 * sensor resolution, and ML Kit itself benefits from the smaller input.
 * Rotation is deferred to [InputImage.fromBitmap]'s second parameter rather
 * than being applied by a secondary [Bitmap.createBitmap] allocation, so the
 * capture path never holds two full-size bitmaps at once.
 */
class TextRecognitionService(@Suppress("UNUSED_PARAMETER") context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Runs OCR on a pre-decoded [bitmap]. The caller is responsible for the
     * bitmap's lifecycle; we do not recycle it.
     *
     * @param rotationDegrees Rotation to apply during recognition — must be
     *   one of 0, 90, 180, 270. ML Kit rotates internally; normalised bounding
     *   boxes use post-rotation dimensions.
     */
    suspend fun recognize(
        bitmap: Bitmap,
        rotationDegrees: Int = 0,
    ): List<ScannedTextBlock> = withContext(Dispatchers.IO) {
        val input = InputImage.fromBitmap(bitmap, normalizedRotation(rotationDegrees))
        val result = awaitRecognition(input)
        val sideways = rotationDegrees == 90 || rotationDegrees == 270
        val width = (if (sideways) bitmap.height else bitmap.width).toFloat().coerceAtLeast(1f)
        val height = (if (sideways) bitmap.width else bitmap.height).toFloat().coerceAtLeast(1f)
        result.toScannedBlocks(width, height)
    }

    /**
     * Gallery-picker entry. Decodes the URI into a downsampled bitmap under
     * our own control, reads EXIF orientation, then delegates to the bitmap
     * overload. The bitmap is recycled as soon as OCR returns; callers never
     * see it.
     *
     * Before: `InputImage.fromFilePath(context, uri)` decoded at native
     * resolution — a 12 MP gallery photo meant a ~48 MB transient bitmap
     * inside ML Kit. After: a ~12 MB downsampled bitmap lives only for the
     * duration of the recognize call.
     */
    suspend fun recognize(context: Context, uri: Uri): List<ScannedTextBlock> = withContext(Dispatchers.IO) {
        val (bitmap, rotation) = decodeDownsampledForOcr(context.contentResolver, uri)
            ?: return@withContext emptyList()
        try {
            recognize(bitmap, rotation)
        } finally {
            bitmap.recycle()
        }
    }

    private fun Text.toScannedBlocks(width: Float, height: Float): List<ScannedTextBlock> =
        textBlocks.map { block ->
            val box = block.boundingBox
            val bounds = if (box != null) {
                ScannedTextBlock.Bounds(
                    left = box.left / width,
                    top = box.top / height,
                    right = box.right / width,
                    bottom = box.bottom / height,
                )
            } else {
                ScannedTextBlock.Bounds.FULL
            }
            ScannedTextBlock(transcript = block.text, bounds = bounds)
        }

    private suspend fun awaitRecognition(input: InputImage): Text =
        suspendCancellableCoroutine { cont ->
            recognizer.process(input)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    private fun normalizedRotation(rotationDegrees: Int): Int = when {
        rotationDegrees == 0 || rotationDegrees == 90 ||
            rotationDegrees == 180 || rotationDegrees == 270 -> rotationDegrees
        else -> 0
    }

    /**
     * Two-pass decode: a bounds read to pick the sample size, then a single
     * allocation at the downsampled size. EXIF is read from a third, cheap
     * stream since the bounds/decode streams can't be rewound.
     */
    private fun decodeDownsampledForOcr(
        resolver: ContentResolver,
        source: Uri,
    ): Pair<Bitmap, Int>? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: return null
        val w = bounds.outWidth
        val h = bounds.outHeight
        if (w <= 0 || h <= 0) return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = computeInSampleSize(w, h, OCR_TARGET_MAX_DIMENSION)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = resolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return null

        val rotation = runCatching {
            resolver.openInputStream(source)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        }.getOrDefault(0)

        return bitmap to rotation
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
        /**
         * Target long-edge resolution handed to ML Kit. Product labels remain
         * fully legible to the Latin recognizer at 1600 px; capturing at the
         * full sensor size (often 4000+ px) just wastes memory and CPU on the
         * OCR side.
         */
        private const val OCR_TARGET_MAX_DIMENSION = 1600
    }
}
