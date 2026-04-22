package com.pocketsave.billing

import android.util.Log

/**
 * Tiny analytics surface for billing events. PocketSave has no existing
 * analytics system, so the default in [com.pocketsave.app.AppContainer] is a
 * no-op — the hooks fire but land nowhere until someone plugs in a real
 * sink (Firebase, Mixpanel, PostHog, etc.).
 *
 * The shape is deliberately minimal:
 *   - One interface, one method.
 *   - Events are a sealed class so adding, renaming, or removing events
 *     surfaces as compile errors in every emit site.
 *   - No user data / PII / screen context on events — just billing facts
 *     the flow already handles.
 *
 * Swapping is a one-line change in [com.pocketsave.app.AppContainer]; no
 * feature code or paywall code needs to know the concrete type.
 */
fun interface BillingAnalytics {
    fun track(event: BillingEvent)
}

/**
 * Closed set of events the paywall / purchase flow can emit. The `trigger`
 * field on relevant events carries the [PremiumFeature.key] that opened the
 * paywall, or `null` for ambient entries (e.g. the More tab's Pro row).
 */
sealed class BillingEvent {

    /** Paywall destination mounted. */
    data class PaywallShown(val trigger: String?) : BillingEvent()

    /**
     * Paywall destination torn down. Fires on any exit path — user close,
     * back gesture, nav pop after a successful purchase — so analytics can
     * join with [PurchaseSucceeded] to distinguish conversion from bounce.
     */
    data class PaywallDismissed(val trigger: String?) : BillingEvent()

    /** User tapped Start; the Play Billing sheet is about to launch. */
    data class PurchaseStarted(
        val productId: String,
        val trigger: String?,
    ) : BillingEvent()

    /** Play reported a completed purchase and the Pro entitlement is active. */
    data class PurchaseSucceeded(
        val productId: String,
        val trigger: String?,
    ) : BillingEvent()

    /** User dismissed the Play sheet. Not a failure. */
    data class PurchaseCancelled(
        val productId: String,
        val trigger: String?,
    ) : BillingEvent()

    /** Purchase failed for any non-cancel reason (network, Play error, etc.). */
    data class PurchaseFailed(
        val productId: String,
        val trigger: String?,
        val reason: String,
    ) : BillingEvent()

    /** User tapped Restore purchases. */
    data object RestoreStarted : BillingEvent()

    /**
     * Restore completed. [isPro] reflects whether the Google account on this
     * device actually had a Pro entitlement to restore — so the funnel can
     * tell "recovered a sub" apart from "user tapped restore but nothing
     * was there".
     */
    data class RestoreSucceeded(val isPro: Boolean) : BillingEvent()

    data class RestoreFailed(val reason: String) : BillingEvent()
}

/** No-op default. Every event lands here unless the container swaps it out. */
object NoOpBillingAnalytics : BillingAnalytics {
    override fun track(event: BillingEvent) = Unit
}

/**
 * Logcat-backed sink. Useful during development — tail with
 * `adb logcat -s PocketSaveBilling:I` to watch the funnel. Swap in
 * [com.pocketsave.app.AppContainer] when wiring a real analytics backend.
 */
class LogcatBillingAnalytics : BillingAnalytics {
    override fun track(event: BillingEvent) {
        Log.i(TAG, event.toString())
    }
    companion object {
        private const val TAG = "PocketSaveBilling"
    }
}
