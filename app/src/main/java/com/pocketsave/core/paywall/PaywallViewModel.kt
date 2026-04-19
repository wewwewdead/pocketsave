package com.pocketsave.core.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pocketsave.billing.BillingAnalytics
import com.pocketsave.billing.BillingEvent
import com.pocketsave.billing.NoOpBillingAnalytics
import com.pocketsave.billing.PremiumFeature
import com.pocketsave.billing.PurchaseResult
import com.pocketsave.billing.SubscriptionManager
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the paywall's transient UI state: which package is selected, whether
 * a purchase / restore is in flight, and any one-shot results that the
 * Compose layer needs to turn into snackbars or auto-dismiss.
 *
 * Intentionally does **not** hold an [Activity] reference. Compose passes one
 * in to [purchaseSelected] at call time — the VM just forwards it to
 * [SubscriptionManager] for the Play Billing handoff.
 */
class PaywallViewModel(
    private val subscriptionManager: SubscriptionManager,
    private val analytics: BillingAnalytics = NoOpBillingAnalytics,
    private val trigger: PremiumFeature? = null,
) : ViewModel() {

    private val triggerKey: String? get() = trigger?.key

    /**
     * Snapshot consumed by [PaywallScreen]. All fields except [error] and
     * [purchaseCompleted] / [restoreResult] are steady-state; the latter
     * three are one-shot and cleared by the UI via [consumeError] /
     * [consumeRestoreResult] or on dismissal.
     */
    data class UiState(
        /**
         * `true` while offerings are being fetched for the first time. Goes
         * to `false` as soon as we have any packages, even if a refresh is
         * still running in the background.
         */
        val isLoading: Boolean = true,

        /**
         * Mirrors [com.pocketsave.billing.SubscriptionState.isConfigured].
         * When `false`, the paywall shows a friendly "unavailable" card
         * rather than pretending prices will load.
         */
        val isConfigured: Boolean = false,

        /** Current entitlement verdict — Pro flips CTAs into their "already on" mode. */
        val isPro: Boolean = false,

        val monthlyPackage: Package? = null,
        val yearlyPackage: Package? = null,
        val selectedPackage: Package? = null,

        val isPurchasing: Boolean = false,
        val isRestoring: Boolean = false,

        /** One-shot: set to `true` the moment a purchase upgrades the user. */
        val purchaseCompleted: Boolean = false,

        /** One-shot: outcome of the last [restorePurchases] call. */
        val restoreResult: RestoreOutcome? = null,

        /** One-shot: most-recent user-facing error message. */
        val error: String? = null,
    )

    sealed class RestoreOutcome {
        /** Restore succeeded and a Pro entitlement was recovered. */
        data object Restored : RestoreOutcome()

        /** Restore succeeded but the Google account has no Pro subscription. */
        data object NoActiveSubscription : RestoreOutcome()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        analytics.track(BillingEvent.PaywallShown(triggerKey))
        viewModelScope.launch {
            subscriptionManager.state.collect { subState ->
                val offering: Offering? = subState.currentOffering
                val monthly = offering?.pickMonthlyPackage()
                val yearly = offering?.pickYearlyPackage()
                _uiState.update { current ->
                    current.copy(
                        // Loading stays true only while there is literally nothing
                        // to show yet; once a package has surfaced (even from a
                        // previous fetch) the UI switches to the real selector.
                        isLoading = subState.isLoading && monthly == null && yearly == null,
                        isConfigured = subState.isConfigured,
                        isPro = subState.isPro,
                        monthlyPackage = monthly,
                        yearlyPackage = yearly,
                        // Default the selection to yearly when available —
                        // highest-value choice, most teams see it convert best.
                        // Only set the default once; respect an explicit pick
                        // the user has already made.
                        selectedPackage = current.selectedPackage
                            ?: yearly
                            ?: monthly,
                    )
                }
            }
        }
        // Nudge a refresh so that opening the paywall always reflects the
        // freshest pricing / offering from the RC dashboard. Safe to call
        // repeatedly — the manager guards against concurrent fetches.
        subscriptionManager.refresh()
    }

    fun selectPackage(pkg: Package) {
        _uiState.update { it.copy(selectedPackage = pkg) }
    }

    /**
     * Launch the Play Billing sheet for the currently selected package.
     * Caller (Compose) must pass a live [Activity] — RevenueCat anchors the
     * purchase UI to it. No-ops if a purchase is already in flight or no
     * package is selected yet.
     */
    fun purchaseSelected(activity: Activity) {
        val pkg = _uiState.value.selectedPackage ?: return
        if (_uiState.value.isPurchasing) return
        val productId = pkg.product.id
        analytics.track(BillingEvent.PurchaseStarted(productId, triggerKey))
        _uiState.update { it.copy(isPurchasing = true, error = null) }
        viewModelScope.launch {
            val result = subscriptionManager.purchase(activity, pkg)
            when (result) {
                is PurchaseResult.Success -> {
                    analytics.track(BillingEvent.PurchaseSucceeded(productId, triggerKey))
                    _uiState.update {
                        it.copy(
                            isPurchasing = false,
                            isPro = result.isPro,
                            purchaseCompleted = result.isPro,
                        )
                    }
                }
                PurchaseResult.UserCancelled -> {
                    analytics.track(BillingEvent.PurchaseCancelled(productId, triggerKey))
                    _uiState.update { it.copy(isPurchasing = false) }
                }
                PurchaseResult.NotConfigured -> {
                    analytics.track(
                        BillingEvent.PurchaseFailed(productId, triggerKey, "not_configured"),
                    )
                    _uiState.update {
                        it.copy(
                            isPurchasing = false,
                            error = "Subscriptions aren't available on this device yet.",
                        )
                    }
                }
                is PurchaseResult.Error -> {
                    analytics.track(
                        BillingEvent.PurchaseFailed(productId, triggerKey, result.message.ifBlank { "unknown" }),
                    )
                    _uiState.update {
                        it.copy(
                            isPurchasing = false,
                            error = result.message.ifBlank {
                                "Something went wrong. Please try again."
                            },
                        )
                    }
                }
            }
        }
    }

    fun restorePurchases() {
        if (_uiState.value.isRestoring) return
        analytics.track(BillingEvent.RestoreStarted)
        _uiState.update { it.copy(isRestoring = true, error = null, restoreResult = null) }
        viewModelScope.launch {
            val result = subscriptionManager.restore()
            when (result) {
                is PurchaseResult.Success -> {
                    analytics.track(BillingEvent.RestoreSucceeded(result.isPro))
                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            isPro = result.isPro,
                            restoreResult = if (result.isPro) RestoreOutcome.Restored
                            else RestoreOutcome.NoActiveSubscription,
                        )
                    }
                }
                PurchaseResult.UserCancelled -> {
                    _uiState.update { it.copy(isRestoring = false) }
                }
                PurchaseResult.NotConfigured -> {
                    analytics.track(BillingEvent.RestoreFailed("not_configured"))
                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            error = "Subscriptions aren't available on this device yet.",
                        )
                    }
                }
                is PurchaseResult.Error -> {
                    analytics.track(BillingEvent.RestoreFailed(result.message.ifBlank { "unknown" }))
                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            error = result.message.ifBlank {
                                "Couldn't restore purchases. Please try again."
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        // Fires on any exit path — user close, system back, nav pop after
        // a successful purchase. Analytics consumers can join with
        // `PurchaseSucceeded` to distinguish "converted and dismissed" from
        // "dismissed without buying" without needing a close-reason field.
        analytics.track(BillingEvent.PaywallDismissed(triggerKey))
        super.onCleared()
    }

    fun consumeError() {
        _uiState.update { it.copy(error = null) }
    }

    fun consumeRestoreResult() {
        _uiState.update { it.copy(restoreResult = null) }
    }

    private fun Offering.pickMonthlyPackage(): Package? =
        monthly ?: availablePackages.firstOrNull { it.packageType == PackageType.MONTHLY }

    private fun Offering.pickYearlyPackage(): Package? =
        annual ?: availablePackages.firstOrNull { it.packageType == PackageType.ANNUAL }

    class Factory(
        private val subscriptionManager: SubscriptionManager,
        private val analytics: BillingAnalytics = NoOpBillingAnalytics,
        private val trigger: PremiumFeature? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PaywallViewModel::class.java))
            return PaywallViewModel(
                subscriptionManager = subscriptionManager,
                analytics = analytics,
                trigger = trigger,
            ) as T
        }
    }
}
