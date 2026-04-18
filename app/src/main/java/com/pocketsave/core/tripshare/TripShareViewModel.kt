package com.pocketsave.core.tripshare

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pocketsave.common.util.ColorOption
import com.pocketsave.common.util.ShareHelper
import com.pocketsave.core.cart.CartDetailUiState
import com.pocketsave.core.service.VaultService
import com.pocketsave.data.prefs.CartBackgroundStore
import com.pocketsave.domain.model.CartStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Port of `PocketSave/Core/TripShare/ViewModel/TripShareViewModel.swift`.
 *
 * Responsibilities:
 *   - Resolve the cart + shopping state + selected background (colour / image).
 *   - Render the share bitmap off the main thread via [TripShareBitmapRenderer].
 *   - Expose one-shot UI events (share intent, save feedback) to the Compose layer.
 *
 * The iOS view model also drives a timer that alternates between a "receipt"
 * view and a "physics" view. The physics mode depends on per-item images being
 * laid out via SwiftUI — there's no direct Compose equivalent without shipping
 * a custom physics layout. Phase 8 ships the receipt mode only; the view model
 * keeps an enum + flag so the physics mode can drop in later.
 */
class TripShareViewModel(
    private val context: Context,
    private val vaultService: VaultService,
    private val backgroundStore: CartBackgroundStore,
    private val cartId: String,
) : ViewModel() {

    enum class ViewType { RECEIPT, PHYSICS }

    data class UiState(
        val cartDetail: CartDetailUiState = CartDetailUiState(),
        val backgroundColor: ColorOption = ColorOption.defaultColor,
        val backgroundImageUri: String? = null,
        val viewType: ViewType = ViewType.RECEIPT,
        val isRendering: Boolean = false,
        val feedback: String? = null,
    )

    sealed interface OneShot {
        data class Share(val intent: android.content.Intent) : OneShot
        data class Feedback(val message: String) : OneShot
    }

    private val _oneShots = MutableStateFlow<OneShot?>(null)
    val oneShots: StateFlow<OneShot?> = _oneShots.asStateFlow()
    fun consumeOneShot() { _oneShots.value = null }

    private val _viewType = MutableStateFlow(ViewType.RECEIPT)
    private val _rendering = MutableStateFlow(false)
    private val _feedback = MutableStateFlow<String?>(null)

    val uiState: StateFlow<UiState> = combine(
        vaultService.state,
        backgroundStore.colorHex(cartId),
        backgroundStore.imageUri(cartId),
        _viewType,
        _rendering,
    ) { snapshot, colorHex, imageUri, viewType, rendering ->
        val cart = snapshot.carts.firstOrNull { it.id == cartId }
        val items = snapshot.cartItemsByCart[cartId].orEmpty()
        val status = cart?.let { CartStatus.fromRaw(it.status) } ?: CartStatus.PLANNING
        val totalSpent = cart?.let { vaultService.computeTotalSpent(status, items) } ?: 0.0
        val itemById = snapshot.items.associateBy { it.id }
        val rows = items.map { ci ->
            val name = when {
                ci.isShoppingOnlyItem -> ci.shoppingOnlyName.orEmpty()
                else -> itemById[ci.itemId]?.name
                    ?: ci.vaultItemNameSnapshot
                    ?: "Unknown"
            }
            val store = ci.actualStore ?: ci.plannedStore
            val price = ci.actualPrice ?: ci.plannedPrice ?: 0.0
            val qty = ci.actualQuantity ?: ci.quantity
            com.pocketsave.core.cart.CartDetailItemRow(
                cartItem = ci,
                vaultItem = itemById[ci.itemId],
                displayName = name,
                displayStore = store,
                price = price,
                unit = ci.actualUnit ?: ci.plannedUnit.orEmpty(),
                displayQuantity = qty,
                lineTotal = if (ci.isSkippedDuringShopping) 0.0 else price * qty,
            )
        }
        val sections = rows.groupBy { it.displayStore }.map { (store, storeRows) ->
            com.pocketsave.core.cart.StoreSection(
                store = store,
                rows = storeRows,
                storeTotal = storeRows.sumOf { it.lineTotal },
            )
        }
        val detail = CartDetailUiState(
            cart = cart,
            status = status,
            totalSpent = totalSpent,
            sections = sections,
            itemCount = items.size,
            fulfilledCount = items.count { it.isFulfilled && !it.isSkippedDuringShopping },
            skippedCount = items.count { it.isSkippedDuringShopping },
            isReady = cart != null,
        )
        val color = ColorOption.byHex(colorHex ?: "") ?: ColorOption.defaultColor
        UiState(
            cartDetail = detail,
            backgroundColor = color,
            backgroundImageUri = imageUri,
            viewType = viewType,
            isRendering = rendering,
            feedback = _feedback.value,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    /** Toggle between receipt and physics. Placeholder until physics mode lands. */
    fun setViewType(type: ViewType) {
        _viewType.value = type
    }

    /**
     * Render + hand off to the Android share chooser. Work runs on
     * [Dispatchers.Default] so the main thread stays free while the bitmap is
     * drawn; the resulting intent is surfaced via [oneShots].
     */
    fun share(currencyFormatter: com.pocketsave.core.currency.CurrencyFormatter) {
        viewModelScope.launch {
            val bitmap = renderBitmap(currencyFormatter) ?: return@launch
            val intent = ShareHelper.shareBitmap(
                context = context,
                bitmap = bitmap,
                subject = uiState.value.cartDetail.name.ifEmpty { "PocketSave trip" },
            )
            _oneShots.value = OneShot.Share(intent)
        }
    }

    /**
     * Port of iOS `saveToAlbum`. Writes the bitmap via [ShareHelper.saveBitmapToGallery]
     * and pushes a feedback string; the Compose layer can toast it.
     */
    fun saveToGallery(currencyFormatter: com.pocketsave.core.currency.CurrencyFormatter) {
        viewModelScope.launch {
            val bitmap = renderBitmap(currencyFormatter) ?: run {
                _feedback.value = "Couldn't render trip"
                return@launch
            }
            val ok = withContext(Dispatchers.IO) { ShareHelper.saveBitmapToGallery(context, bitmap) }
            _oneShots.value = OneShot.Feedback(if (ok) "Saved to Pictures" else "Save failed")
        }
    }

    private suspend fun renderBitmap(
        currencyFormatter: com.pocketsave.core.currency.CurrencyFormatter,
    ): Bitmap? {
        val state = uiState.value
        val cart = state.cartDetail.cart ?: return null
        _rendering.value = true
        return try {
            withContext(Dispatchers.Default) {
                TripShareBitmapRenderer(context).renderShareImage(
                    TripShareBitmapRenderer.RenderInput(
                        cart = cart,
                        ui = state.cartDetail,
                        backgroundColor = state.backgroundColor,
                        backgroundImageUri = state.backgroundImageUri,
                        captureDate = cart.completedAt ?: Date(),
                        currencyFormatter = currencyFormatter,
                    ),
                )
            }
        } finally {
            _rendering.value = false
        }
    }

    class Factory(
        private val context: Context,
        private val vaultService: VaultService,
        private val backgroundStore: CartBackgroundStore,
        private val cartId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TripShareViewModel::class.java))
            return TripShareViewModel(
                context = context.applicationContext,
                vaultService = vaultService,
                backgroundStore = backgroundStore,
                cartId = cartId,
            ) as T
        }
    }
}
