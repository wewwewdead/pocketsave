package com.pocketsave.core.scanner

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
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
 */
class TextRecognitionService(@Suppress("UNUSED_PARAMETER") context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Runs OCR on [bitmap] and returns block-level transcripts + normalised bounds. */
    suspend fun recognize(bitmap: Bitmap): List<ScannedTextBlock> = withContext(Dispatchers.IO) {
        val input = InputImage.fromBitmap(bitmap, 0)
        val result = awaitRecognition(input)
        val width = bitmap.width.toFloat().coerceAtLeast(1f)
        val height = bitmap.height.toFloat().coerceAtLeast(1f)

        result.textBlocks.map { block ->
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
    }

    /** Convenience entry for URI-backed images (gallery picker). */
    suspend fun recognize(context: Context, uri: Uri): List<ScannedTextBlock> = withContext(Dispatchers.IO) {
        val input = InputImage.fromFilePath(context, uri)
        val result = awaitRecognition(input)
        val width = input.width.coerceAtLeast(1).toFloat()
        val height = input.height.coerceAtLeast(1).toFloat()
        result.textBlocks.map { block ->
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
    }

    private suspend fun awaitRecognition(input: InputImage): com.google.mlkit.vision.text.Text =
        suspendCancellableCoroutine { cont ->
            recognizer.process(input)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
}
