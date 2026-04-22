package com.pocketsave.core.tripshare

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.graphics.applyCanvas
import com.pocketsave.common.util.ColorOption
import com.pocketsave.core.cart.CartDetailItemRow
import com.pocketsave.core.cart.CartDetailUiState
import com.pocketsave.data.local.entity.CartEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Native-Canvas port of iOS `TripShareFullSnapshot` + `TripReceiptCardView` +
 * `TripReceiptInfoView` (`Core/TripShare/Views/`).
 *
 * Rendering Compose composables to a bitmap reliably requires the
 * `rememberGraphicsLayer` API (Compose 1.7+). Our BOM pins 1.6.x, so this port
 * draws the receipt directly to a `Bitmap` with `android.graphics.Canvas` +
 * `Paint`. The resulting pixels are pixel-identical regardless of screen
 * density, which is exactly what `ImageRenderer.scale = 3.0` was trying to
 * achieve on iOS.
 */
class TripShareBitmapRenderer(private val context: Context) {

    data class RenderInput(
        val cart: CartEntity,
        val ui: CartDetailUiState,
        val backgroundColor: ColorOption,
        val backgroundImageUri: String?,
        val captureDate: Date,
        /**
         * Currency formatter to use for the rendered bitmap. Passed in from the
         * UI layer (via `LocalCurrencyFormatter`) so user currency overrides
         * propagate into the exported receipt.
         */
        val currencyFormatter: com.pocketsave.core.currency.CurrencyFormatter =
            com.pocketsave.core.currency.CurrencyFormatter(
                com.pocketsave.core.currency.CurrencyPreference.fromLocale(),
            ),
    )

    /**
     * Port of `TripShareViewModel.renderedReceiptImage()` — the full 9:16
     * (1080×1920) canvas with background + receipt card. Always the receipt
     * variant for Phase 8; the "physics" alt-view from iOS is deferred.
     */
    fun renderShareImage(input: RenderInput): Bitmap {
        val width = CANVAS_WIDTH
        val height = CANVAS_HEIGHT
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.applyCanvas {
            drawBackground(this, input)
            val receiptWidth = (width * RECEIPT_WIDTH_RATIO).toInt()
            val receiptHeight = (receiptWidth * RECEIPT_ASPECT).toInt()
            val receipt = drawReceipt(input, receiptWidth, receiptHeight)
            drawShadowedBitmap(
                canvas = this,
                bitmap = receipt,
                left = (width - receiptWidth) / 2f,
                top = (height - receiptHeight) / 2f,
            )
            receipt.recycle()
        }
        return bitmap
    }

