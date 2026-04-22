package com.pocketsave.common.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Android side of iOS `TripShareViewModel.presentSystemShareSheet` +
 * `saveToAlbum`.
 *
 * - `shareBitmap` writes the bitmap to `cacheDir/trip_shares/…png`, hands back a
 *   content URI via [FileProvider], and builds an `ACTION_SEND` chooser intent.
 * - `saveBitmapToGallery` uses `MediaStore.Images` so the image lands in the
 *   user's Pictures/PocketSave album without needing `WRITE_EXTERNAL_STORAGE`
 *   on API 29+. On API 26–28 we fall back to the classic
 *   `Environment.DIRECTORY_PICTURES` + MediaStore insert.
 */
object ShareHelper {

    private const val SHARES_DIR = "trip_shares"
    private const val ALBUM_NAME = "PocketSave"

    fun shareBitmap(context: Context, bitmap: Bitmap, subject: String? = null): Intent {
        val uri = writeToShareCache(context, bitmap)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (subject != null) putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newRawUri(null, uri)
        }
        return Intent.createChooser(send, subject ?: "Share")
    }

    /**
     * Writes [bitmap] to cache and returns the FileProvider URI. Returned URIs
     * are valid for the chooser flow and are automatically cleaned up as the
     * OS reclaims cache space.
     */
    fun writeToShareCache(context: Context, bitmap: Bitmap): Uri {
        val dir = File(context.cacheDir, SHARES_DIR).apply { if (!exists()) mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    /**
     * Port of iOS `UIImageWriteToSavedPhotosAlbum`. Returns `true` on success.
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, displayName: String = "pocketsave-trip"): Boolean {
        val fileName = "$displayName-${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM_NAME")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val uri = resolver.insert(collection, values) ?: return false
        return runCatching {
            resolver.openOutputStream(uri)?.use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    throw IllegalStateException("Bitmap compress failed")
                }
            } ?: throw IllegalStateException("Null output stream for $uri")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
            }
            true
        }.getOrElse {
            resolver.delete(uri, null, null)
            false
        }
    }
}
