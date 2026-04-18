package com.pocketsave.core.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.pocketsave.core.currency.CurrencyFormatter
import com.pocketsave.core.currency.CurrencyPreference
import com.pocketsave.core.service.VaultService
import com.pocketsave.data.local.entity.CartEntity
import com.pocketsave.data.local.entity.CartItemEntity
import com.pocketsave.data.local.entity.ItemEntity
import com.pocketsave.data.prefs.AppPreferences
import com.pocketsave.domain.model.CartStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Port of iOS `PocketSavePurchasedItemsWidgetSnapshotWriter`.
 *
 * Role:
 *   - Subscribe to [VaultService.state] and produce a snapshot whenever cart
 *     data changes.
 *   - Persist the snapshot as JSON under app-private `filesDir/widget/`.
 *   - Trigger the Glance widget to re-render.
 *
 * The subscription is debounced (500ms) so a burst of cart mutations during
 * shopping coalesces into a single snapshot write, matching the iOS
 * `scheduleWidgetRefresh` debouncing semantics.
 */
class WidgetSnapshotStore(
    private val context: Context,
    private val preferences: AppPreferences,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    private var subscription: Job? = null

    @OptIn(FlowPreview::class)
    fun start(vaultService: VaultService) {
        subscription?.cancel()
        subscription = vaultService.state
            .debounce(500)
            .onEach { snapshot ->
                try {
                    val formatter = resolveCurrencyFormatter()
                    val widgetSnapshot = buildSnapshot(snapshot, formatter)
                    writeSnapshot(widgetSnapshot)
                    requestWidgetRefresh()
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to refresh widget snapshot", t)
                }
            }
            .launchIn(scope)
    }

    private suspend fun resolveCurrencyFormatter(): CurrencyFormatter {
        val code = preferences.selectedCurrencyCode.first()
        val symbol = preferences.selectedCurrencySymbol.first()
        val preference = if (code != null) {
            CurrencyPreference(code = code, symbol = symbol ?: code)
        } else {
            CurrencyPreference.fromLocale()
        }
        return CurrencyFormatter(preference)
    }

    /**
     * Mirrors iOS `preferredCart` — picks the most recent shopping cart, falls
     * back to the newest non-deleted cart.
     */
    private fun buildSnapshot(
        state: VaultService.Snapshot,
        formatter: CurrencyFormatter,
    ): PurchasedItemsWidgetSnapshot {
        val shoppingCarts = state.carts.filter { CartStatus.fromRaw(it.status) == CartStatus.SHOPPING }
        val preferred = shoppingCarts.maxByOrNull { cartRecencyKey(it) }
            ?: state.carts.maxByOrNull { cartRecencyKey(it) }
            ?: return PurchasedItemsWidgetSnapshot(generatedAtMillis = System.currentTimeMillis())

        val items = state.cartItemsByCart[preferred.id].orEmpty()
        val itemById = state.items.associateBy { it.id }

        val fulfilledItems = items.filter { isPurchased(it) }.sortedWith(
            compareByDescending<CartItemEntity> { it.addedAt?.time ?: Long.MIN_VALUE }
                .thenBy { displayName(it, itemById).lowercase() },
        )
        val fulfilledIds = fulfilledItems.map { it.itemId }.toSet()

        val activeItems = items.filter { isActiveForWidget(it) }
            .sortedBy { it.addedAt?.time ?: Long.MAX_VALUE }

        val itemSnapshots = activeItems.map { cartItem ->
            val vaultItem = itemById[cartItem.itemId]
            PurchasedItemsWidgetSnapshot.ItemSnapshot(
                id = cartItem.itemId,
                name = displayName(cartItem, itemById),
                imageUri = imageUriFor(cartItem, vaultItem),
                isFulfilled = cartItem.itemId in fulfilledIds,
            )
        }

        val totalSpending = fulfilledItems.sumOf {
            val price = it.actualPrice ?: it.plannedPrice ?: 0.0
            val qty = it.actualQuantity ?: it.quantity
            price * qty
        }

        return PurchasedItemsWidgetSnapshot(
            generatedAtMillis = System.currentTimeMillis(),
            cart = PurchasedItemsWidgetSnapshot.CartSnapshot(
                id = preferred.id,
                name = preferred.name,
                status = statusLabel(preferred),
                purchasedCount = fulfilledItems.size,
                totalCount = activeItems.count { !it.isSkippedDuringShopping && it.quantity > 0 },
                totalSpending = totalSpending,
                totalSpendingLabel = formatter.format(totalSpending),
                items = itemSnapshots,
            ),
        )
    }

    private fun isPurchased(cartItem: CartItemEntity): Boolean {
        if (!cartItem.isFulfilled || cartItem.isSkippedDuringShopping) return false
        val quantity = cartItem.actualQuantity ?: cartItem.quantity
        return quantity > 0.0
    }

    private fun isActiveForWidget(cartItem: CartItemEntity): Boolean {
        if (cartItem.isSkippedDuringShopping) return false
        val quantity = cartItem.actualQuantity ?: cartItem.quantity
        return quantity > 0.0
    }

    private fun displayName(cartItem: CartItemEntity, itemById: Map<String, ItemEntity>): String =
        itemById[cartItem.itemId]?.name
            ?: cartItem.shoppingOnlyName
            ?: cartItem.vaultItemNameSnapshot
            ?: "Unknown Item"

    private fun imageUriFor(cartItem: CartItemEntity, vaultItem: ItemEntity?): String? =
        vaultItem?.imageUri ?: cartItem.shoppingOnlyImageUri

    private fun statusLabel(cart: CartEntity): String = when (CartStatus.fromRaw(cart.status)) {
        CartStatus.PLANNING -> "Planning"
        CartStatus.SHOPPING -> "Shopping"
        CartStatus.COMPLETED -> "Completed"
    }

    private fun cartRecencyKey(cart: CartEntity): Long {
        val started = cart.startedAt?.time ?: 0L
        val completed = cart.completedAt?.time ?: 0L
        return maxOf(cart.updatedAt.time, started, completed, cart.createdAt.time)
    }

    private fun writeSnapshot(snapshot: PurchasedItemsWidgetSnapshot) {
        val dir = File(context.filesDir, WIDGET_DIR).apply { if (!exists()) mkdirs() }
        val file = File(dir, SNAPSHOT_FILE)
        file.writeText(json.encodeToString(PurchasedItemsWidgetSnapshot.serializer(), snapshot))
    }

    /**
     * Tell both Glance and AppWidgetManager to re-render. The
     * `GlanceAppWidgetManager.updateAll` call does a proper Compose-side
     * refresh; the AppWidgetManager broadcast is a belt-and-suspenders fallback
     * for hosts that missed the Glance update.
     */
    private suspend fun requestWidgetRefresh() {
        runCatching {
            PurchasedItemsGlanceWidget().updateAll(context)
        }
        runCatching {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, PurchasedItemsWidgetReceiver::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                context.sendBroadcast(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                        setPackage(context.packageName)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    },
                )
            }
        }
    }

    companion object {
        private const val WIDGET_DIR = "widget"
        private const val SNAPSHOT_FILE = "purchased_items.json"
        private const val TAG = "WidgetSnapshotStore"

        /** Readers (widget provider) — synchronous because Glance providers run on IO. */
        fun readSnapshot(context: Context): PurchasedItemsWidgetSnapshot? {
            val file = File(File(context.filesDir, WIDGET_DIR), SNAPSHOT_FILE)
            if (!file.exists()) return null
            return runCatching {
                Json { ignoreUnknownKeys = true }
                    .decodeFromString(PurchasedItemsWidgetSnapshot.serializer(), file.readText())
            }.getOrNull()
        }
    }
}

