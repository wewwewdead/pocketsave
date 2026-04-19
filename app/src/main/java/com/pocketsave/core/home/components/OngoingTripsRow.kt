package com.pocketsave.core.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketsave.common.ui.CardShadowColor
import com.pocketsave.common.ui.Motion
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.core.home.pressScale

data class OngoingTripItem(
    val cartId: String,
    val name: String,
    val statusLabel: String,
    val isShopping: Boolean,
    val itemCount: Int,
    val spentLabel: String,
    val budgetLabel: String?,
    val progress: Float,
)

/**
 * Horizontal row of every ongoing (non-completed) cart — the single entry
 * point on Home for browsing active trips. Shows an inviting empty-state
 * card (with a Start-a-trip CTA) when the list is empty, so the section
 * always has something to say to the user.
 */
@Composable
fun OngoingTripsRow(
    trips: List<OngoingTripItem>,
    onOpen: (String) -> Unit,
    onCreateTrip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "In progress",
            subtitle = if (trips.isEmpty()) {
                "Nothing in progress yet"
            } else {
                "${trips.size} ongoing trip${if (trips.size == 1) "" else "s"}"
            },
        )
        Spacer(Modifier.height(12.dp))
        if (trips.isEmpty()) {
            OngoingEmptyState(onCreateTrip = onCreateTrip)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(trips, key = { it.cartId }) { trip ->
                    OngoingTripCard(trip = trip, onClick = { onOpen(trip.cartId) })
                }
            }
        }
    }
}

@Composable
private fun OngoingEmptyState(onCreateTrip: () -> Unit) {
    val pastels = PocketSaveTokens.pastels
    val interaction = remember { MutableInteractionSource() }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = CardShadowColor,
                spotColor = CardShadowColor,
            )
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onCreateTrip,
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(pastels.peachSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddShoppingCart,
                    contentDescription = null,
                    tint = pastels.peachDeep,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Start a new trip",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Set a budget, pull from your vault, and we'll track the run.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OngoingTripCard(trip: OngoingTripItem, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pastels = PocketSaveTokens.pastels
    val progress by animateFloatAsState(
        targetValue = trip.progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = Motion.MediumMs),
        label = "ongoing-progress",
    )
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .width(210.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = CardShadowColor,
                spotColor = CardShadowColor,
            )
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(label = trip.statusLabel, isShopping = trip.isShopping)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(pastels.canvasTint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = trip.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${trip.itemCount} item${if (trip.itemCount == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            MiniProgressTrack(progress = progress, hasBudget = trip.budgetLabel != null)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = trip.spentLabel,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (trip.budgetLabel != null) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "/ ${trip.budgetLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 1.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, isShopping: Boolean) {
    val pastels = PocketSaveTokens.pastels
    val tint = if (isShopping) pastels.mintSoft else pastels.lavenderSoft
    val ink = if (isShopping) pastels.mintDeep else pastels.lavenderDeep
    Surface(
        color = tint,
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(ink),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = ink,
            )
        }
    }
}

@Composable
private fun MiniProgressTrack(progress: Float, hasBudget: Boolean) {
    val pastels = PocketSaveTokens.pastels
    val track = pastels.canvasTint
    val fill = when {
        !hasBudget -> pastels.mintDeep.copy(alpha = 0.25f)
        // Near-budget fill uses the clay warning — the only non-sage color
        // in the system, reserved for "you're running hot" semantics.
        progress >= 0.85f -> pastels.blushDeep
        else -> pastels.mintDeep
    }
    val display = if (hasBudget) progress else 0.18f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(track),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(display)
                .clip(RoundedCornerShape(999.dp))
                .background(fill),
        )
    }
}
