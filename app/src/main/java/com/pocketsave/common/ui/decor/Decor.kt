package com.pocketsave.common.ui.decor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Canvas-drawn decorative primitives. All of them render in a single draw
 * pass via [drawBehind]/[drawWithCache] so they add character without
 * ballooning the frame cost.
 *
 * The point of this file: keep decoration buildable and on-brand. Every
 * shape here uses the existing token colors — no hardcoded hex outside
 * the token layer — so editing the palette ripples through decor too.
 */

/**
 * Soft editorial blob. Draws a single organic shape using a four-point
 * Bezier loop; the random seed lets callers vary the silhouette without the
 * blob flickering between recompositions (the seed stabilises the Path).
 *
 * Looks like a paint smudge. Use behind hero copy, in the corner of a card,
 * or inside a sticker badge — anywhere a flat tint would feel sterile.
 */
fun Modifier.blobDecor(
    color: Color,
    seed: Int = 0,
    alpha: Float = 1f,
): Modifier = drawWithCache {
    val w = size.width
    val h = size.height
    val rng = Random(seed)
    val cx = w / 2f + (rng.nextFloat() - 0.5f) * w * 0.08f
    val cy = h / 2f + (rng.nextFloat() - 0.5f) * h * 0.08f
    val rx = w * (0.42f + rng.nextFloat() * 0.06f)
    val ry = h * (0.42f + rng.nextFloat() * 0.06f)

    val path = Path().apply {
        val pts = 6
        val jitters = FloatArray(pts) { 0.86f + rng.nextFloat() * 0.22f }
        val firstAngle = 0.0
        // Starting point.
        moveTo(
            (cx + rx * jitters[0] * cos(firstAngle)).toFloat(),
            (cy + ry * jitters[0] * sin(firstAngle)).toFloat(),
        )
        for (i in 1..pts) {
            val a = firstAngle + (i * 2 * PI / pts)
            val prev = firstAngle + ((i - 1) * 2 * PI / pts)
            val mid = (prev + a) / 2.0
            val endJ = jitters[i % pts]
            val ctrlJ = jitters[i % pts] * 1.05
            val ex = (cx + rx * endJ * cos(a)).toFloat()
            val ey = (cy + ry * endJ * sin(a)).toFloat()
            val cxCtrl = (cx + rx * ctrlJ * cos(mid) * 1.08).toFloat()
            val cyCtrl = (cy + ry * ctrlJ * sin(mid) * 1.08).toFloat()
            quadraticBezierTo(cxCtrl, cyCtrl, ex, ey)
        }
        close()
    }

    onDrawBehind {
        drawPath(path = path, color = color.copy(alpha = color.alpha * alpha))
    }
}

/**
 * Grain overlay. Scatters tiny dots across the element at a given density.
 * Creates a papery, editorial texture underneath hero content.
 *
 * The dots are cached per-size so we don't re-randomise every frame, and
 * the seed keeps the pattern stable per composition.
 */
fun Modifier.grainOverlay(
    tint: Color,
    density: Float = 1f,
    dotRadius: Dp = 0.6.dp,
    seed: Int = 7,
): Modifier = drawWithCache {
    val w = size.width
    val h = size.height
    val areaPx = w * h
    val count = (areaPx / 900f * density).toInt().coerceIn(12, 3200)
    val rng = Random(seed)
    val points = Array(count) {
        Offset(rng.nextFloat() * w, rng.nextFloat() * h)
    }
    val r = dotRadius.toPx()
    onDrawBehind {
        points.forEach { p ->
            drawCircle(color = tint, radius = r, center = p)
        }
    }
}

/**
 * Hand-drawn underline swoosh. One lightly-curved stroke tucked under a
 * headline so the section heading feels authored instead of set.
 *
 * Keeps the stroke subtle — 2.5 dp by default — so it reads as a flourish,
 * not an affordance.
 */
@Composable
fun UnderlineSwoosh(
    color: Color,
    modifier: Modifier = Modifier,
    strokeDp: Dp = 2.5.dp,
) {
    Canvas(
        modifier = modifier
            .size(width = 44.dp, height = 10.dp),
    ) {
        val stroke = strokeDp.toPx()
        val path = Path().apply {
            moveTo(0f, size.height * 0.7f)
            cubicTo(
                size.width * 0.25f, size.height * 0.1f,
                size.width * 0.55f, size.height * 0.95f,
                size.width, size.height * 0.35f,
            )
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = stroke),
        )
    }
}

/**
 * Scatters a small set of decorative "confetti bits" inside the element's
 * bounds. Each bit is a tiny pill drawn in a supporting-hue colour. Use as
 * a STATIC decoration — a printed-confetti feel — not an animation. For the
 * animated reward burst see `CelebrationBurst`.
 */
fun Modifier.confettiStatic(
    tints: List<Color>,
    seed: Int = 11,
    bitCount: Int = 14,
): Modifier = drawWithCache {
    val w = size.width
    val h = size.height
    val rng = Random(seed)
    data class Bit(
        val center: Offset,
        val lenPx: Float,
        val angleRad: Float,
        val color: Color,
        val thickness: Float,
    )
    val bits = List(bitCount) {
        val len = 6f + rng.nextFloat() * 10f
        val thickness = 2.4f + rng.nextFloat() * 1.8f
        val angle = rng.nextFloat() * (2f * PI.toFloat())
        Bit(
            center = Offset(rng.nextFloat() * w, rng.nextFloat() * h),
            lenPx = len,
            angleRad = angle,
            color = tints[rng.nextInt(tints.size)],
            thickness = thickness,
        )
    }
    onDrawBehind {
        bits.forEach { bit ->
            val half = bit.lenPx / 2f
            val dx = cos(bit.angleRad) * half
            val dy = sin(bit.angleRad) * half
            val start = Offset(bit.center.x - dx, bit.center.y - dy)
            val end = Offset(bit.center.x + dx, bit.center.y + dy)
            drawLine(
                color = bit.color,
                start = start,
                end = end,
                strokeWidth = bit.thickness,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}

/**
 * Soft rounded hairline border drawn inside the composable's bounds. Used
 * by sticker chips to give them a paper-cut edge without a hard outline.
 */
fun Modifier.softHairline(
    color: Color,
    cornerRadius: Dp = 16.dp,
    strokeDp: Dp = 1.dp,
): Modifier = drawBehind {
    val stroke = strokeDp.toPx()
    val radius = cornerRadius.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2f, stroke / 2f),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(width = stroke),
    )
}
