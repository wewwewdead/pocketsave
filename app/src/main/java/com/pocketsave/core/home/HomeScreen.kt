package com.pocketsave.core.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketsave.billing.FeatureLimits
import com.pocketsave.billing.PremiumFeature
import com.pocketsave.billing.SubscriptionManager
import com.pocketsave.billing.rememberPaywallGate
import com.pocketsave.core.cart.CreateCartSheet
import com.pocketsave.core.cart.VaultSelectionStore
import com.pocketsave.core.currency.LocalCurrencyFormatter
import com.pocketsave.core.paywall.CapHintBanner
import com.pocketsave.core.home.components.BudgetDelta
import com.pocketsave.core.home.components.OngoingTripItem
import com.pocketsave.core.home.components.OngoingTripsRow
import com.pocketsave.core.home.components.QuickActionsGrid
import com.pocketsave.core.home.components.RecentTripRow
import com.pocketsave.core.home.components.RecentTripsSection
import com.pocketsave.core.home.components.RememberedItem
import com.pocketsave.core.home.components.RememberedItemsRow
import com.pocketsave.core.home.components.SummaryPillsRow
import com.pocketsave.core.home.components.VaultCategoryTile
import com.pocketsave.core.home.components.VaultPaletteCycle
import com.pocketsave.core.home.components.VaultPreviewRow
import com.pocketsave.core.home.hints.HomeFirstRunHints
import com.pocketsave.core.service.VaultService
import com.pocketsave.core.vault.icons.AppIcon
import com.pocketsave.data.local.entity.CartEntity
import com.pocketsave.data.prefs.AppPreferences
import com.pocketsave.data.prefs.CartBackgroundStore
import com.pocketsave.domain.model.CartStatus
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Redesigned Home: a calm, bright dashboard built from real VaultService data.
 *
 * Sections stack vertically inside a LazyColumn and each one reveals with a
 * short staggered entrance. The outer signature — including all navigation
 * callbacks, the first-run hint overlay, and the in-place create-cart /
 * currency sheets — is preserved so no caller has to change.
 */
