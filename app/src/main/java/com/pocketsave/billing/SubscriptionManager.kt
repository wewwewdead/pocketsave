package com.pocketsave.billing

import android.app.Activity
import com.revenuecat.purchases.Package
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin abstraction over the billing backend. The concrete
 * [RevenueCatSubscriptionManager] is the only class that touches the RevenueCat
 * SDK — every other layer in the app consumes this interface so we can swap
 * the implementation for tests or a stub in screenshots without pulling
 * Play Billing + RevenueCat into unit tests.
 *
 * Lifecycle:
 *   1. [start] exactly once per process, in `Application.onCreate`.
 *   2. Consumers observe [isPro] (or the richer [state]) reactively.
 *   3. Paywall calls [purchase] with an [Activity] host.
 *   4. Settings calls [restore] when the user taps "Restore purchases".
 */
interface SubscriptionManager {

    /** Full billing snapshot — offerings, customer info, loading, error. */
    val state: StateFlow<SubscriptionState>

    /**
     * Derived view of [state.isPro]. Kept as its own [StateFlow] so feature
     * screens can wire a single-line `collectAsState` without pulling
     * [SubscriptionState] (and its RevenueCat types) into their compile graph.
     */
    val isPro: StateFlow<Boolean>

    /**
     * Configure the RevenueCat SDK, attach the customer-info listener, and
     * kick off the first offerings + customer-info fetch. Safe to call more
     * than once — subsequent calls are no-ops.
     */
    fun start()

    /**
     * Re-fetch offerings + customer info. Called implicitly by [start] and
     * by [restore]; call explicitly from the paywall when the sheet opens so
     * any upstream subscription changes land on screen.
     */
    fun refresh()

    /**
     * Launch the Play Billing sheet for [pkg] anchored to [activity].
     * Must be called from the main thread.
     */
    suspend fun purchase(activity: Activity, pkg: Package): PurchaseResult

    /**
     * Ask RevenueCat to sync the current Google account's purchases. Used by
     * the "Restore purchases" row in settings so users who reinstall or sign
     * in on a new device recover their Pro entitlement.
     */
    suspend fun restore(): PurchaseResult
}