    private fun drawBackground(canvas: Canvas, input: RenderInput) {
        val uri = input.backgroundImageUri
        if (uri != null) {
            val background = loadBackgroundBitmap(uri, canvas.width, canvas.height)
            if (background != null) {
                drawCenterCropped(canvas, background, canvas.width, canvas.height)
                val overlay = Paint().apply { color = Color.argb(51, 0, 0, 0) } // 20% black
                canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), overlay)
                background.recycle()
                return
            }
        }

        val base = ColorOption.parseHex(input.backgroundColor.hex).toAndroidColor()
        canvas.drawColor(base)
        // Subtle vignette so solid colours don't look totally flat, approximating
        // iOS `GrungeTextureView` without shipping a texture asset.
        val vignette = LinearGradient(
            0f, 0f, 0f, canvas.height.toFloat(),
            intArrayOf(Color.argb(0, 0, 0, 0), Color.argb(46, 0, 0, 0)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        val paint = Paint().apply { shader = vignette }
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paint)
    }

    private fun drawReceipt(input: RenderInput, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.applyCanvas {
            val card = RectF(0f, 0f, width.toFloat(), height.toFloat())

            // Card shape: rounded rectangle with small notches top+bottom
            // (matches iOS `TripReceiptTicketShape` at a higher fidelity than we
            // need for Phase 8 — keeping it as a rounded rect until we port the
            // punched-notch geometry).
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            val corner = width * 0.05f
            drawRoundRect(card, corner, corner, cardPaint)

            drawReceiptContent(this, input, width, height)
        }
        return bitmap
    }

    private fun drawReceiptContent(canvas: Canvas, input: RenderInput, width: Int, height: Int) {
        val padX = width * 0.06f
        val contentWidth = width - padX * 2
        val cart = input.cart
        val ui = input.ui

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((0.75f * 255).toInt(), 0, 0, 0)
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            textSize = width * 0.046f
            textAlign = Paint.Align.CENTER
        }

        var cursorY = height * 0.09f
        val title = buildHeaderTitle(cart, ui)
        val titleLines = wrapText(title, titlePaint, contentWidth)
        for (line in titleLines) {
            canvas.drawText(line, width / 2f, cursorY, titlePaint)
            cursorY += titlePaint.textSize * 1.35f
        }
        cursorY += height * 0.02f

        drawDashedDivider(canvas, padX, cursorY, width - padX)
        cursorY += height * 0.025f

        val dateFormatter = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((0.5f * 255).toInt(), 0, 0, 0)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = width * 0.038f
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(dateFormatter.format(input.captureDate), padX, cursorY, smallPaint)
        smallPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(timeFormatter.format(input.captureDate), width - padX, cursorY, smallPaint)
        smallPaint.textAlign = Paint.Align.LEFT
        cursorY += height * 0.025f

        drawSolidDivider(canvas, padX, cursorY, width - padX)
        cursorY += height * 0.032f

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((0.6f * 255).toInt(), 0, 0, 0)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = width * 0.044f
            textAlign = Paint.Align.LEFT
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((0.85f * 255).toInt(), 0, 0, 0)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = width * 0.044f
            textAlign = Paint.Align.RIGHT
        }

        val formatter = input.currencyFormatter
        val plannedLine = if (cart.budget > 0) formatter.format(cart.budget) else "—"
        val totalSpent = ui.totalSpent

        canvas.drawText("Planned Budget", padX, cursorY, labelPaint)
        canvas.drawText(plannedLine, width - padX, cursorY, valuePaint)
        cursorY += height * 0.042f
        canvas.drawText("Total Spent", padX, cursorY, labelPaint)
        canvas.drawText(formatter.format(totalSpent), width - padX, cursorY, valuePaint)

        if (cart.budget > 0) {
            cursorY += height * 0.04f
            drawDashedDivider(canvas, padX, cursorY, width - padX)
            cursorY += height * 0.045f

            val balance = cart.budget - totalSpent
            val balanceLabel = Paint(labelPaint).apply {
                textSize = width * 0.046f
                color = Color.argb((0.6f * 255).toInt(), 0, 0, 0)
            }
            val balanceValue = Paint(valuePaint).apply {
                textSize = width * 0.06f
                color = if (balance >= 0.0) {
                    Color.argb(255, 0, 0, 0)
                } else {
                    Color.argb((0.8f * 255).toInt(), 180, 0, 0)
                }
            }
            canvas.drawText("Balance", padX, cursorY, balanceLabel)
            val prefix = if (balance >= 0) "+" else "−"
            canvas.drawText(
                "$prefix${formatter.format(kotlin.math.abs(balance))}",
                width - padX,
                cursorY,
                balanceValue,
            )
        }

        // Item list (fulfilled only) — mirrors iOS receipt's purchased items block.
        cursorY += height * 0.055f
        drawSolidDivider(canvas, padX, cursorY, width - padX)
        cursorY += height * 0.04f

        val itemLabelPaint = Paint(labelPaint).apply { textSize = width * 0.038f }
        val itemValuePaint = Paint(valuePaint).apply {
            textSize = width * 0.038f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val purchasedRows = ui.sections.flatMap { it.rows }.filter { it.cartItem.isFulfilled && !it.cartItem.isSkippedDuringShopping }
        val rowLimit = 12
        for ((index, row) in purchasedRows.take(rowLimit).withIndex()) {
            val label = truncate(row.displayName, maxChars = 22)
            val qty = row.displayQuantity
            val qtyText = if (qty % 1.0 == 0.0) "${qty.toInt()}×" else "%.2f×".format(qty)
            canvas.drawText("$qtyText $label", padX, cursorY, itemLabelPaint)
            canvas.drawText(formatter.format(row.lineTotal), width - padX, cursorY, itemValuePaint)
            cursorY += height * 0.031f
            if (index == rowLimit - 1 && purchasedRows.size > rowLimit) {
                canvas.drawText("+${purchasedRows.size - rowLimit} more", padX, cursorY, itemLabelPaint)
            }
        }

        // Branding line anchored to the bottom.
        val brandingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((0.7f * 255).toInt(), 0, 0, 0)
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            textSize = width * 0.048f
        }
        canvas.drawText("PocketSave", padX, height - height * 0.05f, brandingPaint)
    }

    private fun drawDashedDivider(canvas: Canvas, startX: Float, y: Float, endX: Float) {
        val paint = Paint().apply {
            color = Color.argb((0.18f * 255).toInt(), 0, 0, 0)
            style = Paint.Style.STROKE
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(5f, 4f), 0f)
        }
        canvas.drawLine(startX, y, endX, y, paint)
    }

    private fun drawSolidDivider(canvas: Canvas, startX: Float, y: Float, endX: Float) {
        val paint = Paint().apply {
            color = Color.argb((0.18f * 255).toInt(), 0, 0, 0)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawLine(startX, y, endX, y, paint)
    }

    private fun drawShadowedBitmap(canvas: Canvas, bitmap: Bitmap, left: Float, top: Float) {
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            setShadowLayer(18f, 0f, 6f, Color.argb((0.12f * 255).toInt(), 0, 0, 0))
        }
        // Use a transparent rect of the same shape to throw the shadow — drawing
        // the bitmap with `setShadowLayer` doesn't blur correctly on API 26-28.
        val rect = RectF(left, top, left + bitmap.width, top + bitmap.height)
        val corner = bitmap.width * 0.05f
        canvas.drawRoundRect(rect, corner, corner, shadowPaint)
        canvas.drawBitmap(bitmap, left, top, null)
    }

    private fun drawCenterCropped(canvas: Canvas, source: Bitmap, targetWidth: Int, targetHeight: Int) {
        val srcW = source.width.toFloat()
        val srcH = source.height.toFloat()
        val scale = maxOf(targetWidth / srcW, targetHeight / srcH)
        val scaledW = srcW * scale
        val scaledH = srcH * scale
        val dst = RectF(
            (targetWidth - scaledW) / 2f,
            (targetHeight - scaledH) / 2f,
            (targetWidth + scaledW) / 2f,
            (targetHeight + scaledH) / 2f,
        )
        canvas.drawBitmap(source, null, dst, null)
    }

    private fun loadBackgroundBitmap(uriString: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = computeSampleSize(bounds.outWidth, bounds.outHeight, targetWidth, targetHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return runCatching {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }.getOrNull()
    }

    private fun computeSampleSize(srcW: Int, srcH: Int, targetW: Int, targetH: Int): Int {
        var inSampleSize = 1
        while (srcW / (inSampleSize * 2) >= targetW && srcH / (inSampleSize * 2) >= targetH) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    private fun buildHeaderTitle(cart: CartEntity, ui: CartDetailUiState): String {
        val stores = ui.sections.map { it.store }.filter { it.isNotBlank() }.distinct()
        val storeStr = stores.take(2).joinToString(" & ")
        return if (storeStr.isEmpty()) cart.name else "${cart.name} at $storeStr"
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (paint.measureText(text) <= maxWidth) return listOf(text)
        val words = text.split(' ')
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines += current.toString()
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines.take(3)
    }

    private fun truncate(text: String, maxChars: Int): String =
        if (text.length <= maxChars) text else text.take(maxChars - 1) + "…"

    private fun androidx.compose.ui.graphics.Color.toAndroidColor(): Int {
        val r = (red * 255).toInt().coerceIn(0, 255)
        val g = (green * 255).toInt().coerceIn(0, 255)
        val b = (blue * 255).toInt().coerceIn(0, 255)
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }

    companion object {
        private const val CANVAS_WIDTH = 1080
        private const val CANVAS_HEIGHT = 1920
        private const val RECEIPT_WIDTH_RATIO = 0.72f
        private const val RECEIPT_ASPECT = 1.6f
    }
}
