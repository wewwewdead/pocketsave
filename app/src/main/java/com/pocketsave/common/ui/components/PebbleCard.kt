package com.pocketsave.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocketsave.common.ui.AppShapes
import com.pocketsave.common.ui.CardShadowColor
import com.pocketsave.common.ui.pressScale

/**
 * Reusable surface with pebble geometry + soft shadow + optional press scale.
 * Abstracts the "card that reacts to taps" pattern that every Home section
 * currently open-codes, so component code stops repeating 12 lines of
 * shadow/shape/clickable wiring.
 *
 * When [onClick] is null the surface behaves as a static card (no press
 * feedback and no clickable modifier, so talkback won't surface it as a
 * tap target).
 */
@Composable
fun PebbleSurface(
    modifier: Modifier = Modifier,
    shape: Shape = AppShapes.SoftCard,
    color: Color,
    elevation: Dp = 10.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    var chain = modifier
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = CardShadowColor,
            spotColor = CardShadowColor,
        )
        .clip(shape)
        .background(color)
    if (onClick != null) {
        chain = chain
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
    }
    androidx.compose.foundation.layout.Box(modifier = chain, content = { content() })
}
