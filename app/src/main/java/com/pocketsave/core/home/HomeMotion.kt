package com.pocketsave.core.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import com.pocketsave.common.ui.Motion
import com.pocketsave.common.ui.PocketSaveSprings
import kotlinx.coroutines.delay

/**
 * Duolingo-flavored entrance: the tile fades in quickly, scales up from 88%
 * with a bouncy spring, and slides up a few percent of its height. The
 * combination makes the element feel like it "lands" on the canvas instead
 * of fading in. Alpha still uses a tween because springing alpha reads as
 * jittery — we want a clean fade, just the geometry should have character.
 */
private fun entranceEnter(): EnterTransition =
    fadeIn(
        animationSpec = tween(
            durationMillis = 220,
            easing = LinearOutSlowInEasing,
        ),
    ) + scaleIn(
        initialScale = 0.88f,
        animationSpec = PocketSaveSprings.Bouncy,
    ) + slideInVertically(
        initialOffsetY = { full -> (full * 0.18f).toInt() },
        animationSpec = PocketSaveSprings.BouncySlide,
    )

/**
 * Staggered entrance wrapper. The stagger is driven by [LaunchedEffect] +
 * `delay` rather than baked into the animation spec so the springs above
 * start from rest (spring specs ignore delayMillis). Each Entrance gets its
 * own visibility flag, so re-entering a LazyColumn item replays the motion
 * — subtle "alive" feel when scrolling up and back down.
 */
@Composable
fun Entrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index.coerceAtLeast(0) * Motion.EntranceStaggerMs).toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = entranceEnter(),
        modifier = modifier,
    ) {
        content()
    }
}

/**
 * Directional press-scale. Pressing down snaps firmly (high damping, high
 * stiffness) so the tile reacts instantly to the finger; releasing over-
 * shoots and settles (low damping, medium stiffness) so the tile "pops"
 * back up with a tiny bounce. This two-phase character is what makes
 * Duolingo-style taps feel tactile instead of mechanical.
 *
 * Uses an [Animatable] + [LaunchedEffect] rather than `animateFloatAsState`
 * because we need a *different* spec per direction — and Animatable lets us
 * swap specs without reconfiguring the state.
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.94f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val pressAnim = remember { Animatable(1f) }
    LaunchedEffect(pressed) {
        if (pressed) {
            pressAnim.animateTo(pressedScale, animationSpec = PocketSaveSprings.Snap)
        } else {
            pressAnim.animateTo(1f, animationSpec = PocketSaveSprings.PopBack)
        }
    }
    scale(pressAnim.value)
}
