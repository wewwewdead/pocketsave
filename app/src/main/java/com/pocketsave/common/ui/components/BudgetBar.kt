package com.pocketsave.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocketsave.common.ui.PocketSaveColors

/**
 * 3-stop gradient budget progress bar. iOS reference: the horizontal progress
 * strip shown at the top of CartDetailScreen and inside HomeCartRow.
 *
 * Gradient stops match the iOS tokens:
 *   0.0  →  BudgetSafe     (#98F476)
 *   0.6  →  BudgetWarning  (#F4B576)
 *   1.0  →  BudgetOver     (#F47676)
 *
 * When the progress value exceeds 1.0 (the user has spent more than the budget)
 * the whole bar is tinted red to communicate the overflow.
 *
 * @param progress 0..1+ — values over 1 are clamped for the fill width but the
 *        `overBudget` tint applies regardless.
 * @param trackBackground background behind the fill. Default is a translucent
 *        white so the bar reads on both colored and white surfaces.
 */
@Composable
fun BudgetBar(
    progress: Float,
    overBudget: Boolean = progress > 1f,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackBackground: Color = Color.White.copy(alpha = 0.5f),
) {
    val clamped = progress.coerceIn(0f, 1f)
    val fillGradient = Brush.horizontalGradient(
        0f to PocketSaveColors.BudgetSafe,
        0.6f to PocketSaveColors.BudgetWarning,
        1f to PocketSaveColors.BudgetOver,
    )

    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height))
            .background(trackBackground),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped)
                .background(fillGradient),
        )
        if (overBudget) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .background(PocketSaveColors.BudgetOver.copy(alpha = 0.3f)),
            )
        }
    }
}
