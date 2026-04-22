package com.pocketsave.core.onboarding.motion

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Motion tokens and transition factories for onboarding. One place to tune the
 * whole flow so every screen exits and enters with the same rhythm.
 *
 * We lean on short slides (~12% of content width), a soft scale, and gentle
 * fade overlaps so transitions read as a single connected journey instead of
 * a hard cut between pages.
 */
object OnboardingMotion {
    const val FadeInMs = 320
    const val FadeOutMs = 240
    const val SlideMs = 380

    /** Primary non-bouncy spring used for content size + celebratory pulses. */
    val EmphasizedSpring = spring<Float>(
        dampingRatio = 0.88f,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Calm spring used for content-size adjustments when validation errors appear. */
    val ContentSizeSpring = spring<androidx.compose.ui.unit.IntSize>(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMediumLow,
    )
}

/**
 * Step transition for [androidx.compose.animation.AnimatedContent]. Short slide
 * + fade + subtle scale, directional based on whether the user is moving
 * forward or back through the flow.
 */
@OptIn(ExperimentalAnimationApi::class)
fun onboardingStepTransition(forward: Boolean): ContentTransform {
    val sign = if (forward) 1 else -1

    val enter = slideInHorizontally(
        animationSpec = tween(OnboardingMotion.SlideMs, easing = FastOutSlowInEasing),
    ) { full -> (sign * full * 0.12f).toInt() } +
        fadeIn(tween(OnboardingMotion.FadeInMs, delayMillis = 40, easing = LinearOutSlowInEasing)) +
        scaleIn(
            initialScale = 0.985f,
            animationSpec = tween(OnboardingMotion.SlideMs, easing = FastOutSlowInEasing),
        )

    val exit = slideOutHorizontally(
        animationSpec = tween(OnboardingMotion.SlideMs, easing = FastOutSlowInEasing),
    ) { full -> (-sign * full * 0.08f).toInt() } +
        fadeOut(tween(OnboardingMotion.FadeOutMs, easing = FastOutLinearInEasing)) +
        scaleOut(
            targetScale = 1.015f,
            animationSpec = tween(OnboardingMotion.SlideMs, easing = FastOutSlowInEasing),
        )

    return enter togetherWith exit
}

/**
 * Single-shot celebratory pulse. Pass a monotonically-increasing [trigger] —
 * each change fires one scale-up then settle. First composition (trigger == 0)
 * is intentionally a no-op so screens don't flash on entry.
 */
fun Modifier.onboardingCelebrationPulse(trigger: Int): Modifier = composed {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            scale.animateTo(
                targetValue = 1.035f,
                animationSpec = tween(140, easing = FastOutSlowInEasing),
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 220f),
            )
        }
    }
    graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
