package com.pocketsave.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitRestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * RevenueCat-backed [SubscriptionManager]. Only class in the codebase that
 * imports from `com.revenuecat.purchases.*`; every feature screen talks to
 * the [SubscriptionManager] interface instead.
 *
 * Responsibilities:
 *   - Configure the SDK exactly once per process (guarded by
 *     [Purchases.isConfigured] + [started]).
 *   - Listen for push updates from the `updatedCustomerInfoListener` so
 *     renewals/cancellations from Play Console land without a screen reload.
 *   - Project a compact [SubscriptionState] for paywall/settings screens and
 *     a flat [isPro] flow for feature gates.
 *   - Mirror the latest [isPro] into DataStore via [cachedIsProWriter] so the
 *     next cold launch can seed [state] with the last-known entitlement
 *     before the network fetch resolves. This is what makes the first frame
 *     usable offline for Pro subscribers.
 *
 * The class is constructed in [com.pocketsave.app.AppContainer] and
 * [start]ed from [com.pocketsave.PocketSaveApplication.onCreate].
 */
class RevenueCatSubscriptionManager(
    private val applicationContext: Context,
    private val apiKey: String = BillingConfig.REVENUECAT_ANDROID_API_KEY,
    private val entitlementId: String = BillingConfig.ENTITLEMENT_PRO,
    private val defaultOfferingId: String = BillingConfig.OFFERING_DEFAULT,
    private val debugLogging: Boolean = false,
    /** Reads the last-known Pro flag persisted in DataStore (offline hint). */
    private val cachedIsProReader: suspend () -> Boolean = { false },
    /** Writes the latest Pro flag to DataStore whenever RC pushes an update. */
    private val cachedIsProWriter: suspend (Boolean) -> Unit = {},
    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : SubscriptionManager {

    private val _state = MutableStateFlow(SubscriptionState())
    override val state: StateFlow<SubscriptionState> = _state.asStateFlow()

    override val isPro: StateFlow<Boolean> = state
        .map { it.isPro }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val started = AtomicBoolean(false)

    /**
     * Set while a [refresh] round-trip is in flight. Coalesces concurrent
     * refresh requests from multiple screens (e.g. paywall opens while the
     * foreground-resume observer also fires) into a single network fetch.
     * A concurrent caller whose request is skipped still gets the new state
     * via [state] / [isPro] because the flow fans out to all observers.
     */
    private val refreshInFlight = AtomicBoolean(false)

    private val isApiKeyPlaceholder: Boolean
        get() = apiKey.isBlank() || apiKey.startsWith(BillingConfig.PLACEHOLDER_KEY_PREFIX)

    override fun start() {
        if (!started.compareAndSet(false, true)) return

        // Seed state from the cached Pro flag first so the first isPro emission
        // after boot isn't a false negative for returning subscribers. The
        // subsequent refresh() below will overwrite with the server verdict.
        scope.launch {
            val cached = runCatching { cachedIsProReader() }.getOrDefault(false)
            if (cached) _state.update { it.copy(isPro = true) }
        }

        if (isApiKeyPlaceholder) {
            Log.w(
                TAG,
                "RevenueCat public API key is not configured — billing stays inert. " +
                    "Set BillingConfig.REVENUECAT_ANDROID_API_KEY to enable purchases.",
            )
            _state.update { it.copy(isConfigured = false, isLoading = false) }
            return
        }

        if (!Purchases.isConfigured) {
            try {
                Purchases.logLevel = if (debugLogging) LogLevel.DEBUG else LogLevel.WARN
                Purchases.configure(
                    PurchasesConfiguration.Builder(applicationContext, apiKey).build(),
                )
            } catch (t: Throwable) {
                // Keeps the app running even if the SDK can't configure (e.g.
                // malformed key that passes the placeholder check, missing
                // Play Services on a bare emulator, etc.). The rest of the
                // app stays on the cached `isPro` value and all gates
                // continue to function — just no network purchases.
                Log.e(TAG, "Purchases.configure failed", t)
                _state.update {
                    it.copy(isConfigured = false, isLoading = false, error = t.message)
                }
                return
            }
        }
        _state.update { it.copy(isConfigured = true) }

        // Push-style updates from RC: renewals, cancellations, grace-period
        // transitions all land here without needing a manual refresh.
        Purchases.sharedInstance.updatedCustomerInfoListener =
            UpdatedCustomerInfoListener { info -> applyCustomerInfo(info) }

        refresh()
    }

    override fun refresh() {
        if (!Purchases.isConfigured) return
        // Coalesce: if a refresh is already running, skip. Observers still
        // receive the results through `state` / `isPro` so callers that
        // "missed" their turn don't need to retry.
        if (!refreshInFlight.compareAndSet(false, true)) return
        scope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }
                val offerings: Offerings? = runCatching {
                    Purchases.sharedInstance.awaitOfferings()
                }.onFailure { Log.w(TAG, "awaitOfferings failed", it) }.getOrNull()

                val info: CustomerInfo? = runCatching {
                    Purchases.sharedInstance.awaitCustomerInfo()
                }.onFailure { Log.w(TAG, "awaitCustomerInfo failed", it) }.getOrNull()

                // Resolve the offering the paywall will render: look up by the
                // configured identifier first, fall back to whatever is flagged
                // "current" in the dashboard. This keeps the wiring working if
                // the dashboard names the offering something other than
                // `BillingConfig.OFFERING_DEFAULT` — as long as it's flagged
                // current, we still find it.
                val resolvedCurrent = offerings?.let { it.getOffering(defaultOfferingId) ?: it.current }

                val pro = info?.hasEntitlement()
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        offerings = offerings ?: current.offerings,
                        currentOffering = resolvedCurrent ?: current.currentOffering,
                        customerInfo = info ?: current.customerInfo,
                        isPro = pro ?: current.isPro,
                        error = null,
                    )
                }
                if (pro != null) persistCachedIsPro(pro)
            } finally {
                refreshInFlight.set(false)
            }
        }
    }

    override suspend fun purchase(activity: Activity, pkg: Package): PurchaseResult {
        if (!Purchases.isConfigured) return PurchaseResult.NotConfigured
        return suspendCancellableCoroutine { cont ->
            val params = PurchaseParams.Builder(activity, pkg).build()
            Purchases.sharedInstance.purchase(
                purchaseParams = params,
                callback = object : PurchaseCallback {
                    override fun onCompleted(
                        storeTransaction: StoreTransaction,
                        customerInfo: CustomerInfo,
                    ) {
                        applyCustomerInfo(customerInfo)
                        if (!cont.isCompleted) {
                            cont.resume(
                                PurchaseResult.Success(
                                    customerInfo = customerInfo,
                                    isPro = customerInfo.hasEntitlement(),
                                ),
                            )
                        }
                    }

                    override fun onError(error: PurchasesError, userCancelled: Boolean) {
                        if (cont.isCompleted) return
                        cont.resume(
                            if (userCancelled) PurchaseResult.UserCancelled
                            else PurchaseResult.Error(error.message),
                        )
                    }
                },
            )
        }
    }

    override suspend fun restore(): PurchaseResult {
        if (!Purchases.isConfigured) return PurchaseResult.NotConfigured
        return try {
            val info = Purchases.sharedInstance.awaitRestore()
            applyCustomerInfo(info)
            PurchaseResult.Success(info, info.hasEntitlement())
        } catch (t: Throwable) {
            Log.w(TAG, "awaitRestore failed", t)
            PurchaseResult.Error(t.message ?: "Unable to restore purchases")
        }
    }

    private fun applyCustomerInfo(info: CustomerInfo) {
        val pro = info.hasEntitlement()
        _state.update { it.copy(customerInfo = info, isPro = pro, error = null) }
        persistCachedIsPro(pro)
    }

    private fun persistCachedIsPro(value: Boolean) {
        scope.launch { runCatching { cachedIsProWriter(value) } }
    }

    private fun CustomerInfo.hasEntitlement(): Boolean =
        entitlements[entitlementId]?.isActive == true

    companion object {
        private const val TAG = "SubscriptionManager"
    }
}
