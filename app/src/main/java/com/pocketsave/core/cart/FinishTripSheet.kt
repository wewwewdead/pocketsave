package com.pocketsave.core.cart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pocketsave.common.ui.AppShapes
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.common.ui.components.CelebrationBurst
import com.pocketsave.common.ui.decor.UnderlineSwoosh
import com.pocketsave.core.haptics.AppHaptic
import com.pocketsave.core.haptics.rememberAppHaptics
import com.pocketsave.core.service.VaultService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Finish-trip confirmation. The hero is a semicircular tick gauge that sweeps
 * from empty to the user's actual spend percent, then the numeric labels
 * stagger in after the arc settles. The burst is rare by design — only a
 * genuine completion earns it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishTripSheet(
    viewModel: CartDetailViewModel,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val pastels = PocketSaveTokens.pastels
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val formatter = com.pocketsave.core.currency.LocalCurrencyFormatter.current
    val scope = rememberCoroutineScope()
    val haptics = rememberAppHaptics()

    val ui by viewModel.uiState.collectAsState()

    var insights by remember { mutableStateOf<VaultService.CartInsights?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var celebrationTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { insights = viewModel.loadInsights() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Box(modifier = Modifier.fillMaxWidth()) {
            CelebrationBurst(trigger = celebrationTrigger)

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "wrap it up",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.9.sp,
                        ),
                        color = pastels.peachDeep,
                    )
                    Spacer(Modifier.width(6.dp))
                    UnderlineSwoosh(color = pastels.peachDeep.copy(alpha = 0.55f))
                }
                Text(
                    text = "Finish this trip",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = pastels.inkBerry,
                )
                Text(
                    text = "We'll tuck today's actual prices into your vault so it stays accurate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val data = insights
                if (data == null) {
                    Text(
                        text = "Gathering the numbers…",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    GaugeHeroCard(
                        spent = data.actualTotal,
                        budget = ui.budget,
                        formatter = formatter,
                    )
                    if (data.priceChanges.isNotEmpty()) {
                        Text(
                            text = "Price moves (${data.priceChanges.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                        ) {
                            items(data.priceChanges, key = { it.itemName + it.plannedPrice + it.actualPrice }) { change ->
                                PriceChangeRow(change = change, formatter = formatter)
                                HorizontalDivider(color = pastels.hairline)
                            }
                        }
                    }

                    // "Trophy shelf" — every fulfilled item contributes its
                    // subject-segmented sticker to a gently bobbing row at
                    // the bottom of the sheet. Drops in after the gauge beat
                    // so it reads as a reward, not a data table. Unchecked
                    // items don't qualify: if you didn't buy it, it doesn't
                    // land on the shelf.
                    val stickerUris = remember(ui.sections) {
                        ui.sections
                            .flatMap { it.rows }
                            .filter {
                                it.cartItem.isFulfilled &&
                                    !it.cartItem.isSkippedDuringShopping
                            }
                            .mapNotNull { row ->
                                row.cartItem.shoppingOnlyImageUri
                                    ?: row.vaultItem?.imageUri
                            }
                            .distinct()
                    }
                    if (stickerUris.isNotEmpty()) {
                        StickerShelf(uris = stickerUris)
                    }
                }

                if (submitError != null) {
                    Text(
                        text = submitError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                        modifier = Modifier.weight(1f),
                    ) { Text("Not yet") }
                    Button(
                        onClick = {
                            scope.launch {
                                submitError = null
                                isSubmitting = true
                                val completed = viewModel.completeShoppingNow()
                                isSubmitting = false
                                if (completed) {
                                    haptics.perform(AppHaptic.Confirm)
                                    celebrationTrigger += 1
                                    delay(620)
                                    onConfirm()
                                } else {
                                    submitError = "Couldn't complete trip. Try again."
                                }
                            }
                        },
                        enabled = insights != null && !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = if (isSubmitting) "Wrapping…" else "Wrap it up",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// --------------------------------------------------------------- gauge card

/**
 * Hero card for the wrap-up sheet. The tick arc sweeps first (0 → target in
 * ~900ms on an ease-out curve — a single confident settle, no bounce), then
 * the four text blocks fade + rise in sequence, ~90ms apart. Over-budget
 * recolours the filled ticks to clay so the arc carries the warning
 * non-verbally before the caption spells it out.
 */
@Composable
private fun GaugeHeroCard(
    spent: Double,
    budget: Double,
    formatter: com.pocketsave.core.currency.CurrencyFormatter,
    modifier: Modifier = Modifier,
) {
    val pastels = PocketSaveTokens.pastels
    val hasBudget = budget > 0.0
    val ratio = if (hasBudget) (spent / budget).toFloat().coerceAtLeast(0f) else 1f
    // Visually cap the arc at 100% even when the trip overshoots — the arc
    // wrapping around itself would fight the caption, which already says
    // "X% of your ₱Y" unambiguously.
    val target = ratio.coerceIn(0f, 1f)
    val overBudget = hasBudget && ratio > 1f
    val fillColor = if (overBudget) pastels.blushDeep else MaterialTheme.colorScheme.primary
    val dimColor = pastels.inkBerry.copy(alpha = 0.08f)

    val arcProgress = remember { Animatable(0f) }
    var stage by remember { mutableIntStateOf(0) }

    LaunchedEffect(spent, budget) {
        arcProgress.snapTo(0f)
        stage = 0
        // Breath before the sweep, so the sheet's own entry animation gets to
        // settle and the tick arc reads as a distinct beat.
        delay(140)
        arcProgress.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = 900, easing = EaseOutCubic),
        )
        // Stagger the numeric reveals on the exhale of the arc. Each rung
        // ~90ms so the full sequence closes inside ~1200ms.
        stage = 1
        delay(90); stage = 2
        delay(90); stage = 3
        delay(100); stage = 4
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = AppShapes.SoftCard,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth().clip(AppShapes.SoftCard),
    ) {
        Column(
            modifier = Modifier.padding(
                top = 22.dp,
                bottom = 20.dp,
                start = 18.dp,
                end = 18.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Gauge area — tick arc + curved "Cart value" along the top +
            // "Spent" / amount anchored inside the bowl of the arc.
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(2.1f),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawGauge(
                        progress = arcProgress.value,
                        filledColor = fillColor,
                        dimColor = dimColor,
                    )
                }

                // Curved "Cart value: ₱X" on top of the arc. Fades in with
                // stage 1 — first text beat after the arc settles.
                StaggeredReveal(
                    visible = stage >= 1,
                    modifier = Modifier.matchParentSize(),
                ) {
                    CurvedCartValue(
                        text = "Cart value:  ${formatter.format(spent)}",
                        color = pastels.inkBerry.copy(alpha = 0.72f),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 6.dp),
                ) {
                    StaggeredReveal(visible = stage >= 2) {
                        Text(
                            text = "Spent",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                            color = pastels.inkBerry.copy(alpha = 0.72f),
                        )
                    }
                    Spacer(Modifier.height(1.dp))
                    StaggeredReveal(visible = stage >= 3, hasPop = true) {
                        Text(
                            text = formatter.format(spent),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 34.sp,
                            ),
                            color = pastels.inkBerry,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            StaggeredReveal(visible = stage >= 4) {
                Text(
                    text = if (hasBudget) {
                        "${(ratio * 100).roundToInt()}% of your ${formatter.format(budget)} budget"
                    } else {
                        "Set a monthly budget to track your pace."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (overBudget) pastels.blushDeep
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Paints the semicircular tick arc. 64 radial strokes spaced evenly from
 * 9 o'clock through 12 o'clock to 3 o'clock; filled count rises with
 * [progress]. The rounded caps + long ticks (inner radius 72% of outer) give
 * the arc a nautical-compass weight — it reads as "instrument" rather than
 * "progress bar."
 */
private fun DrawScope.drawGauge(
    progress: Float,
    filledColor: Color,
    dimColor: Color,
) {
    val tickCount = 64
    val centerX = size.width / 2f
    // Baseline sits a hair above the bottom edge so the outermost ticks
    // aren't clipped by the composable bounds.
    val centerY = size.height * 0.98f
    val outerR = minOf(size.width * 0.48f, size.height * 0.96f)
    val innerR = outerR * 0.72f
    val strokePx = 3.dp.toPx()
    val filledTicks = (progress * tickCount).roundToInt().coerceIn(0, tickCount)

    for (i in 0 until tickCount) {
        val t = i / (tickCount - 1f)
        // 180° = left horizon, 270° = apex, 360° = right horizon.
        val angleDeg = 180f + t * 180f
        val angleRad = (angleDeg * PI / 180.0).toFloat()
        val c = cos(angleRad)
        val s = sin(angleRad)
        val innerPt = Offset(centerX + innerR * c, centerY + innerR * s)
        val outerPt = Offset(centerX + outerR * c, centerY + outerR * s)
        drawLine(
            color = if (i < filledTicks) filledColor else dimColor,
            start = innerPt,
            end = outerPt,
            strokeWidth = strokePx,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * "Cart value: ₱X" curved along the top of the arc. Uses the native canvas
 * `drawTextOnPath` because Compose text layout can't bend a baseline. The
 * path radius sits a few dp outside the tick outer radius so the glyphs
 * float just above the dial rather than colliding with it.
 */
@Composable
private fun CurvedCartValue(
    text: String,
    color: Color,
) {
    val density = LocalDensity.current
    val fontSizePx = with(density) { 12.sp.toPx() }
    val radialOffsetPx = with(density) { 14.dp.toPx() }
    val argb = color.toArgb()

    val paint = remember(argb, fontSizePx) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            this.color = argb
            textSize = fontSizePx
            textAlign = android.graphics.Paint.Align.CENTER
            letterSpacing = 0.04f
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.NORMAL,
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height * 0.98f
        val outerR = minOf(size.width * 0.48f, size.height * 0.96f)
        val textRadius = outerR + radialOffsetPx
        val left = centerX - textRadius
        val top = centerY - textRadius
        val right = centerX + textRadius
        val bottom = centerY + textRadius

        val path = android.graphics.Path().apply {
            addArc(left, top, right, bottom, 180f, 180f)
        }
        drawContext.canvas.nativeCanvas.drawTextOnPath(text, path, 0f, 0f, paint)
    }
}

/**
 * Fade + short vertical rise reveal used for every text block in the hero
 * card. [hasPop] adds a subtle scale-up on the way in, reserved for the hero
 * amount so it lands like a stamp while the smaller labels just waft up.
 */
@Composable
private fun StaggeredReveal(
    visible: Boolean,
    modifier: Modifier = Modifier,
    hasPop: Boolean = false,
    content: @Composable () -> Unit,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 360, easing = EaseOutCubic),
        label = "reveal-alpha",
    )
    val translateY by animateFloatAsState(
        targetValue = if (visible) 0f else 10f,
        animationSpec = tween(durationMillis = 420, easing = EaseOutCubic),
        label = "reveal-translate",
    )
    val scale by animateFloatAsState(
        targetValue = if (!hasPop || visible) 1f else 0.92f,
        animationSpec = tween(durationMillis = 420, easing = EaseOutCubic),
        label = "reveal-scale",
    )
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translateY
            this.scaleX = scale
            this.scaleY = scale
        },
        contentAlignment = Alignment.Center,
    ) { content() }
}

// ---------------------------------------------------------- sticker shelf

/**
 * Floating "trophy shelf" at the bottom of the wrap-up sheet. Every fulfilled
 * item's photo — already subject-segmented into a Buldak-style sticker by the
 * vault/shopping-only add flows — drifts up into place with a slight stagger,
 * each tilted a few degrees in alternating directions so the row reads as a
 * scrapbook pile rather than a grid. A slow infinite-transition breath gives
 * every sticker a subtle hover (±3dp Y, ±2° rotation) so the shelf never goes
 * completely still — the wrap-up moment keeps its pulse.
 *
 * Capped at [MAX_STICKERS] to keep composition cheap on long trips; surplus
 * items collapse into a "+N" pebble at the end. Free-tier concerns aside,
 * more than six stickers pile up visually anyway.
 */
@Composable
private fun StickerShelf(
    uris: List<String>,
    modifier: Modifier = Modifier,
) {
    val visible = uris.take(MAX_STICKERS)
    val overflow = (uris.size - visible.size).coerceAtLeast(0)

    // Deterministic tilt/offset table keyed off index so the shelf looks the
    // same every time the sheet opens for a given trip — no random jitter on
    // recomposition. The phase offset on the hover makes each sticker bob on
    // its own cycle instead of marching in unison.
    val decorations = remember(visible.size) {
        visible.indices.map { idx ->
            val tilt = if (idx % 2 == 0) -6f else 7f
            val verticalSeat = if (idx % 2 == 0) 0f else 6f
            StickerDecoration(
                baseRotation = tilt + ((idx % 3) - 1) * 1.5f,
                verticalSeat = verticalSeat,
                phaseShift = (idx * 0.23f) % 1f,
            )
        }
    }

    // Single entry-stage flag — flipped after a small delay so the stickers
    // don't interrupt the gauge + text stagger above. Each individual sticker
    // reads its own per-index delay off this flag.
    var entered by remember(visible) { mutableStateOf(false) }
    LaunchedEffect(visible) {
        // Matches roughly: gauge ~140ms + 900ms + 4×90ms text stagger = ~1400ms
        // before the shelf begins. Landing this AFTER the gauge text keeps the
        // visual hierarchy — numbers first, memento second.
        delay(1380)
        entered = true
    }

    val infinite = rememberInfiniteTransition(label = "sticker-shelf-hover")
    // One shared phase the shelf consults for per-sticker bobs. Individual
    // stickers shift their read into this cycle via the phaseShift field so
    // they don't move in lockstep.
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sticker-shelf-phase",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "on the shelf",
            style = MaterialTheme.typography.labelSmall.copy(
                fontStyle = FontStyle.Italic,
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = PocketSaveTokens.pastels.peachDeep.copy(alpha = 0.85f),
            modifier = Modifier.graphicsLayer {
                alpha = if (entered) 0.9f else 0f
                translationY = if (entered) 0f else 10f
            },
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom,
        ) {
            visible.forEachIndexed { index, uri ->
                if (index > 0) {
                    // Negative-ish spacing to overlap stickers slightly, like
                    // a scattered pile.
                    Spacer(Modifier.width((-8).dp))
                }
                Sticker(
                    uri = uri,
                    decoration = decorations[index],
                    entered = entered,
                    index = index,
                    phase = phase,
                )
            }
            if (overflow > 0) {
                Spacer(Modifier.width(4.dp))
                OverflowPebble(
                    count = overflow,
                    entered = entered,
                    index = visible.size,
                )
            }
        }
    }
}

@Composable
private fun Sticker(
    uri: String,
    decoration: StickerDecoration,
    entered: Boolean,
    index: Int,
    phase: Float,
) {
    // Entry animation — delayed per index so the shelf cascades in. The
    // animation curves mirror the gauge's EaseOutCubic settle so the two
    // motion systems feel like siblings.
    val targetEntry = if (entered) 1f else 0f
    val entryAlpha by animateFloatAsState(
        targetValue = targetEntry,
        animationSpec = tween(
            durationMillis = 520,
            delayMillis = index * STICKER_STAGGER_MS,
            easing = EaseOutCubic,
        ),
        label = "sticker-alpha-$index",
    )
    val entryRise by animateFloatAsState(
        targetValue = targetEntry,
        animationSpec = tween(
            durationMillis = 640,
            delayMillis = index * STICKER_STAGGER_MS,
            easing = EaseOutCubic,
        ),
        label = "sticker-rise-$index",
    )

    // Continuous hover — read phase offset by this sticker's phaseShift so
    // each bobs on its own cycle. Map 0..1 phase to symmetric -1..1.
    val shiftedPhase = ((phase + decoration.phaseShift) % 1f)
    val wave = kotlin.math.sin(shiftedPhase * 2 * Math.PI).toFloat()
    val hoverY = wave * 3f
    val hoverRot = wave * 1.5f

    Box(
        modifier = Modifier
            .size(STICKER_SIZE)
            .graphicsLayer {
                alpha = entryAlpha
                // Rise from 28dp below, land at the seat Y (stagger variation).
                translationY = (1f - entryRise) * 28f + decoration.verticalSeat + hoverY
                rotationZ = decoration.baseRotation + hoverRot
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.95f)
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun OverflowPebble(count: Int, entered: Boolean, index: Int) {
    val pastels = PocketSaveTokens.pastels
    val targetEntry = if (entered) 1f else 0f
    val alpha by animateFloatAsState(
        targetValue = targetEntry,
        animationSpec = tween(
            durationMillis = 520,
            delayMillis = index * STICKER_STAGGER_MS,
            easing = EaseOutCubic,
        ),
        label = "overflow-alpha",
    )
    val rise by animateFloatAsState(
        targetValue = targetEntry,
        animationSpec = tween(
            durationMillis = 640,
            delayMillis = index * STICKER_STAGGER_MS,
            easing = EaseOutCubic,
        ),
        label = "overflow-rise",
    )

    Box(
        modifier = Modifier
            .size(STICKER_SIZE)
            .graphicsLayer {
                this.alpha = alpha
                translationY = (1f - rise) * 28f + 4f
            },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Surface(
            color = pastels.canvasTint,
            shape = androidx.compose.foundation.shape.CircleShape,
            shadowElevation = 1.dp,
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "+$count",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = pastels.inkBerry,
                )
            }
        }
    }
}

private data class StickerDecoration(
    val baseRotation: Float,
    val verticalSeat: Float,
    val phaseShift: Float,
)

private val STICKER_SIZE = 64.dp
private const val MAX_STICKERS = 6
private const val STICKER_STAGGER_MS = 90

// ------------------------------------------------------------ price-change

@Composable
private fun PriceChangeRow(
    change: VaultService.PriceChange,
    formatter: com.pocketsave.core.currency.CurrencyFormatter,
) {
    val pastels = PocketSaveTokens.pastels
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = change.itemName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            )
            Text(
                text = "${formatter.format(change.plannedPrice)} → ${formatter.format(change.actualPrice)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = if (change.difference >= 0)
                "+${formatter.format(change.difference)}"
            else
                "−${formatter.format(kotlin.math.abs(change.difference))}",
            color = if (change.difference > 0.0) pastels.blushDeep else pastels.mintDeep,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}
