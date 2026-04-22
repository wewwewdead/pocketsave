package com.pocketsave.common.ui.shapes

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Scalloped top + bottom ticket shape. Port of iOS
 * `TripReceiptTicketShape.swift` (Core/TripShare/Views/).
 *
 * Used for:
 *  - Trip Share receipt card
 *  - Vault item detail receipt overlay
 *  - Onboarding "New Item" receipt container
 *
 * The top and bottom edges of the rectangle are replaced by a row of tangent
 * semicircle notches cut into the paper — the classic torn-ticket look.
 *
 * @param notchRadius radius of each half-circle scallop (default 3.5dp as iOS).
 * @param flatWidth gap between adjacent scallops (default 2dp as iOS).
 */
class ReceiptTicketShape(
    private val notchRadius: Dp = 3.5.dp,
    private val flatWidth: Dp = 2.dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val r = with(density) { notchRadius.toPx() }
        val c = with(density) { flatWidth.toPx() }

        val unit = r * 2 + c
        val count = max(1, ((size.width + c) / unit).toInt())
        val usedW = count * (r * 2) + (count - 1) * c
        val startX = max(0f, (size.width - usedW) / 2f)

        val path = Path().apply {
            moveTo(startX, 0f)
            // Top edge — row of semicircular bites cut into the paper.
            for (i in 0 until count) {
                val sx = startX + i * (r * 2 + c)
                arcTo(
                    rect = Rect(
                        left = sx,
                        top = -r,
                        right = sx + r * 2,
                        bottom = r,
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = -180f,
                    forceMoveTo = false,
                )
                if (i < count - 1) {
                    lineTo(sx + r * 2 + c, 0f)
                }
            }
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)

            // Bottom edge — mirror of top.
            lineTo(startX + usedW, size.height)
            for (i in count - 1 downTo 0) {
                val sx = startX + i * (r * 2 + c)
                arcTo(
                    rect = Rect(
                        left = sx,
                        top = size.height - r,
                        right = sx + r * 2,
                        bottom = size.height + r,
                    ),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = -180f,
                    forceMoveTo = false,
                )
                if (i > 0) {
                    lineTo(sx - c, size.height)
                }
            }
            lineTo(0f, size.height)
            lineTo(0f, 0f)
            close()
        }

        return Outline.Generic(path)
    }
}

/** Convenience: pre-built shape at default iOS notch dimensions. */
val DefaultReceiptTicketShape: Shape = ReceiptTicketShape()
