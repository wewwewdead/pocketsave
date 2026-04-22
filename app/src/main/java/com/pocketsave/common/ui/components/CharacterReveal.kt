package com.pocketsave.common.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.pocketsave.common.ui.PocketSaveColors

/**
 * Per-character reveal animation. iOS reference:
 * Components/CharacterRevealView.swift.
 *
 * Characters fade + slide up into place one by one (10ms stagger), matching the
 * hand-drawn savings amount + category headings on iOS. When [showsUnderline]
 * is true, an animated underline sweeps in left → right at the same pace.
 *
 * If the [text] value changes, the reveal restarts when [animateOnChange]
 * is true.
 */
@Composable
fun CharacterReveal(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit,
    color: Color = PocketSaveColors.DarkPrimary,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight = FontWeight.Normal,
    showsUnderline: Boolean = true,
    underlineColor: Color = PocketSaveColors.TextSubtle,
    underlineThickness: Dp = 1.dp,
    staggerMs: Int = 10,
    initialDelayMs: Int = 0,
    animateOnChange: Boolean = false,
) {
    val characters = remember(text) { text.toList() }
    var revealedCount by remember(text, animateOnChange) { mutableIntStateOf(0) }

    // Start the staggered reveal after [initialDelayMs]. Each tick reveals one
    // more character until all are shown. Using `delay` inside LaunchedEffect
    // rather than animate*AsState keeps each character's spring independent
    // (the iOS version uses one interpolatingSpring per character).
    LaunchedEffect(text) {
        if (initialDelayMs > 0) delay(initialDelayMs.toLong())
        while (revealedCount < characters.size) {
            revealedCount += 1
            delay(staggerMs.toLong())
        }
    }

    val underlineProgress by animateFloatAsState(
        targetValue = if (revealedCount >= characters.size) 1f else revealedCount / characters.size.coerceAtLeast(1).toFloat(),
        animationSpec = tween(
            durationMillis = (characters.size * staggerMs + 320).coerceAtLeast(160),
            easing = FastOutSlowInEasing,
        ),
        label = "characterRevealUnderline",
    )

    Column(modifier) {
        Row {
            characters.forEachIndexed { index, c ->
                RevealedChar(
                    char = c,
                    isVisible = index < revealedCount,
                    fontSize = fontSize,
                    color = color,
                    fontFamily = fontFamily,
                    fontWeight = fontWeight,
                )
            }
        }
        if (showsUnderline) {
            // Underline fills the text width proportionally to revealed progress.
            // Drawn under the text baseline — subtle, matches iOS subtle stroke.
            Box(
                Modifier
                    .fillMaxRevealWidth(underlineProgress)
                    .height(underlineThickness)
                    .drawBehind {
                        drawRect(underlineColor)
                    },
            )
        }
    }
}

@Composable
private fun RevealedChar(
    char: Char,
    isVisible: Boolean,
    fontSize: TextUnit,
    color: Color,
    fontFamily: FontFamily?,
    fontWeight: FontWeight,
) {
    val offsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 8f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "charOffset",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "charAlpha",
    )
    androidx.compose.material3.Text(
        text = char.toString(),
        style = TextStyle(
            fontSize = fontSize,
            color = color,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
        ),
        modifier = Modifier.graphicsLayer {
            translationY = offsetY
            this.alpha = alpha
        },
    )
}

/**
 * Grows the modifier's width proportionally to [progress]. Used by the reveal
 * underline so the stroke sweeps in from the left rather than fading.
 */
private fun Modifier.fillMaxRevealWidth(progress: Float): Modifier =
    this.then(
        Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val targetWidth = (placeable.width * progress.coerceIn(0f, 1f)).toInt()
            layout(targetWidth, placeable.height) {
                placeable.placeRelative(0, 0)
            }
        },
    )
