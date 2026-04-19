package com.pocketsave.core.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.common.ui.components.AffectionateEmpty
import com.pocketsave.common.ui.components.BudgetCardSticker
import com.pocketsave.common.ui.components.NestedBudgetCard
import com.pocketsave.common.ui.components.PSSectionHeader
import com.pocketsave.common.ui.components.TripBudgetDelta

data class OngoingTripItem(
    val cartId: String,
    val name: String,
    val statusLabel: String,
    val isShopping: Boolean,
    val itemCount: Int,
    val spentLabel: String,
    val budgetLabel: String?,
    val progress: Float,
    /** Delta shown as the top-left pill on the nested card: "left" when
     *  under budget, "over" when past it, null when no budget. */
    val remainingLabel: String? = null,
    val overBudget: Boolean = false,
)

/**
 * Horizontal row of the user's ongoing trips, each rendered as a nested-
 * cream budget card. Alternating card rhythm comes from the sticker color
 * and the natural variation in headline length; the underlying card shape
 * is the same across the whole app so trips read as one visual family.
 */
@Composable
fun OngoingTripsRow(
    trips: List<OngoingTripItem>,
    onOpen: (String) -> Unit,
    onCreateTrip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pastels = PocketSaveTokens.pastels
    Column(modifier = modifier.fillMaxWidth()) {
        PSSectionHeader(
            title = "On the go",
            kicker = "in motion",
            accent = pastels.mintDeep,
            subtitle = if (trips.isEmpty()) {
                "Start a trip when you're ready."
            } else {
                "${trips.size} trip${if (trips.size == 1) "" else "s"} in progress"
            },
        )
        Spacer(Modifier.height(12.dp))
        if (trips.isEmpty()) {
            AffectionateEmpty(
                title = "Nothing in the cart yet.",
                body = "Set a little budget, pull from your vault, and we'll walk the aisles with you.",
                icon = Icons.Outlined.AddShoppingCart,
                accent = pastels.mintDeep,
                cta = {
                    Button(
                        onClick = onCreateTrip,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            text = "Start a trip",
                            modifier = Modifier.padding(horizontal = 6.dp),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(trips, key = { it.cartId }) { trip ->
                    // Fixed card width so the horizontal rhythm stays calm —
                    // too-wide cards would let one trip dominate the row.
                    val delta = when {
                        trip.remainingLabel == null -> TripBudgetDelta.None
                        trip.overBudget -> TripBudgetDelta.Over("−${trip.remainingLabel}")
                        else -> TripBudgetDelta.Saved("+${trip.remainingLabel}")
                    }
                    NestedBudgetCard(
                        modifier = Modifier.width(320.dp),
                        headline = trip.name,
                        itemCountLabel = trip.itemCount.toString(),
                        dateLabel = trip.statusLabel,
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
                        onClick = { onOpen(trip.cartId) },
                    )
                }
            }
        }
    }
}
