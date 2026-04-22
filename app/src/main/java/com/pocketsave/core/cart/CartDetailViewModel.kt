package com.pocketsave.core.cart

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pocketsave.core.service.VaultService
import com.pocketsave.data.local.entity.CartEntity
import com.pocketsave.data.local.entity.CartItemEntity
import com.pocketsave.data.local.entity.ItemEntity
import com.pocketsave.domain.model.CartStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Port of the iOS `CartDetailViewModel` concept (`PocketSave/Core/Detail Cart/ViewModel/CartDetailViewModel.swift`
 * — the live Swift file is commented out and the logic is inlined on the
 * view). Collapses the inlined logic back into a dedicated ViewModel and
 * supports all three cart statuses (Planning, Shopping, Completed) so the
 * phase-6 UI can switch per status.
 */
class CartDetailViewModel(
    private val vaultService: VaultService,
    private val cartId: String,
) : ViewModel() {

    val uiState: StateFlow<CartDetailUiState> = vaultService.state
        .map { snapshot -> buildUiState(snapshot) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CartDetailUiState.empty(cartId))

    // MARK: - Shared actions

    fun rename(newName: String) {
        viewModelScope.launch { vaultService.updateCartName(cartId, newName) }
    }

    fun updateBudget(newBudget: Double) {
        viewModelScope.launch { vaultService.updateCartBudget(cartId, newBudget) }
    }

    fun deleteCart(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val ok = vaultService.deleteCart(cartId)
            if (ok) onDeleted()
        }
    }

    // MARK: - Planning actions

    fun removeItem(itemId: String) {
        viewModelScope.launch { vaultService.removeItemFromCart(cartId, itemId) }
    }

    fun updatePlannedQuantity(itemId: String, newQuantity: Double) {
        viewModelScope.launch {
            vaultService.updateCartItemPlannedData(cartId, itemId, plannedQuantity = newQuantity)
        }
    }

    fun updatePlannedPrice(itemId: String, newPrice: Double?) {
        viewModelScope.launch {
            vaultService.updateCartItemPlannedData(cartId, itemId, plannedPrice = newPrice)
        }
    }

    fun updatePlannedUnit(itemId: String, newUnit: String) {
        viewModelScope.launch {
            vaultService.updateCartItemPlannedData(cartId, itemId, plannedUnit = newUnit)
        }
    }

    fun changeStore(itemId: String, newStore: String) {
        viewModelScope.launch { vaultService.changeCartItemStore(cartId, itemId, newStore) }
    }

    fun addVaultItemToCart(item: ItemEntity, quantity: Double, store: String?) {
        viewModelScope.launch { vaultService.addVaultItemToCart(cartId, item, quantity, store) }
    }

    fun startShopping() {
        viewModelScope.launch { vaultService.startShopping(cartId) }
    }

    // MARK: - Shopping actions

    fun toggleFulfilled(itemId: String) {
        viewModelScope.launch { vaultService.toggleItemFulfillment(cartId, itemId) }
    }

    fun skipDuringShopping(itemId: String) {
        viewModelScope.launch { vaultService.skipCartItemDuringShopping(cartId, itemId) }
    }

    fun unskip(itemId: String) {
        viewModelScope.launch { vaultService.unskipCartItem(cartId, itemId) }
    }

    fun updateActual(
        itemId: String,
        price: Double? = null,
        quantity: Double? = null,
        unit: String? = null,
        store: String? = null,
    ) {
        viewModelScope.launch {
            vaultService.updateCartItemActualData(
                cartId = cartId,
                itemId = itemId,
                actualPrice = price,
                actualQuantity = quantity,
                actualUnit = unit,
                actualStore = store,
            )
        }
    }

    fun changeActualStore(itemId: String, newStore: String) {
        viewModelScope.launch { vaultService.changeCartItemStoreShopping(cartId, itemId, newStore) }
    }

    fun addVaultItemDuringShopping(
        item: ItemEntity,
        store: String,
        price: Double,
        unit: String,
        quantity: Double,
    ) {
        viewModelScope.launch {
            vaultService.addVaultItemToCartDuringShopping(cartId, item, store, price, unit, quantity)
        }
    }

    fun addShoppingOnlyItem(
        name: String,
        store: String,
        price: Double,
        unit: String,
        quantity: Double,
        categoryName: String?,
        imageUri: String?,
    ) {
        viewModelScope.launch {
            vaultService.addShoppingOnlyItemToCart(
                cartId = cartId,
                name = name,
                store = store,
                price = price,
                unit = unit,
                quantity = quantity,
                categoryName = categoryName,
                imageUri = imageUri,
            )
        }
    }

    fun returnToPlanning() {
        viewModelScope.launch { vaultService.returnToPlanning(cartId) }
    }

    suspend fun completeShoppingNow(): Boolean = vaultService.completeShopping(cartId)

    suspend fun loadInsights(): VaultService.CartInsights = vaultService.getCartInsights(cartId)

    // MARK: - Completed actions

    fun reopenCart(onReopened: (String) -> Unit) {
        viewModelScope.launch {
            val newId = vaultService.reopenCart(cartId) ?: return@launch
            onReopened(newId)
        }
    }

    private fun buildUiState(snapshot: VaultService.Snapshot): CartDetailUiState {
        val cart = snapshot.carts.firstOrNull { it.id == cartId }
            ?: return CartDetailUiState.empty(cartId)
        val items = snapshot.cartItemsByCart[cartId].orEmpty()
        val status = CartStatus.fromRaw(cart.status)
        // Use the pre-computed map from the snapshot rather than rebuilding it
        // per emission.
        val itemById = snapshot.itemsById

        val rows = items.map { cartItem -> toRow(cartItem, itemById, status, cart.createdAt) }
        val totalSpent = vaultService.computeTotalSpent(status, items)
        val fulfilledCount = items.count { it.isFulfilled && !it.isSkippedDuringShopping }
        val skippedCount = items.count { it.isSkippedDuringShopping }

        // Group rows by the store iOS uses for this status (plannedStore for
        // planning/shopping; actualStore for completed, falling back to planned).
        val storeSections = rows
            .groupBy { it.displayStore }
            .toSortedMap()
            .map { (store, storeRows) ->
                StoreSection(
                    store = store,
                    rows = storeRows.sortedByDescending { it.cartItem.addedAt ?: cart.createdAt },
                    storeTotal = storeRows.sumOf { it.lineTotal },
                )
            }

        return CartDetailUiState(
            cart = cart,
            status = status,
            totalSpent = totalSpent,
            fulfilledAmount = rows.filter { it.cartItem.isFulfilled && !it.cartItem.isSkippedDuringShopping }
                .sumOf { it.lineTotal },
            sections = storeSections,
            itemCount = items.size,
            fulfilledCount = fulfilledCount,
            skippedCount = skippedCount,
            isReady = true,
        )
    }

    private fun toRow(
        cartItem: CartItemEntity,
        itemById: Map<String, ItemEntity>,
        status: CartStatus,
        cartCreatedAt: java.util.Date,
    ): CartDetailItemRow {
        val vaultItem = itemById[cartItem.itemId]
        val displayName = when {
            cartItem.isShoppingOnlyItem -> cartItem.shoppingOnlyName.orEmpty()
            else -> vaultItem?.name
                ?: cartItem.vaultItemNameSnapshot
                ?: "Unknown Item"
        }

        // Resolve the per-status store (iOS `CartItem.getStore(cart:)`).
        val displayStore = when (status) {
            CartStatus.PLANNING, CartStatus.SHOPPING -> cartItem.plannedStore
            CartStatus.COMPLETED -> cartItem.actualStore ?: cartItem.plannedStore
        }

        // Resolve price/unit per status — matches iOS `getShoppingDisplayPrice`/`getUnit`.
        val (price, unit) = when (status) {
            CartStatus.PLANNING -> (cartItem.plannedPrice ?: 0.0) to cartItem.plannedUnit.orEmpty()
            CartStatus.SHOPPING -> {
                if (cartItem.isFulfilled || cartItem.wasEditedDuringShopping) {
                    (cartItem.actualPrice ?: cartItem.plannedPrice ?: 0.0) to
                        (cartItem.actualUnit ?: cartItem.plannedUnit ?: "")
                } else {
                    (cartItem.plannedPrice ?: 0.0) to cartItem.plannedUnit.orEmpty()
                }
            }
            CartStatus.COMPLETED -> (cartItem.actualPrice ?: cartItem.plannedPrice ?: 0.0) to
                (cartItem.actualUnit ?: cartItem.plannedUnit ?: "")
        }

        val quantity = when (status) {
            CartStatus.PLANNING -> cartItem.quantity
            CartStatus.SHOPPING -> {
                if (cartItem.isFulfilled || cartItem.wasEditedDuringShopping) {
                    cartItem.actualQuantity ?: cartItem.quantity
                } else cartItem.quantity
            }
            CartStatus.COMPLETED -> cartItem.actualQuantity ?: cartItem.quantity
        }

        val lineTotal = when {
            status == CartStatus.SHOPPING && cartItem.isSkippedDuringShopping -> 0.0
            status == CartStatus.COMPLETED && cartItem.isSkippedDuringShopping -> 0.0
            else -> price * quantity
        }

        return CartDetailItemRow(
            cartItem = cartItem,
            vaultItem = vaultItem,
            displayName = displayName,
            displayStore = displayStore,
            price = price,
            unit = unit,
            displayQuantity = quantity,
            lineTotal = lineTotal,
        )
    }

    class Factory(
        private val vaultService: VaultService,
        private val cartId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(CartDetailViewModel::class.java))
            return CartDetailViewModel(vaultService, cartId) as T
        }
    }
}

