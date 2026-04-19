package com.pocketsave.billing

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings

/**
 * Snapshot of the billing world the app cares about. Produced by
 * [SubscriptionManager] implementations; consumed by the paywall screen and
 * by any settings surface that shows entitlement status.
 *
 * RevenueCat types (`Offerings`, `Offering`, `CustomerInfo`) are exposed here
 * because the paywall and manage-subscription flows need them. Feature screens
 * should read [SubscriptionManager.isPro] only — they never need these fields.
 */
data class SubscriptionState(
    /**
     * `true` once [SubscriptionManager.start] has successfully called
     * `Purchases.configure`. If the public API key is still the placeholder
     * this stays `false` forever and no SDK calls are made.
     */
    val isConfigured: Boolean = false,

    /** An offerings / customer-info fetch is currently in flight. */
    val isLoading: Boolean = false,

    /**
     * Current entitlement verdict. Mirrors the `"pro"` entitlement on the
     * RevenueCat [customerInfo]. Optimistically seeded from the cached flag
     * in DataStore on cold start so the first frame doesn't briefly flash
     * `false` for Pro users on reboot.
     */
    val isPro: Boolean = false,

    val customerInfo: CustomerInfo? = null,

    /** Full offerings catalog — used by the paywall to list alt offerings. */
    val offerings: Offerings? = null,

    /**
     * Convenience handle on `offerings.current` — the offering flagged as
     * current in the RevenueCat dashboard. This is where monthly + yearly
     * packages for the paywall live.
     */
    val currentOffering: Offering? = null,

    /** User-facing message from the last failed fetch / purchase, if any. */
    val error: String? = null,
)
