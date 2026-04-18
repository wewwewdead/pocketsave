package com.pocketsave.core.cart

import com.pocketsave.common.util.ActiveItemSelectionKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Port of iOS `VaultItemQuantityStore` — the interface both `CartViewModel` and
 * `ManageCartQuantityStore` conform to (`PocketSave/Core/Vault/ViewModel/ManageCartQuantityStore.swift`).
 *
 * During Phase 4 this is the sole implementation: the Vault screen uses it to
 * stage item + store + quantity tuples that will be handed to the real
 * `CartViewModel` when the Cart phase lands. Keys are produced by
 * [ActiveItemSelectionKey] so a single item with multiple stores can appear as
 * separate selections (same as iOS).
 */
class VaultSelectionStore {

    private val _activeCartItems = MutableStateFlow<Map<String, Double>>(emptyMap())
    val activeCartItems: StateFlow<Map<String, Double>> = _activeCartItems.asStateFlow()

    /**
     * Quantity of the (itemId, store) selection identified by [key], or 0 when
     * the selection isn't active. Mirrors iOS `quantityTracker(for:)`.
     */
    fun quantity(key: String): Double = _activeCartItems.value[key] ?: 0.0

    /** All selection keys for a given `itemId` — used by vault-item row UI. */
    fun selectionKeys(itemId: String): Set<String> =
        _activeCartItems.value.keys.filter { ActiveItemSelectionKey.itemId(fromKey = it) == itemId }.toSet()

    /** Port of `updateActiveItem(itemId:quantity:)`. Quantity <= 0 removes the key. */
    fun updateActiveItem(key: String, quantity: Double) {
        _activeCartItems.update { current ->
            if (quantity > 0.0) current + (key to quantity) else current - key
        }
    }

    fun clearAll() {
        _activeCartItems.value = emptyMap()
    }
}
