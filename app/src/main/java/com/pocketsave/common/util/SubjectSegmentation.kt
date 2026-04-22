package com.pocketsave.common.util

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Android port of the iOS `SubjectMaskProcessor` in `SubjectMaskProcessor.swift`.
 * Uses Google ML Kit Subject Segmentation (Vision's counterpart on Android) to
 * lift the foreground subject from a user-captured photo, feathers the alpha
 * edge, crops to the subject bounds, and applies a Buldak-style white halo —
 * same visual language as the iOS stickers so the two platforms agree.
 *
 * ML Kit downloads the segmentation model (~10 MB) on first use. Until that
 * resolves, `extract` returns `null` and callers should fall back to the
 * original bitmap. The segmenter itself is safe to reuse across calls and
 * hangs on to its native resources between invocations — instantiate once per
 * [SubjectSegmenter] and share.
 */
object SubjectSegmenter {

    private val client by lazy {
        // `enableForegroundBitmap()` asks ML Kit to composite the subject into
        // an ARGB bitmap for us so we never touch the raw mask — skips the
        // per-pixel feathering / compositing loop we'd otherwise need.
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()
        SubjectSegmentation.getClient(options)
    }

    /**
     * Runs segmentation on [source] and returns a sticker (foreground with
     * alpha + a white outline halo + feathered edges), or `null` if the
     * model isn't ready, the call fails, or no foreground was found.
     *
     * Safe to call from a coroutine scope — suspends until ML Kit resolves.
     * Bitmap work happens on [Dispatchers.Default]; the ML Kit call marshals
     * to its own thread.
     */
    suspend fun extract(source: Bitmap): Bitmap? {
        val foreground = runCatching { segment(source) }.getOrNull() ?: return null
        return withContext(Dispatchers.Default) {
            val cropped = cropToAlphaBounds(foreground) ?: return@withContext null
            if (cropped !== foreground) foreground.recycle()
            val feathered = featherEdges(cropped)
            if (feathered !== cropped) cropped.recycle()
            val withOutline = applyStickerOutline(feathered)
            if (withOutline !== feathered) feathered.recycle()
            withOutline
        }
    }

    private suspend fun segment(source: Bitmap): Bitmap? = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(source, 0)
        client.process(image)
            .addOnSuccessListener { result ->
                cont.resume(result.foregroundBitmap)
            }
            .addOnFailureListener { err ->
                cont.resumeWithException(err)
            }
    }

    /**
     * Finds the tight bounding box of the opaque pixels in [source] and
     * returns a new bitmap cropped to those bounds. Returns `null` if the
     * foreground is empty (nothing above the alpha threshold). Mirrors
     * iOS `CGImage.alphaContentStats`.
     */
    private fun cropToAlphaBounds(source: Bitmap, alphaThreshold: Int = 12): Bitmap? {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1
        for (y in 0 until height) {
            val rowStart = y * width
            for (x in 0 until width) {
                val alpha = (pixels[rowStart + x] ushr 24) and 0xFF
                if (alpha <= alphaThreshold) continue
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
            }
        }
        if (maxX < minX || maxY < minY) return null

        // Give the crop a small breathing pad so the upcoming feather + outline
        // have somewhere to spread into without clipping at the sticker edge.
        val pad = max(2, (max(width, height) * 0.012f).toInt())
        val left = (minX - pad).coerceAtLeast(0)
        val top = (minY - pad).coerceAtLeast(0)
        val right = (maxX + 1 + pad).coerceAtMost(width)
        val bottom = (maxY + 1 + pad).coerceAtMost(height)
        return Bitmap.createBitmap(source, left, top, right - left, bottom - top)
    }

    /**
     * Softens hard mask edges by blurring the alpha channel and re-compositing
     * it onto the RGB. Direct port of iOS's CoreImage feather pass
     * (extract alpha → Gaussian blur → blend with alpha mask).
     *
     * [BlurMaskFilter] works on whatever is drawn with the paint; drawing the
     * source bitmap through it yields an alpha-blurred silhouette, which we
     * then use as the mask when re-compositing the original RGB on top.
     */
    private fun featherEdges(source: Bitmap): Bitmap {
        val longEdge = max(source.width, source.height)
        val radius = max(1.5f, longEdge * 0.008f)
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 1) Draw the source through a BlurMaskFilter — this produces a
        //    blurred-alpha silhouette of the subject, filling the layer's
        //    alpha channel with soft edges.
        val softenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawBitmap(source, 0f, 0f, softenPaint)

        // 2) Punch the original RGB back onto the soft silhouette. SRC_IN
        //    keeps the blurred alpha but replaces the color with the crisp
        //    original — exactly what `CIBlendWithAlphaMask` does on iOS.
        val composite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        canvas.drawBitmap(source, 0f, 0f, composite)
        return output
    }

    /**
     * Adds the white halo outline used by the iOS stickers. We draw the
     * subject's silhouette — tinted white — offset at 30 angular steps around
     * the origin, then lay the colored subject on top. The overlapping
     * silhouettes merge into a continuous rim.
     *
     * Canvas is expanded by `basePad` so the rim has room to bloom without
     * clipping at the original bitmap's edge.
     */
    private fun applyStickerOutline(source: Bitmap): Bitmap {
        val longEdge = max(source.width, source.height)
        val outlineWidth = max(6f, min(longEdge * 0.028f, 24f))
        val basePad = outlineWidth + max(4f, longEdge * 0.018f)

        val outWidth = (source.width + basePad * 2f).toInt()
        val outHeight = (source.height + basePad * 2f).toInt()
        val output = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // White-tinted silhouette paint: the color filter forces every opaque
        // pixel to white but preserves the source's own alpha, giving a clean
        // shadow-casting silhouette.
        val silhouettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            isFilterBitmap = true
        }
        for (step in 0 until OUTLINE_STEPS) {
            val angle = (step.toDouble() / OUTLINE_STEPS) * 2.0 * Math.PI
            val dx = (cos(angle) * outlineWidth).toFloat()
            val dy = (sin(angle) * outlineWidth).toFloat()
            canvas.drawBitmap(source, basePad + dx, basePad + dy, silhouettePaint)
        }

        // Colored original on top — the silhouettes become a rim around it.
        val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        canvas.drawBitmap(source, basePad, basePad, facePaint)
        return output
    }

    // Matches the iOS `outlineStepDegrees` constant: 30 steps of 12° each.
    private const val OUTLINE_STEPS = 30
}

/**
 * Convenience bounds type — same role as iOS's `AlphaContentStats.bounds`.
 * Kept internal to the module so it doesn't leak into public APIs.
 */
internal data class AlphaBounds(val bounds: Rect, val transparencyRatio: Float)
