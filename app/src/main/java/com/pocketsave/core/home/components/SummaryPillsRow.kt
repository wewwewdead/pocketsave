package com.pocketsave.core.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.LocalMall
import androidx.compose.material.icons.outlined.Savings
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsave.common.ui.AppShapes
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.common.ui.decor.grainOverlay
import com.pocketsave.common.ui.pressScale
import com.pocketsave.core.currency.LocalCurrencyFormatter

/**
 * Three characterful summary pills: active trips (mint), monthly budget
 * (sky→blush as it climbs), and saved items (lavender-dusk). Pebble shape
 * alternates so the row has a gentle handmade rhythm; each pill carries a
 * subtle grain wash so no tile reads as a flat chip.
 */
data class SummaryPill(
    val label: String,
    val value: String,
    val tint: Color,
    val iconTint: Color,
    val icon: ImageVector,
    val shape: Shape = AppShapes.Pebble,
    val onClick: (() -> Unit)? = null,
    val secondaryLabel: String? = null,
)

@Composable
fun SummaryPillsRow(
    activeTrips: Int,
    monthlyBudget: Double,
    monthlySpent: Double,
    savedItems: Int,
    onOpenVault: () -> Unit,
    onOpenActiveTrips: () -> Unit,
    onOpenMonthlyBudget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pastels = PocketSaveTokens.pastels
    val formatter = LocalCurrencyFormatter.current
    val errorColor = MaterialTheme.colorScheme.error

    // Monthly-budget pill lives on sky until ~50% spent, then drifts toward a
    // warm clay so you feel the cap approaching before you hit it.
    val fraction = if (monthlyBudget > 0.0) {
        (monthlySpent / monthlyBudget).toFloat().coerceAtLeast(0f)
    } else 0f
    val redLerp = ((fraction - 0.5f) / 0.5f).coerceIn(0f, 1f)
    val baseTint = if (monthlyBudget > 0.0) pastels.skySoft else pastels.skySoft
    val baseInk = if (monthlyBudget > 0.0) pastels.skyDeep else pastels.skyDeep
    val targetTint = lerp(baseTint, pastels.blushSoft, redLerp)
    val targetIconTint = lerp(baseInk, errorColor, redLerp)
    val budgetTint by animateColorAsState(
        targetValue = targetTint,
        animationSpec = tween(durationMillis = 400),
        label = "budgetTint",
    )
    val budgetIconTint by animateColorAsState(
        targetValue = targetIconTint,
        animationSpec = tween(durationMillis = 400),
        label = "budgetIconTint",
    )

    val budgetValue = if (monthlyBudget > 0.0) formatter.format(monthlySpent) else "—"
    // "/ ₩10,000" is a hair shorter than "of ₩10,000" and reads more like a
    // spent-over-budget fraction, which is what the pill is actually showing.
    val budgetSubtitle = when {
        monthlyBudget <= 0.0 -> "Set in More"
        else -> "/ ${formatter.format(monthlyBudget)}"
    }

    val pills = listOf(
        SummaryPill(
            label = "Trips in motion",
            value = activeTrips.toString(),
            tint = pastels.mintSoft,
            iconTint = pastels.mintDeep,
            icon = Icons.Outlined.LocalMall,
            shape = AppShapes.Pebble,
            onClick = onOpenActiveTrips,
        ),
        SummaryPill(
            label = "This month",
            value = budgetValue,
            secondaryLabel = budgetSubtitle,
            tint = budgetTint,
            iconTint = budgetIconTint,
            icon = Icons.Outlined.Savings,
            shape = AppShapes.PebbleAlt,
            onClick = onOpenMonthlyBudget,
        ),
        SummaryPill(
            // Read vertically as "4 / items / in your vault" — the big
            // number, then a plural-aware "item(s)" line, then where they
            // live. Keeps the sentence legible without overflowing the
            // narrow pill width.
            label = if (savedItems == 1) "item" else "items",
            value = savedItems.toString(),
            secondaryLabel = "in your vault",
            tint = pastels.lavenderSoft,
            iconTint = pastels.lavenderDeep,
            icon = Icons.Outlined.Bookmark,
            shape = AppShapes.Pebble,
            onClick = onOpenVault,
        ),
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        pills.forEach { pill ->
            SummaryPillCard(pill = pill, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryPillCard(
    pill: SummaryPill,
    modifier: Modifier = Modifier,
) {
    val pastels = PocketSaveTokens.pastels
    val interaction = remember { MutableInteractionSource() }
    val tapModifier = pill.onClick?.let { handler ->
        Modifier
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = handler,
            )
    } ?: Modifier

    Column(
        modifier = modifier
            .clip(pill.shape)
            .background(pill.tint)
            .grainOverlay(tint = pastels.grain, density = 0.6f)
            .then(tapModifier)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.62f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = pill.icon,
                    contentDescription = null,
                    tint = pill.iconTint,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        // Value sits in a narrow column (≈ one-third of the screen) so
        // large currency formats like "₩123,000" would clip at display-
        // size. Title-large keeps it impactful while still fitting, and
        // ellipsis overflow is the safety net for unusually large numbers.
        Text(
            text = pill.value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
            ),
            color = pill.iconTint,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Text(
            text = pill.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = pill.iconTint.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        if (pill.secondaryLabel != null) {
            Text(
                text = pill.secondaryLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontStyle = FontStyle.Italic,
                ),
                color = pill.iconTint.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}