@Composable
fun HomeScreen(
    vaultService: VaultService,
    selectionStore: VaultSelectionStore,
    backgroundStore: CartBackgroundStore,
    preferences: AppPreferences,
    subscriptionManager: SubscriptionManager,
    onOpenVault: () -> Unit,
    onOpenCart: (cartId: String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenPaywall: (PremiumFeature) -> Unit,
    onOpenActiveTrips: () -> Unit = {},
) {
    val state by vaultService.state.collectAsState()
    val selectedItems by selectionStore.activeCartItems.collectAsState()
    val formatter = LocalCurrencyFormatter.current

    val active = state.carts.filter { CartStatus.fromRaw(it.status) != CartStatus.COMPLETED }
    val completed = state.carts.filter { CartStatus.fromRaw(it.status) == CartStatus.COMPLETED }

    // Every ongoing cart (planning or shopping) mapped to the compact row
    // item. The In-progress section is the single entry point to active
    // trips on this page — sorted by VaultService's newest-first ordering.
    val ongoingTrips: List<OngoingTripItem> = remember(active, state.cartItemsByCart) {
        active.map { cart ->
            val status = CartStatus.fromRaw(cart.status)
            val items = state.cartItemsByCart[cart.id].orEmpty()
            val spent = vaultService.computeTotalSpent(status, items)
            val progress = if (cart.budget > 0.0 && spent > 0.0) {
                (spent / cart.budget).toFloat().coerceIn(0f, 1f)
            } else 0f
            OngoingTripItem(
                cartId = cart.id,
                name = cart.name.ifBlank { "Untitled trip" },
                statusLabel = status.displayName,
                isShopping = status == CartStatus.SHOPPING,
                itemCount = items.size,
                spentLabel = formatter.format(spent),
                budgetLabel = cart.budget.takeIf { it > 0.0 }?.let { formatter.format(it) },
                progress = progress,
            )
        }
    }

    // Budget-left pill: sum remaining across active carts that actually have a
    // budget set. If none do, fall back to showing total items saved (the next
    // most useful single number from the real data surface).
    val budgetLeftLabel = remember(active, state.cartItemsByCart) {
        val budgeted = active.filter { it.budget > 0.0 }
        if (budgeted.isEmpty()) {
            "—"
        } else {
            val remaining = budgeted.sumOf { cart ->
                val items = state.cartItemsByCart[cart.id].orEmpty()
                val spent = vaultService.computeTotalSpent(CartStatus.fromRaw(cart.status), items)
                (cart.budget - spent).coerceAtLeast(0.0)
            }
            formatter.format(remaining)
        }
    }

    val categoryTiles: List<VaultCategoryTile> = remember(state.categories, state.items) {
        val countsByCategory = state.items.groupingBy { it.categoryUid }.eachCount()
        state.categories
            .sortedBy { it.sortOrder }
            .mapIndexed { index, cat ->
                val (tint, iconTint) = VaultPaletteCycle.tintFor(index, cat.colorHex)
                VaultCategoryTile(
                    id = cat.uid,
                    name = cat.name,
                    itemCount = countsByCategory[cat.uid] ?: 0,
                    icon = AppIcon.resolveIcon(cat.iconKey),
                    tint = tint,
                    iconTint = iconTint,
                )
            }
    }

    val rememberedItems: List<RememberedItem> = remember(state.items, state.categories) {
        val categoryById = state.categories.associateBy { it.uid }
        state.items
            .asSequence()
            .filter { it.shoppingPrice != null && (it.shoppingPrice ?: 0.0) > 0.0 }
            .filterNot { it.isDeleted }
            .sortedByDescending { it.createdAt.time }
            .take(8)
            .toList()
            .mapIndexed { index, item ->
                val category = item.categoryUid?.let { categoryById[it] }
                val (tint, ink) = VaultPaletteCycle.tintFor(index, category?.colorHex)
                RememberedItem(
                    id = item.id,
                    name = item.name,
                    priceLabel = formatter.format(item.shoppingPrice ?: 0.0),
                    unit = item.shoppingUnit,
                    categoryIcon = AppIcon.resolveIcon(category?.iconKey),
                    categoryTint = tint,
                    iconTint = ink,
                )
            }
    }

    val recentTrips: List<RecentTripRow> = remember(completed, state.cartItemsByCart) {
        completed.take(4).map { cart ->
            val items = state.cartItemsByCart[cart.id].orEmpty()
            val spent = vaultService.computeTotalSpent(CartStatus.COMPLETED, items)
            val delta = when {
                cart.budget <= 0.0 -> BudgetDelta.NoBudget
                spent <= cart.budget -> BudgetDelta.Under(
                    label = "${formatter.format((cart.budget - spent).coerceAtLeast(0.0))} under",
                )
                else -> BudgetDelta.Over(
                    label = "${formatter.format(spent - cart.budget)} over",
                )
            }
            RecentTripRow(
                cartId = cart.id,
                name = cart.name.ifBlank { "Untitled trip" },
                dateLabel = relativeDateLabel(cart.completedAt ?: cart.updatedAt),
                itemCount = items.size,
                spentLabel = formatter.format(spent),
                budgetDelta = delta,
            )
        }
    }

    var showCreateCartSheet by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CartEntity?>(null) }
    val scope = rememberCoroutineScope()

    // Free-active-carts gate. Every trip-creation entry point on Home
    // funnels through `requestCreateCart` so a capped free user always sees
    // the same paywall instead of the create sheet.
    val paywallGate = rememberPaywallGate(subscriptionManager, vaultService, onOpenPaywall)
    val requestCreateCart: () -> Unit = {
        paywallGate.check(PremiumFeature.CreateActiveTrip) { showCreateCartSheet = true }
    }
    // True only when a free user is exactly at the active-cart cap. Pro users
    // never see the banner; free users under the cap don't either.
    val showTripCapBanner = !paywallGate.isAllowed(PremiumFeature.CreateActiveTrip)

    // First-run hints behavior kept identical to the original implementation —
    // only surfaced once a cart exists so the bubbles have an anchor.
    val shouldShowFirstRunHints by preferences.shouldShowFirstRunHints.collectAsState(initial = false)
    val firstRunHintsVisible = shouldShowFirstRunHints && active.isNotEmpty()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        Box(modifier = Modifier.padding(inner).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 16.dp,
                    bottom = 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                item(key = "pills") {
                    Entrance(index = 0) {
                        SummaryPillsRow(
                            activeTrips = active.size,
                            budgetRemainingLabel = budgetLeftLabel,
                            savedItems = state.items.size,
                            onOpenVault = onOpenVault,
                            onOpenActiveTrips = onOpenActiveTrips,
                        )
                    }
                }

                if (showTripCapBanner) {
                    item(key = "trip-cap-banner") {
                        CapHintBanner(
                            label = "${active.size} of ${FeatureLimits.FREE_ACTIVE_CARTS} trips running.",
                            onUpgrade = { onOpenPaywall(PremiumFeature.CreateActiveTrip) },
                        )
                    }
                }

                // Single entry point for active carts on this page. Shows all
                // ongoing trips as a horizontal row, or a friendly empty-state
                // card with a "Start a trip" CTA when nothing's in progress.
                item(key = "ongoing") {
                    Entrance(index = 1) {
                        OngoingTripsRow(
                            trips = ongoingTrips,
                            onOpen = onOpenCart,
                            onCreateTrip = requestCreateCart,
                        )
                    }
                }

                item(key = "quick-actions") {
                    Entrance(index = 2) {
                        QuickActionsGrid(
                            onNewTrip = requestCreateCart,
                            onOpenVault = onOpenVault,
                            onOpenHistory = onOpenHistory,
                            onOpenTrash = onOpenTrash,
                        )
                    }
                }

                item(key = "vault") {
                    Entrance(index = 3) {
                        VaultPreviewRow(
                            categories = categoryTiles,
                            onOpenVault = onOpenVault,
                        )
                    }
                }

                // The remembered row hides itself when the user hasn't recorded
                // any shopping prices yet — no forced empty state.
                item(key = "remembered") {
                    Entrance(index = 4) {
                        RememberedItemsRow(
                            items = rememberedItems,
                            onOpenVault = onOpenVault,
                        )
                    }
                }

                item(key = "recent") {
                    Entrance(index = 5) {
                        RecentTripsSection(
                            trips = recentTrips,
                            onOpenTrip = onOpenCart,
                            onSeeAllHistory = onOpenHistory,
                        )
                    }
                }

                item(key = "tail") { Spacer(Modifier.height(32.dp)) }
            }

            // First-run overlay preserved verbatim — HomeFirstRunHints owns its
            // own fade/slide, so we just pass the visibility and dismissal hook.
            HomeFirstRunHints(
                visible = firstRunHintsVisible,
                onDismissed = {
                    scope.launch { preferences.setShouldShowFirstRunHints(false) }
                },
            )
        }
    }

    if (showCreateCartSheet) {
        CreateCartSheet(
            vaultService = vaultService,
            selectionStore = selectionStore,
            selectedItemCount = selectedItems.size,
            onDismiss = { showCreateCartSheet = false },
            onCreated = { cartId ->
                showCreateCartSheet = false
                onOpenCart(cartId)
            },
        )
    }

    pendingDelete?.let { target ->
        val isCompleted = CartStatus.fromRaw(target.status) == CartStatus.COMPLETED
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        vaultService.deleteCart(target.id)
                        if (!isCompleted) {
                            backgroundStore.clear(target.id)
                        }
                    }
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
            title = { Text("Delete ${target.name}?") },
            text = {
                Text(
                    if (isCompleted)
                        "Completed trips move to Trash and can be restored later."
                    else
                        "This cart and its planned items will be removed permanently.",
                )
            },
        )
    }
}

// Produces a compact "Today / Yesterday / 3 days ago" label from a Date without
// pulling in a formatter dependency. Anything beyond a week falls back to the
// day count so the rows still read meaningfully.
private fun relativeDateLabel(date: Date): String {
    val diffMs = System.currentTimeMillis() - date.time
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)
    return when {
        days <= 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 7L -> "$days days ago"
        days < 14L -> "Last week"
        days < 30L -> "${days / 7} weeks ago"
        else -> "${days / 30} months ago"
    }
}
