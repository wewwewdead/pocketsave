package com.pocketsave.app

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.room.Room
import com.pocketsave.billing.BillingAnalytics
import com.pocketsave.billing.BillingConfig
import com.pocketsave.billing.LogcatBillingAnalytics
import com.pocketsave.billing.NoOpBillingAnalytics
import com.pocketsave.billing.RevenueCatSubscriptionManager
import com.pocketsave.billing.SubscriptionManager
import com.pocketsave.common.util.ImageStorage
import com.pocketsave.core.cart.VaultSelectionStore
import com.pocketsave.core.scanner.TextRecognitionService
import com.pocketsave.core.scanner.classifier.DisabledPackagingClassifier
import com.pocketsave.core.scanner.classifier.PackagingClassifier
import com.pocketsave.core.service.VaultService
import androidx.glance.appwidget.updateAll
import com.pocketsave.core.widget.PurchasedItemsGlanceWidget
import com.pocketsave.core.widget.WidgetSnapshotStore
import com.pocketsave.data.local.db.PocketSaveDatabase
import com.pocketsave.data.prefs.AppPreferences
import com.pocketsave.data.prefs.CartBackgroundStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android equivalent of iOS `ContentViewServices` in `PocketSave/App/GrockApp.swift`.
 *
 * Owns the long-lived singletons (database, preferences, VaultService,
 * selection store, image storage) so that configuration changes and screen
 * recreation don't reconstruct the data brain. Held by [PocketSaveApplication]
 * for the process lifetime.
 */
class AppContainer(context: Context) {

    val database: PocketSaveDatabase = Room.databaseBuilder(
        context.applicationContext,
        PocketSaveDatabase::class.java,
        PocketSaveDatabase.DATABASE_NAME,
    )
        .fallbackToDestructiveMigration()
        .build()

    val preferences: AppPreferences = AppPreferences(context.applicationContext)

    val imageStorage: ImageStorage = ImageStorage(context.applicationContext)

    val selectionStore: VaultSelectionStore = VaultSelectionStore()

    val cartBackgroundStore: CartBackgroundStore = CartBackgroundStore(context.applicationContext)

    /**
     * ML Kit Text Recognition + packaging classifier. The classifier ships as a
     * no-op until the Core ML → TFLite port lands; swap [DisabledPackagingClassifier]
     * for a real implementation here when the model is ready.
     */
    val textRecognitionService: TextRecognitionService = TextRecognitionService(context.applicationContext)
    val packagingClassifier: PackagingClassifier = DisabledPackagingClassifier

    val vaultService: VaultService = VaultService(database).also { it.loadUserAndVault() }

    /**
     * RevenueCat-backed billing layer. Constructed here, but
     * [SubscriptionManager.start] is deliberately invoked from
     * [com.pocketsave.PocketSaveApplication.onCreate] so the configure call
     * happens at the canonical Android startup location.
     *
     * The cache read/write lambdas bridge RevenueCat's `CustomerInfo` updates
     * into the existing DataStore-backed [AppPreferences.isProCached], giving
     * us a correct first-frame `isPro` on cold launch without having to wait
     * for the SDK's offerings/customer-info round-trip.
     */
    private val isDebuggable: Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    /**
     * Minimal billing analytics hook. Default is no-op in release so the
     * app ships no telemetry until a real sink is wired; debug builds route
     * to logcat under the `PocketSaveBilling` tag. Swap the concrete type
     * here when a real analytics backend lands — no feature code or paywall
     * code has to change.
     */
    val billingAnalytics: BillingAnalytics = if (isDebuggable) {
        LogcatBillingAnalytics()
    } else {
        NoOpBillingAnalytics
    }

    val subscriptionManager: SubscriptionManager = RevenueCatSubscriptionManager(
        applicationContext = context.applicationContext,
        apiKey = BillingConfig.REVENUECAT_ANDROID_API_KEY,
        entitlementId = BillingConfig.ENTITLEMENT_PRO,
        defaultOfferingId = BillingConfig.OFFERING_DEFAULT,
        debugLogging = isDebuggable,
        cachedIsProReader = { preferences.isProCachedNow() },
        cachedIsProWriter = { value -> preferences.setIsProCached(value) },
    )

    /**
     * Subscribes to [VaultService.state] and refreshes the purchased-items
     * widget snapshot after each mutation (debounced inside the store).
     * Mirrors iOS `scheduleWidgetRefresh` flow.
     */
    val widgetSnapshotStore: WidgetSnapshotStore = WidgetSnapshotStore(
        context = context.applicationContext,
        preferences = preferences,
    ).also { it.start(vaultService) }

    /**
     * Container-owned scope for cross-module reactions that need to survive
     * screen-scoped lifecycles (e.g. widget refresh on entitlement flip).
     * Uses a supervisor so one failure doesn't tear the collector down.
     */
    private val containerScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        val appContext = context.applicationContext
        // Refresh the home-screen widget whenever the Pro entitlement flips.
        // The widget reads `AppPreferences.isProCached` on every
        // `provideGlance`, but Glance only re-invokes that when the system
        // decides to update — we want an immediate refresh so the upsell
        // tile disappears the second a user subscribes (and reappears if
        // they cancel). `drop(1)` skips the initial replay so cold-start
        // doesn't trigger a redundant update.
        containerScope.launch {
            subscriptionManager.isPro
                .drop(1)
                .collect {
                    runCatching {
                        PurchasedItemsGlanceWidget().updateAll(appContext)
                    }
                }
        }
    }

    /**
     * Most-recent pending deep link (`pocketsave://…`). MainActivity parses the
     * intent and pushes the resolved [PendingDeepLink] here; the cart detail
     * screen reads it once, takes the post-navigation action, then clears.
     */
    private val _pendingDeepLink = MutableStateFlow<PendingDeepLink?>(null)
    val pendingDeepLink: StateFlow<PendingDeepLink?> = _pendingDeepLink.asStateFlow()

    fun submitDeepLink(link: PendingDeepLink?) {
        _pendingDeepLink.value = link
    }

    fun consumeDeepLink() {
        _pendingDeepLink.value = null
    }

    /**
     * Reset-App orchestration: wipe the database, the preference store, the
     * per-cart background store, and stored item images. Any dangling in-memory
     * state (selection store, pending deep link) is also cleared so the UI
     * can't replay a stale choice after the reset.
     *
     * Returns once everything on disk and in memory is back to first-run state.
     * Callers decide where to navigate next (usually back to onboarding).
     */
    suspend fun resetAllData() = withContext(Dispatchers.IO) {
        // All DB work must run off the main thread — Room's clearAllTables()
        // is @WorkerThread and loadUserAndVaultBlocking() hits DAOs
        // synchronously. The UI caller uses the default Main dispatcher, so
        // we switch once here instead of sprinkling withContext around every
        // blocking call below.
        vaultService.resetAllData()
        preferences.clearAll()
        cartBackgroundStore.clearAll()
        imageStorage.clearAll()
        selectionStore.clearAll()
        _pendingDeepLink.value = null
        // Re-seed the user/vault row so the next onboarding run can operate
        // against a live baseline — loadUserAndVaultBlocking handles the
        // "no user present" branch and publishes a fresh snapshot.
        vaultService.loadUserAndVaultBlocking()
    }
}
