package com.pocketsave.billing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.pocketsave.core.service.VaultService
import com.pocketsave.domain.model.CartStatus
import com.pocketsave.domain.model.GroceryCategory

/**
 * Single source of truth for "can the user do this right now?". Every
 * monetization gate in the app goes through [check] — feature screens never
 * compare counts or `isPro` themselves.
 *
 * Usage pattern from Compose:
 *
 * ```
 * val gate = rememberPaywallGate(subscriptionManager, vaultService, onOpenPaywall)
 * // …
 * Button(onClick = {
 *     gate.check(PremiumFeature.CreateActiveTrip) { showCreateCartSheet = true }
 * })
 * ```
 *
 * `check` either runs the `allowed` block (user is Pro, or under the free
 * cap for this feature) or routes navigation to the paywall with the
 * matching trigger so the hero reads context-aware copy.
 *
 * The gate is a cheap value object — it's rebuilt each time `isPro` or the
 * vault snapshot changes, via [rememberPaywallGate], so the counts it reads
 * always match the UI around it.
 */
class PaywallGate(
    private val isPro: Boolean,
    private val vaultSnapshot: VaultService.Snapshot,
    private val onNavigateToPaywall: (PremiumFeature) -> Unit,
) {

    /**
     * Returns `true` iff the user can take [feature] right now. Used for
     * read-only UI affordances — e.g. a "full history" indicator, or a
     * disabled state — that need to know the verdict without triggering a
     * navigation.
     */
    fun isAllowed(feature: PremiumFeature): Boolean {
        if (isPro) return true
        return when (feature) {
            PremiumFeature.CreateActiveTrip ->
                activeCartCount() < FeatureLimits.FREE_ACTIVE_CARTS
            PremiumFeature.AddVaultItem ->
                vaultSnapshot.items.size < FeatureLimits.FREE_VAULT_ITEMS
            PremiumFeature.AddCustomCategory ->
                customCategoryCount() < FeatureLimits.FREE_CUSTOM_CATEGORIES
            PremiumFeature.DeepHistory ->
                completedTripCount() <= FeatureLimits.FREE_HISTORY_ROWS
            // Binary feature gates — always paid.
            PremiumFeature.Scanner,
            PremiumFeature.CartTheme,
            PremiumFeature.TripShareCard,
            PremiumFeature.Widget -> false
        }
    }

    /**
     * Either runs [allowed] or navigates to the paywall with [feature] as
     * the trigger. Idiomatic "wrap the onClick" pattern at the UI layer.
     */
    fun check(feature: PremiumFeature, allowed: () -> Unit) {
        if (isAllowed(feature)) allowed() else onNavigateToPaywall(feature)
    }

    /** Explicit redirect — surfaces on the "Unlock full history" row. */
    fun openPaywall(feature: PremiumFeature) {
        onNavigateToPaywall(feature)
    }

    // --- count helpers ------------------------------------------------------

    private fun activeCartCount(): Int =
        vaultSnapshot.carts.count { CartStatus.fromRaw(it.status) != CartStatus.COMPLETED }

    private fun completedTripCount(): Int =
        vaultSnapshot.carts.count { CartStatus.fromRaw(it.status) == CartStatus.COMPLETED }

    private fun customCategoryCount(): Int {
        val defaultTitles = GroceryCategory.entries.map { it.title }.toSet()
        return vaultSnapshot.categories.count { it.name !in defaultTitles }
    }
}

/**
 * Binds [PaywallGate] to the live `isPro` flow and vault snapshot. The
 * returned gate re-creates whenever either input changes so every call
 * reflects the latest state. Passing the navigation callback in keeps the
 * billing module free of any `NavController` knowledge.
 */
@Composable
fun rememberPaywallGate(
    subscriptionManager: SubscriptionManager,
    vaultService: VaultService,
    onOpenPaywall: (PremiumFeature) -> Unit,
): PaywallGate {
    val isPro by subscriptionManager.isPro.collectAsState()
    val snapshot by vaultService.state.collectAsState()
    return remember(isPro, snapshot, onOpenPaywall) {
        PaywallGate(
            isPro = isPro,
            vaultSnapshot = snapshot,
            onNavigateToPaywall = onOpenPaywall,
        )
    }
}
