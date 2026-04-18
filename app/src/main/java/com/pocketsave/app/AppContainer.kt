package com.pocketsave.app

import android.content.Context
import androidx.room.Room
import com.pocketsave.common.util.ImageStorage
import com.pocketsave.core.cart.VaultSelectionStore
import com.pocketsave.core.scanner.TextRecognitionService
import com.pocketsave.core.scanner.classifier.DisabledPackagingClassifier
import com.pocketsave.core.scanner.classifier.PackagingClassifier
import com.pocketsave.core.service.VaultService
import com.pocketsave.core.widget.WidgetSnapshotStore
import com.pocketsave.data.local.db.PocketSaveDatabase
import com.pocketsave.data.prefs.AppPreferences
import com.pocketsave.data.prefs.CartBackgroundStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
     * Subscribes to [VaultService.state] and refreshes the purchased-items
     * widget snapshot after each mutation (debounced inside the store).
     * Mirrors iOS `scheduleWidgetRefresh` flow.
     */
    val widgetSnapshotStore: WidgetSnapshotStore = WidgetSnapshotStore(
        context = context.applicationContext,
        preferences = preferences,
    ).also { it.start(vaultService) }

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
}