@Immutable
data class CartDetailUiState(
    val cart: CartEntity? = null,
    val status: CartStatus = CartStatus.PLANNING,
    val totalSpent: Double = 0.0,
    val fulfilledAmount: Double = 0.0,
    val sections: List<StoreSection> = emptyList(),
    val itemCount: Int = 0,
    val fulfilledCount: Int = 0,
    val skippedCount: Int = 0,
    val isReady: Boolean = false,
) {
    val budget: Double get() = cart?.budget ?: 0.0
    val name: String get() = cart?.name.orEmpty()
    val remaining: Double get() = budget - totalSpent
    val isOverBudget: Boolean get() = totalSpent > budget && budget > 0.0
    val progress: Double
        get() = when (status) {
            CartStatus.PLANNING -> if (budget > 0.0) (totalSpent / budget).coerceIn(0.0, 1.0) else 0.0
            CartStatus.SHOPPING -> if (itemCount == 0) 0.0 else fulfilledCount.toDouble() / itemCount
            CartStatus.COMPLETED -> 1.0
        }

    companion object {
        fun empty(@Suppress("UNUSED_PARAMETER") cartId: String) = CartDetailUiState()
    }
}

@Immutable
data class StoreSection(
    val store: String,
    val rows: List<CartDetailItemRow>,
    val storeTotal: Double,
)

@Immutable
data class CartDetailItemRow(
    val cartItem: CartItemEntity,
    val vaultItem: ItemEntity?,
    val displayName: String,
    val displayStore: String,
    /** Price in the current status (planned, actual-or-planned, or actual). */
    val price: Double,
    val unit: String,
    /** Quantity in the current status (planned, actual-or-planned, or actual). */
    val displayQuantity: Double,
    val lineTotal: Double,
)
