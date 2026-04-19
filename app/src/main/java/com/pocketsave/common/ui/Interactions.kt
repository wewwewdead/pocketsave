package com.pocketsave.common.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale

/**
 * Shared directional press-scale. Pressing down snaps firmly (high damping,
 * high stiffness) so the target reacts instantly; releasing overshoots and
 * settles so the tile "pops" back up. Picked up by every pressable primitive
 * so taps feel tactile across the whole app.
 *
 * Mirrors [com.pocketsave.core.home.pressScale] which existed first in the
 * Home module. Lives here so common primitives can reach it without a
 * home → common dependency inversion.
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
