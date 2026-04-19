package com.pocketsave.core.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.common.ui.components.BudgetCardSticker
import com.pocketsave.common.ui.components.NestedBudgetCard
import com.pocketsave.common.ui.components.PSSectionHeader
import com.pocketsave.common.ui.components.TripBudgetDelta

data class RecentTripRow(
    val cartId: String,
    val name: String,
    val dateLabel: String,
    val itemCount: Int,
    val spentLabel: String,
    val budgetLabel: String?,
    val budgetDelta: BudgetDelta,
    val progress: Float,
)

sealed class BudgetDelta {
    object NoBudget : BudgetDelta()
    data class Under(val label: String) : BudgetDelta()
    data class Over(val label: String) : BudgetDelta()
}

/**
 * Completed-trip rollup rendered as a column of nested-cream budget cards.
 * Each card shows the date in the top-right, the earned/over delta as the
 * green or clay label in the top-left, the trip name as the headline, and
 * the spent/budget split on the pill progress below.
 */
@Composable
fun RecentTripsSection(
    trips: List<RecentTripRow>,
    onOpenTrip: (String) -> Unit,
    onSeeAllHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (trips.isEmpty()) return
    val pastels = PocketSaveTokens.pastels
    Column(modifier = modifier.fillMaxWidth()) {
        PSSectionHeader(
            title = "Trips past",
            kicker = "what went well",
            accent = pastels.mintDeep,
            subtitle = "A quick look at the last few runs.",
            onSeeAll = onSeeAllHistory,
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            trips.forEach { trip ->
                val delta = when (val d = trip.budgetDelta) {
                    BudgetDelta.NoBudget -> TripBudgetDelta.None
                    is BudgetDelta.Under -> TripBudgetDelta.Saved("+${d.label}")
                    is BudgetDelta.Over -> TripBudgetDelta.Over("−${d.label}")
                }
                NestedBudgetCard(
                    headline = trip.name,
                    itemCountLabel = trip.itemCount.toString(),
                    dateLabel = trip.dateLabel,
                    spentLabel = trip.spentLabel,
                    budgetLabel = trip.budgetLabel,
                    progress = trip.progress,
                    delta = delta,
                    sticker = {
                        BudgetCardSticker(
                            icon = Icons.Outlined.ShoppingBasket,
                            tint = pastels.mintDeep,
                        )
                    },
                    onClick = { onOpenTrip(trip.cartId) },
                )
            }
        }
    }
}
