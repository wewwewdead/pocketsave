package com.pocketsave.common.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsave.common.ui.AppShapes
import com.pocketsave.common.ui.Motion
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.common.ui.decor.grainOverlay
import com.pocketsave.common.ui.pressScale

/**
 * Savings delta surfaced as a green/clay pill on the card header. The
 * positive variant reads as "tucked away", the negative as "over plan"; the
 * colours stay on-brand (sage for good, clay for warning) while the copy is
 * caller-driven.
 */
sealed interface TripBudgetDelta {
    data class Saved(val label: String) : TripBudgetDelta
    data class Over(val label: String) : TripBudgetDelta
    data object None : TripBudgetDelta
}

/**
 * Nested-cream trip card. Outer linen canvas wraps a warmer cream inner card
 * that holds the title + pill-shaped progress bar; budget hangs to the
 * right of the pill with a strikethrough once a spend has registered. A
 * small sticker tucks in the bottom corner so the card feels like a little
 * card-in-a-card recipe note rather than a dashboard tile.
 *
 * The design intentionally puts the spent figure INSIDE the fill — the bar
 * reads more like a little progress jar than a stat row. When the budget
 * hasn't been set the strikethrough + outer number are omitted and the
 * pill collapses to a calm sage fill with just the spent label.
 */
@Composable
fun NestedBudgetCard(
    headline: String,
    itemCountLabel: String?,
    dateLabel: String?,
    spentLabel: String,
    budgetLabel: String?,
    progress: Float,
    delta: TripBudgetDelta,
    sticker: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val pastels = PocketSaveTokens.pastels
    val interaction = remember { MutableInteractionSource() }

    // Outer "paper" card. Linen background + warm hairline so the inner
    // cream tile sits inside like a folded note. Grain wash gives it tooth.
    var outer = modifier
        .fillMaxWidth()
        .clip(AppShapes.HeroCard)
        .background(pastels.canvas)
        .grainOverlay(tint = pastels.grain, density = 0.5f)
    if (onClick != null) {
        outer = outer
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
    }

    Column(
        modifier = outer.padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Header: savings pill + quiet date.
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (delta) {
                is TripBudgetDelta.Saved -> SavingsPill(
                    label = delta.label,
                    background = pastels.mintSoft.copy(alpha = 0.0f),
                    ink = pastels.mintDeep,
                )
                is TripBudgetDelta.Over -> SavingsPill(
                    label = delta.label,
                    background = pastels.blushSoft.copy(alpha = 0.0f),
                    ink = pastels.blushDeep,
                )
                TripBudgetDelta.None -> Spacer(Modifier.width(2.dp))
            }
            Spacer(Modifier.weight(1f))
            if (!dateLabel.isNullOrBlank()) {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Inner "note" card — warmer cream, softer rounding.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.SoftCard)
                .background(pastels.canvasTint)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.6).sp,
                    ),
                    color = pastels.inkBerry,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (!itemCountLabel.isNullOrBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "($itemCountLabel)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }

            // Pill progress + strikethrough budget. Using BoxWithConstraints
            // so the inner "spent" chip can measure its width against the
            // pill fill — when the fill is too narrow to hold the label we
            // tuck the label to the RIGHT of the fill inside the track so it
            // doesn't clip.
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressPill(
                    progress = progress,
                    spentLabel = spentLabel,
                    trackHeight = 36.dp,
                    modifier = Modifier.weight(1f),
                )
                if (!budgetLabel.isNullOrBlank()) {
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = budgetLabel,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.LineThrough,
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }

            if (sticker != null) {
                Box(
                    modifier = Modifier.padding(top = 2.dp),
                ) { sticker() }
            }
        }
    }
}

@Composable
private fun SavingsPill(
    label: String,
    background: Color,
    ink: Color,
) {
    // The screenshot's savings badge sits flush against the card's top edge —
    // no filled chip — so we draw it as bare text with a bold, marker-ish
    // weight. Keeps the composition calm while still letting the "+₩7,000"
    // read as a little celebration.
    Text(
        text = label,
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        ),
        color = ink,
        modifier = Modifier.background(background),
    )
}

@Composable
private fun ProgressPill(
    progress: Float,
    spentLabel: String,
    trackHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val pastels = PocketSaveTokens.pastels
    val clamped = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(Motion.MediumMs),
        label = "budget-pill-progress",
    )
    // Display fraction has a floor so a fresh-start card still shows a small
    // lip of fill — the pill never reads as "broken" when progress is zero.
    val displayFraction = if (progress <= 0f) 0.18f else animated.coerceAtLeast(0.22f)
    val overBudget = progress >= 0.95f
    val fillColor = if (overBudget) pastels.blushDeep else MaterialTheme.colorScheme.primary

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White),
    ) {
        // Filled portion.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(displayFraction)
                .clip(RoundedCornerShape(999.dp))
                .background(fillColor),
        )
        // Centered label — sits over the fill by default. On narrow fills
        // it still renders, visible against the white track once the fill
        // is too small to cover it. Color picked to read against either
        // fill or white: the inner text stays on the sage fill's onPrimary
        // for legibility.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = spentLabel,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                ),
                color = if (displayFraction > 0.6f) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    pastels.inkBerry
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Reusable small sticker — circle in a soft tint with a single icon inside.
 * Drops into [NestedBudgetCard]'s `sticker` slot so callers can carry a
 * category icon or a wholesome "basket" mark into the corner.
 */
@Composable
fun BudgetCardSticker(
    icon: ImageVector,
    tint: Color? = null,
) {
    val pastels = PocketSaveTokens.pastels
    val fg = tint ?: pastels.peachDeep
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(20.dp),
        )
    }
}
