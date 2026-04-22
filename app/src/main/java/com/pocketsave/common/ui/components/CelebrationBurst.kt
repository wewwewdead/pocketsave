package com.pocketsave.common.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocketsave.common.ui.PocketSaveTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Rare, tasteful reward burst. Use ONLY for genuine milestones — finishing a
 * trip under budget, first vault add, etc. Plays a single short arc of soft
 * confetti pills, then is gone. Never loops.
 *
 * Pass an increasing [trigger] value to re-fire the burst; first
 * composition (trigger == 0) is intentionally a no-op so nothing flashes
 * on screen entry.
 */
@Composable
fun CelebrationBurst(
    trigger: Int,
    modifier: Modifier = Modifier,
    durationMs: Int = 1100,
    particleCount: Int = 26,
    seed: Int = 42,
    particleLen: Dp = 10.dp,
    particleThickness: Dp = 3.dp,
    spread: Float = 0.9f,
    tints: List<Color>? = null,
) {
    val pastels = PocketSaveTokens.pastels
    // Monotone sage confetti with a rare gold sparkle. The gold only
    // appears here — the celebration is the one moment where a second
    // hue is allowed, and even then just as a small seasoning.
    val palette = tints ?: listOf(
        pastels.mintDeep,
        pastels.lavenderDeep,
        pastels.butterDeep,
        pastels.rewardGold,
    )
    val progress = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            progress.snapTo(0f)
            progress.animateTo(
                1f,
                animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
            )
        }
    }

    // Seed derived from the incoming seed so particle vectors vary subtly
    // between firings without repeating identically.
    val particles = remember(trigger, particleCount, seed) {
        val rng = Random(seed + trigger)
        List(particleCount) {
            val angle = rng.nextFloat() * (2f * PI.toFloat())
            val magnitude = 0.6f + rng.nextFloat() * 0.4f
            Triple(angle, magnitude, palette[rng.nextInt(palette.size)])
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize(),
    ) {
        val p = progress.value
        if (p == 0f) return@Canvas
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = (minOf(size.width, size.height) / 2f) * spread
        val lenPx = particleLen.toPx()
        val thicknessPx = particleThickness.toPx()
        // Ease-out travel + fade in, then fade out.
        val travelFactor = 1f - (1f - p) * (1f - p)
        val fade = when {
            p < 0.2f -> p / 0.2f
            p > 0.75f -> ((1f - p) / 0.25f).coerceAtLeast(0f)
            else -> 1f
        }
        particles.forEach { (angle, magnitude, color) ->
            val r = maxR * magnitude * travelFactor
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle) - (maxR * 0.25f * travelFactor)
            val half = lenPx / 2f
            val dx = cos(angle) * half
            val dy = sin(angle) * half
            drawLine(
                color = color.copy(alpha = color.alpha * fade),
                start = Offset(x - dx, y - dy),
                end = Offset(x + dx, y + dy),
                strokeWidth = thicknessPx,
                cap = StrokeCap.Round,
            )
        }
    }
}
