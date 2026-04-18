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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A thin animated progress bar used across the onboarding data-entry steps.
 * The fill width animates with the shared emphasized spring so advancing or
 * going back feels like one connected motion with the content transition.
 *
 * [progress] should be in the 0f..1f range. Render nothing when null.
 */
@Composable
fun OnboardingProgressBar(
    progress: Float?,
    modifier: Modifier = Modifier,
) {
    val target = progress?.coerceIn(0f, 1f) ?: 0f
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = OnboardingMotion.EmphasizedSpring,
        label = "onboarding-progress",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0x14000000)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
