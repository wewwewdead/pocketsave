package com.pocketsave.billing

import com.revenuecat.purchases.CustomerInfo

/**
 * Outcome of a [SubscriptionManager.purchase] or
 * [SubscriptionManager.restore] attempt. Collapses RevenueCat's callback
 * signature into a small sealed class so the paywall screen can `when` on
 * it without pulling RC types into UI code.
 */
sealed class PurchaseResult {

    /**
     * Purchase / restore completed successfully. [isPro] is the resolved
     * entitlement state after applying the new [customerInfo] — `true` when
     * the `"pro"` entitlement is active.
     */
    data class Success(
        val customerInfo: CustomerInfo,
        val isPro: Boolean,
    ) : PurchaseResult()

    /** User explicitly dismissed the Play Billing sheet. Not an error. */
    data object UserCancelled : PurchaseResult()

    /**
     * [SubscriptionManager.start] never successfully configured RevenueCat
     * (e.g. the public API key is still the placeholder). The paywall
     * should explain that billing isn't available and fall through.
     */
    data object NotConfigured : PurchaseResult()

    /** Play Billing or network failure. Message is already user-friendly. */
    data class Error(val message: String) : PurchaseResult()
}
