package com.pocketsave.core.onboarding.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.pocketsave.common.ui.PocketSaveTokens

/**
 * The onboarding progress bar. A fuller 6dp track with pill ends and a
 * sage→mint gradient fill so stepping through the flow feels like watching
 * a quiet little plant grow.
 */
@Composable
fun OnboardingProgressBar(
    progress: Float?,
    modifier: Modifier = Modifier,
) {
    val pastels = PocketSaveTokens.pastels
    val target = progress?.coerceIn(0f, 1f) ?: 0f
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = OnboardingMotion.EmphasizedSpring,
        label = "onboarding-progress",
    )
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            pastels.mintDeep,
        ),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(pastels.canvasTint),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(RoundedCornerShape(999.dp))
                .background(gradient),
        )
    }
}
