package com.pocketsave.core.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketsave.billing.FeatureLimits
import com.pocketsave.billing.PremiumFeature
import com.pocketsave.billing.SubscriptionManager
import com.pocketsave.billing.rememberPaywallGate
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.core.currency.LocalCurrencyFormatter
import com.pocketsave.core.haptics.AppHaptic
import com.pocketsave.core.haptics.rememberAppHaptics
import com.pocketsave.core.home.pressScale
import com.pocketsave.core.service.VaultService
import com.pocketsave.data.local.entity.CartEntity
import com.pocketsave.data.prefs.CartBackgroundStore
import com.pocketsave.domain.model.CartStatus
import kotlinx.coroutines.launch
import java.text.DateFormat

/**
 * History tab. Shows a rollup of completed trips at the top and a vertical
 * list below. Each row is tappable (goes to detail, where fuller editing
 * lives) and carries a quick "Reopen" chip that flips the trip back into
 * Shopping without requiring users to dig into detail for a single-tap action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    vaultService: VaultService,
    @Suppress("UNUSED_PARAMETER") backgroundStore: CartBackgroundStore,
    subscriptionManager: SubscriptionManager,
    onBack: () -> Unit,
    onOpenCart: (String) -> Unit,
    onOpenPaywall: (PremiumFeature) -> Unit,
) {
    val state by vaultService.state.collectAsState()
    val formatter = LocalCurrencyFormatter.current
    val scope = rememberCoroutineScope()
    val haptics = rememberAppHaptics()

    val paywallGate = rememberPaywallGate(subscriptionManager, vaultService, onOpenPaywall)

    val completed = remember(state.carts) {
        state.carts
            .filter { CartStatus.fromRaw(it.status) == CartStatus.COMPLETED }
            .sortedByDescending { it.completedAt ?: it.updatedAt }
    }

    // Free users see only the most recent trips in the list; older ones stay
    // persisted and become visible again on upgrade. The rollup above the
    // list always reflects ALL completed trips so the top-line number isn't
    // misleading.
    val hasDeepHistory = paywallGate.isAllowed(PremiumFeature.DeepHistory)
    val visibleCompleted = if (hasDeepHistory) completed
    else completed.take(FeatureLimits.FREE_HISTORY_ROWS)
    val hiddenCount = completed.size - visibleCompleted.size

    val rollup = remember(completed, state.cartItemsByCart) {
        computeRollup(vaultService, completed, state.cartItemsByCart)
    }

    var pendingReopen by remember { mutableStateOf<CartEntity?>(null) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        LazyColumn(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "title") {
                Column {
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (completed.isEmpty())
                            "Your completed trips will land here."
                        else
                            "A calm roll-up of trips you've finished.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (rollup.tripCount > 0) {
                item(key = "rollup") { RollupCard(rollup = rollup, formatLabel = { formatter.format(it) }) }
            }

            if (completed.isEmpty()) {
                item(key = "empty") { EmptyHistoryState() }
            } else {
                item(key = "trips-heading") {
                    Text(
                        text = "Trips",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(visibleCompleted, key = { it.id }) { cart ->
                    val items = state.cartItemsByCart[cart.id].orEmpty()
                    val spent = vaultService.computeTotalSpent(CartStatus.COMPLETED, items)
                    HistoryRow(
                        cart = cart,
                        itemCount = items.size,
                        spentLabel = formatter.format(spent),
                        budgetLabel = cart.budget.takeIf { it > 0.0 }?.let { formatter.format(it) },
                        overBudget = cart.budget > 0.0 && spent > cart.budget,
                        underBudgetLabel = if (cart.budget > 0.0 && spent <= cart.budget) {
                            formatter.format((cart.budget - spent).coerceAtLeast(0.0))
                        } else null,
                        overBudgetLabel = if (cart.budget > 0.0 && spent > cart.budget) {
                            formatter.format(spent - cart.budget)
                        } else null,
                        onOpen = { onOpenCart(cart.id) },
                        onReopen = { pendingReopen = cart },
                    )
                }
                if (hiddenCount > 0) {
                    item(key = "unlock-full-history") {
                        UnlockFullHistoryRow(
                            hiddenCount = hiddenCount,
                            onClick = { paywallGate.openPaywall(PremiumFeature.DeepHistory) },
                        )
                    }
                }
            }
        }
    }

    pendingReopen?.let { cart ->
        AlertDialog(
            onDismissRequest = { pendingReopen = null },
            confirmButton = {
                TextButton(onClick = {
                    val target = cart
                    pendingReopen = null
                    // Confirm once the user actually chooses to reopen. The
                    // suspended DB flip is IO-bound; firing after the user's
                    // confirmation (not after the DB round-trip) keeps the
                    // haptic aligned with the gesture.
                    haptics.perform(AppHaptic.Confirm)
                    scope.launch {
                        // reopenCart flips the cart back to SHOPPING and clears
                        // actuals so the user can re-confirm prices live. After
                        // the reopen we push them into the cart detail so they
                        // see the restored state immediately.
                        vaultService.reopenCart(target.id)
                        onOpenCart(target.id)
                    }
                }) {
                    Text(
                        text = "Reopen trip",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingReopen = null }) { Text("Cancel") }
            },
            title = {
                Text(
                    text = "Reopen ${cart.name.ifBlank { "this trip" }}?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            },
            text = {
                Text(
                    text = "Actual prices and fulfillment marks will clear so you " +
                        "can re-confirm at today's values. The trip moves back to Shopping.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

// ------------------------------------------------------------------ rollup

private data class Rollup(
    val tripCount: Int,
    val totalSpent: Double,
    val totalBudget: Double,
) {
    val overBudget: Double get() = (totalSpent - totalBudget).coerceAtLeast(0.0)
    val underBudget: Double get() = (totalBudget - totalSpent).coerceAtLeast(0.0)
}

private fun computeRollup(
    vaultService: VaultService,
    completedCarts: List<CartEntity>,
    cartItemsByCart: Map<String, List<com.pocketsave.data.local.entity.CartItemEntity>>,
): Rollup {
    var totalSpent = 0.0
    var totalBudget = 0.0
    for (cart in completedCarts) {
        val items = cartItemsByCart[cart.id].orEmpty()
        totalSpent += vaultService.computeTotalSpent(CartStatus.fromRaw(cart.status), items)
        if (cart.budget > 0.0) totalBudget += cart.budget
    }
    return Rollup(tripCount = completedCarts.size, totalSpent = totalSpent, totalBudget = totalBudget)
}

@Composable
private fun RollupCard(rollup: Rollup, formatLabel: (Double) -> String) {
    val pastels = PocketSaveTokens.pastels
    Surface(
        color = pastels.mintSoft,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "${rollup.tripCount} completed trip${if (rollup.tripCount == 1) "" else "s"}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = pastels.mintDeep,
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                RollupStat(
                    label = "Total spent",
                    value = formatLabel(rollup.totalSpent),
                    modifier = Modifier.weight(1f),
                )
                RollupStat(
                    label = "Total budget",
                    value = if (rollup.totalBudget > 0.0) formatLabel(rollup.totalBudget) else "—",
                    modifier = Modifier.weight(1f),
                )
            }
            if (rollup.totalBudget > 0.0) {
                val overall = if (rollup.overBudget > 0.0) {
                    "${formatLabel(rollup.overBudget)} over budget overall"
                } else {
                    "${formatLabel(rollup.underBudget)} under budget overall"
                }
                val color = if (rollup.overBudget > 0.0) pastels.blushDeep else pastels.mintDeep
                Text(
                    text = overall,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun RollupStat(label: String, value: String, modifier: Modifier = Modifier) {
    val pastels = PocketSaveTokens.pastels
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = pastels.mintDeep.copy(alpha = 0.78f),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = pastels.mintDeep,
        )
    }
}

// ------------------------------------------------------------------ row

@Composable
private fun HistoryRow(
    cart: CartEntity,
    itemCount: Int,
    spentLabel: String,
    budgetLabel: String?,
    overBudget: Boolean,
    underBudgetLabel: String?,
    overBudgetLabel: String?,
    onOpen: () -> Unit,
    onReopen: () -> Unit,
) {
    val pastels = PocketSaveTokens.pastels
    val dateFormatter = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    val interaction = remember { MutableInteractionSource() }
    val completedLabel = cart.completedAt?.let { dateFormatter.format(it) } ?: "Completed"

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
                onClick = onOpen,
            ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(pastels.mintSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingBasket,
                        contentDescription = null,
                        tint = pastels.mintDeep,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cart.name.ifBlank { "Untitled trip" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "$completedLabel  ·  $itemCount item${if (itemCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = spentLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (budgetLabel != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "of $budgetLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            // Footer row with budget delta chip on the left and a quick
            // Reopen chip on the right. Reopen is its own tap target so
            // it won't trigger the row's navigate-to-detail.
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    overBudget && overBudgetLabel != null ->
                        DeltaChip(label = "$overBudgetLabel over", warning = true)
                    underBudgetLabel != null ->
                        DeltaChip(label = "$underBudgetLabel under", warning = false)
                    else -> DeltaChip(label = "No budget", neutral = true)
                }
                Spacer(Modifier.weight(1f))
                ReopenChip(onClick = onReopen)
            }
        }
    }
}

@Composable
private fun DeltaChip(label: String, warning: Boolean = false, neutral: Boolean = false) {
    val pastels = PocketSaveTokens.pastels
    val (tint, ink) = when {
        neutral -> pastels.canvasTint to MaterialTheme.colorScheme.onSurfaceVariant
        warning -> pastels.blushSoft to pastels.blushDeep
        else -> pastels.mintSoft to pastels.mintDeep
    }
    Surface(color = tint, shape = RoundedCornerShape(999.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ink,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ReopenChip(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pastels = PocketSaveTokens.pastels
    Surface(
        color = pastels.peachSoft,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .pressScale(interaction, pressedScale = 0.94f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.RestartAlt,
                contentDescription = null,
                tint = pastels.peachDeep,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Reopen",
                style = MaterialTheme.typography.labelSmall,
                color = pastels.peachDeep,
            )
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    val pastels = PocketSaveTokens.pastels
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(pastels.mintSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingBasket,
                    contentDescription = null,
                    tint = pastels.mintDeep,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "No completed trips yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Once you finish a trip it'll land here. Completed trips stay tappable so you can reopen them anytime.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * "Unlock full history" row appended at the tail of a free user's list when
 * there are trips they can't see yet. Hidden trips are still persisted —
 * upgrading brings them straight back without any migration.
 */
@Composable
private fun UnlockFullHistoryRow(
    hiddenCount: Int,
    onClick: () -> Unit,
) {
    val pastels = PocketSaveTokens.pastels
    val interaction = remember { MutableInteractionSource() }
    Surface(
        color = pastels.lavenderSoft,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = null,
                    tint = pastels.lavenderDeep,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Unlock your full history",
                    style = MaterialTheme.typography.titleMedium
                        .copy(fontWeight = FontWeight.SemiBold),
                    color = pastels.lavenderDeep,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$hiddenCount more trip${if (hiddenCount == 1) "" else "s"} waiting with PocketSave Pro.",
                    style = MaterialTheme.typography.labelSmall,
                    color = pastels.lavenderDeep.copy(alpha = 0.78f),
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = pastels.lavenderDeep,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
