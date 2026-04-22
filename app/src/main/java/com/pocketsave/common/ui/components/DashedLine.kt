package com.pocketsave.common.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocketsave.common.ui.PocketSaveColors

/**
 * Horizontal dashed line. iOS reference: Components/DashedLine.swift.
 *
 * Used for: receipt rows (dotted leaders between label and value), side menu
 * dividers, receipt tear edges (when set to `vertical = false` with longer dashes),
 * and the divider in the currency menu (MenuView.swift:152).
 *
 * Defaults mirror iOS: lineWidth=1, dash=[8,4].
 */
@Composable
fun DashedLine(
    modifier: Modifier = Modifier,
    color: Color = PocketSaveColors.DarkPrimary.copy(alpha = 0.22f),
    strokeWidth: Dp = 1.dp,
    dashOn: Float = 8f,
    dashOff: Float = 4f,
) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(strokeWidth.coerceAtLeast(1.dp)),
    ) {
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
            strokeWidth = strokeWidth.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(dashOn, dashOff),
                0f,
            ),
        )
    }
}

/**
 * Dotted leader — wider spacing than DashedLine, used inside receipt rows
 * between a label on the left and a value on the right. Use in a Row with
 * the leader on `Modifier.weight(1f)`.
 */
@Composable
fun DottedLeader(
    modifier: Modifier = Modifier,
    color: Color = PocketSaveColors.DarkPrimary.copy(alpha = 0.35f),
) {
    Canvas(modifier.fillMaxWidth().height(1.dp)) {
        val dot = 1.5f
        val gap = 3f
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
            strokeWidth = dot,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dot, gap), 0f),
        )
    }
}

/**
 * Unused stroke style constant kept handy for call-sites that want to build
 * dashed outlines on their own paths.
 */
fun dashedStroke(width: Dp) = Stroke(
    width = width.value,
    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f),
)
