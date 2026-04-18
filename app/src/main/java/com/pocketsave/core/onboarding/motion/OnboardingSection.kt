package com.pocketsave.core.onboarding.motion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * Staggered-entry wrapper. Hosts its content inside an [AnimatedVisibility]
 * that flips to true after [delayMs]. Gives every onboarding screen a calm
 * "sections settle into place" feeling without per-screen bookkeeping.
 *
 * Only animates on first composition — subsequent recomposes keep the content
 * visible without re-running the entry animation.
 */
@Composable
fun OnboardingSection(
    delayMs: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMs > 0) delay(delayMs.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(340, easing = LinearOutSlowInEasing)) +
            slideInVertically(
                animationSpec = tween(420, easing = FastOutSlowInEasing),
            ) { full -> (full * 0.12f).toInt() },
        modifier = modifier,
    ) {
        content()
    }
}
