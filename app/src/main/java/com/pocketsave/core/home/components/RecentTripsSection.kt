package com.pocketsave.core.home.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.core.home.pressScale

data class RecentTripRow(
    val cartId: String,
    val name: String,
    val dateLabel: String,
    val itemCount: Int,
    val spentLabel: String,
    val budgetDelta: BudgetDelta,
)

sealed class BudgetDelta {
    object NoBudget : BudgetDelta()
    data class Under(val label: String) : BudgetDelta()
    data class Over(val label: String) : BudgetDelta()
}

/**
 * Recent completed trips, up to [HomeScreen]'s chosen limit. Each row gets a
 * soft positive/negative budget indicator, built from real data — nothing
 * invented — and collapses the whole section when no completed trips exist.
 */
@Composable
fun RecentTripsSection(
    trips: List<RecentTripRow>,
    onOpenTrip: (String) -> Unit,
    onSeeAllHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (trips.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Recent trips",
            subtitle = "A quick look at how last week went",
            onSeeAll = onSeeAllHistory,
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            trips.forEach { trip ->
                RecentTripRowCard(trip = trip, onClick = { onOpenTrip(trip.cartId) })
            }
        }
    }
}

@Composable
private fun RecentTripRowCard(trip: RecentTripRow, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pastels = PocketSaveTokens.pastels
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(pastels.canvasTint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingBasket,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trip.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${trip.dateLabel}  ·  ${trip.itemCount} item${if (trip.itemCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = trip.spentLabel,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                BudgetDeltaChip(delta = trip.budgetDelta)
            }
        }
    }
}

@Composable
private fun BudgetDeltaChip(delta: BudgetDelta) {
    val pastels = PocketSaveTokens.pastels
    val (tint, inkColor, icon, text) = when (delta) {
        BudgetDelta.NoBudget -> Quad(pastels.canvasTint, MaterialTheme.colorScheme.onSurfaceVariant, null, "No budget")
        is BudgetDelta.Under -> Quad(pastels.mintSoft, pastels.mintDeep, Icons.Outlined.TrendingDown, delta.label)
        is BudgetDelta.Over -> Quad(pastels.blushSoft, pastels.blushDeep, Icons.Outlined.TrendingUp, delta.label)
    }
    Surface(
        color = tint,
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = inkColor,
                    modifier = Modifier.size(11.dp),
                )
                Spacer(Modifier.size(3.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = inkColor,
            )
        }
    }
}

// Tiny ad-hoc 4-tuple so the `when` above can destructure cleanly.
private data class Quad(
    val tint: Color,
    val ink: Color,
    val icon: ImageVector?,
    val text: String,
)
