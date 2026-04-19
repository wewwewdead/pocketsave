package com.pocketsave.billing

import com.pocketsave.BuildConfig

/**
 * Canonical home for every billing identifier the app ships with. If an id
 * needs to change (renamed entitlement, new product SKU, swapped offering),
 * this is the only file to edit — everything else in the codebase refers to
 * these constants.
 *
 * Design rules:
 *  - **No secrets in source.** The RevenueCat Android public SDK key is read
 *    from `BuildConfig.REVENUECAT_ANDROID_API_KEY`, which Gradle injects from
 *    `android/local.properties` (git-ignored per-developer file). A
 *    placeholder is used if the property is missing so the app still builds.
 *  - **No ambient config lookups.** Feature code never touches `BuildConfig`
 *    or environment strings — it reads these constants or goes through the
 *    [SubscriptionManager] interface.
 *  - **Names match the dashboards.** Each constant documents which
 *    RevenueCat / Play Console entity it maps to, so swaps stay synchronised.
 */
object BillingConfig {

    /**
     * RevenueCat entitlement identifier. Must match the id configured in
     * RevenueCat dashboard → Entitlements. All Pro features gate off this
     * entitlement; there is no second entitlement.
     */
    const val ENTITLEMENT_PRO: String = "pro"

    /**
     * Preferred RevenueCat offering identifier. Must match the offering id
     * configured in RevenueCat dashboard → Offerings. The subscription
     * manager looks this offering up first, then falls back to
     * `offerings.current` if it isn't present, so either wiring works.
     */
    const val OFFERING_DEFAULT: String = "default"

    /**
     * Google Play subscription product ids. These must match the Product IDs
     * created in Play Console → Monetize → Subscriptions, and the Products
     * attached to the `pro` entitlement in RevenueCat.
     *
     * Kept as constants so the paywall can sort / label packages explicitly
     * by price point, and so any mismatch between Play + RC surfaces as a
     * single-line edit here.
     */
    const val PRODUCT_ID_MONTHLY: String = "pocketsave_pro_monthly"
    const val PRODUCT_ID_YEARLY: String = "pocketsave_pro_yearly"

    /**
     * RevenueCat Android public SDK key. Sourced from `local.properties`:
     *
     * ```
     * revenuecat.apiKey.android=goog_YourRealKey
     * ```
     *
     * Not a secret — RevenueCat public keys are designed to ship in the APK.
     * Kept out of committed source purely so each developer (and CI pipeline)
     * can point at their own project without touching code.
     */
    val REVENUECAT_ANDROID_API_KEY: String = BuildConfig.REVENUECAT_ANDROID_API_KEY

    /** Prefix of the Gradle fallback value — used by [isApiKeyConfigured]. */
    internal const val PLACEHOLDER_KEY_PREFIX: String = "goog_YOUR_"

    /**
     * `false` until `local.properties` provides a real API key. Read by
     * [RevenueCatSubscriptionManager.start] so the app keeps running during
     * setup — billing simply stays inert, `isPro` stays `false`, no SDK call
     * is made.
     */
    val isApiKeyConfigured: Boolean
        get() = REVENUECAT_ANDROID_API_KEY.isNotBlank() &&
            !REVENUECAT_ANDROID_API_KEY.startsWith(PLACEHOLDER_KEY_PREFIX)
}
