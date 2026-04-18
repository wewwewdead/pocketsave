package com.pocketsave.core.vault

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pocketsave.common.util.ActiveItemSelectionKey
import com.pocketsave.core.cart.VaultSelectionStore
import com.pocketsave.core.service.VaultService
import com.pocketsave.data.local.entity.CategoryEntity
import com.pocketsave.data.local.entity.ItemEntity
import com.pocketsave.data.local.entity.PriceOptionEntity
import com.pocketsave.domain.model.GroceryCategory
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn

/**
 * Port of iOS `VaultViewModel` (`PocketSave/Core/Vault/ViewModel/VaultViewModel.swift`)
 * merged with the `VaultView+*.swift` extension derivations (visible categories,
 * active-item counts, search matching). Paywall fields are omitted entirely.
 *
 * Keeps a single derived [uiState] so the Compose layer can render without
 * manual `combine` boilerplate, matching the way iOS builds its content from
 * `vaultService.vault` + `cartViewModel.activeCartItems` each render pass.
 */
class VaultViewModel(
    private val vaultService: VaultService,
    private val selectionStore: VaultSelectionStore,
) : ViewModel() {

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _selectedCategoryName = MutableStateFlow<String?>(null)
    val selectedCategoryName: StateFlow<String?> = _selectedCategoryName.asStateFlow()

    @OptIn(FlowPreview::class)
    private val debouncedSearch: StateFlow<String> = _searchText
        .debounce(200)
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val uiState: StateFlow<VaultUiState> = combine(
        vaultService.state,
        selectionStore.activeCartItems,
        debouncedSearch,
        _selectedCategoryName,
    ) { vault, active, search, selected ->
        buildUiState(vault, active, search, selected)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, VaultUiState())

    // Transient screen UI state that should trigger recomposition when the
    // vault screen opens edit/delete surfaces.
    var itemToDelete: ItemEntity? by mutableStateOf(null)
        private set
    var itemToEdit: ItemEntity? by mutableStateOf(null)
        private set

    fun onSearchTextChange(new: String) {
        _searchText.value = new
    }

    fun selectCategory(name: String?) {
        _selectedCategoryName.value = name
    }

    fun requestDeleteItem(item: ItemEntity) {
        itemToDelete = item
    }

    fun cancelDelete() {
        itemToDelete = null
    }

    suspend fun confirmDeleteItem(): Boolean {
        val target = itemToDelete ?: return false
        val result = vaultService.deleteItem(target)
        if (result) {
            // Drop any selection keys pointing at the deleted item — matches iOS
            // `cartViewModel.updateActiveItem(itemId: key, quantity: 0)` loop in
            // `VaultView+Actions.executeDelete()`.
            for (key in selectionStore.selectionKeys(target.id)) {
                selectionStore.updateActiveItem(key, 0.0)
            }
        }
        itemToDelete = null
        return result
    }

    fun requestEditItem(item: ItemEntity) {
        itemToEdit = item
    }

    fun clearEditRequest() {
        itemToEdit = null
    }

    fun updateSelectionQuantity(itemId: String, store: String?, quantity: Double) {
        val key = ActiveItemSelectionKey.make(itemId, store)
        selectionStore.updateActiveItem(key, quantity)
    }

    fun clearSelection() = selectionStore.clearAll()

    private fun buildUiState(
        vault: VaultService.Snapshot,
        activeCartItems: Map<String, Double>,
        search: String,
        selectedCategoryName: String?,
    ): VaultUiState {
        val orderedCategories = vault.categories.sortedBy { it.sortOrder }
        val visibleCategories = computeVisibleCategories(orderedCategories)

        val priceOptionsByItem = vault.priceOptionsByItem
        val activeItemIds = activeCartItems.keys
            .map { ActiveItemSelectionKey.itemId(fromKey = it) }
            .toSet()

        val sections = visibleCategories.map { category ->
            val items = vault.items
                .asSequence()
                .filter { it.categoryUid == category.uid }
                .filter { if (search.isBlank()) true else it.name.contains(search, ignoreCase = true) }
                .sortedByDescending { it.createdAt }
                .map { item ->
                    val options = priceOptionsByItem[item.id].orEmpty()
                    val quantityByKey = activeCartItems
                        .filterKeys { ActiveItemSelectionKey.itemId(fromKey = it) == item.id }
                    VaultItemRow(
                        item = item,
                        priceOptions = options,
                        quantityByKey = quantityByKey,
                    )
                }
                .toList()

            VaultCategorySection(
                category = category,
                iconKey = vaultService.displayIconKeyForCategory(category),
                activeItemCount = items.count { it.item.id in activeItemIds },
                items = items,
            )
        }

        val trimmedSelected = selectedCategoryName?.takeIf { it.isNotBlank() }
        val effectiveSelected = when {
            trimmedSelected != null && visibleCategories.any { it.name == trimmedSelected } -> trimmedSelected
            else -> sections.firstOrNull { it.items.isNotEmpty() }?.category?.name
                ?: visibleCategories.firstOrNull()?.name
        }

        return VaultUiState(
            isReady = !vault.isLoading && vault.vault != null,
            categories = visibleCategories,
            sections = sections,
            totalItems = sections.sumOf { it.items.size },
            activeSelectionCount = activeCartItems.size,
            searchQuery = search,
            selectedCategoryName = effectiveSelected,
        )
    }

    /**
     * iOS `VaultView+CategoryData.visibleCategories` resolves user-configured
     * visible categories (stored in `visibleCategoryNamesData`) against the
     * live category list. Phase 4 shows every stored category unconditionally;
     * the per-user visibility toggle lands with the Menu phase.
     */
    private fun computeVisibleCategories(categories: List<CategoryEntity>): List<CategoryEntity> {
        if (categories.isEmpty()) return emptyList()
        val defaultOrder = GroceryCategory.entries.mapIndexed { idx, cat -> cat.title to idx }.toMap()
        return categories.sortedWith(
            compareBy(
                { it.sortOrder },
                { defaultOrder[it.name] ?: Int.MAX_VALUE },
                { it.name.lowercase() },
            ),
        )
    }

    class Factory(
        private val vaultService: VaultService,
        private val selectionStore: VaultSelectionStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(VaultViewModel::class.java))
            return VaultViewModel(vaultService, selectionStore) as T
        }
    }
}

data class VaultUiState(
    val isReady: Boolean = false,
    val categories: List<CategoryEntity> = emptyList(),
    val sections: List<VaultCategorySection> = emptyList(),
    val totalItems: Int = 0,
    val activeSelectionCount: Int = 0,
    val searchQuery: String = "",
    val selectedCategoryName: String? = null,
)

data class VaultCategorySection(
    val category: CategoryEntity,
    val iconKey: String,
    val activeItemCount: Int,
    val items: List<VaultItemRow>,
)

data class VaultItemRow(
    val item: ItemEntity,
    val priceOptions: List<PriceOptionEntity>,
    /** Selection quantities keyed by [ActiveItemSelectionKey] for this item. */
    val quantityByKey: Map<String, Double>,
) {
    val primaryStore: String? get() = priceOptions.firstOrNull()?.store
    val primaryPrice: Double? get() = priceOptions.firstOrNull()?.pricePerUnit?.priceValue
    val primaryUnit: String? get() = priceOptions.firstOrNull()?.pricePerUnit?.unit

    /** Quantity for the row's primary store — drives the quantity stepper. */
    fun quantityForStore(store: String?): Double {
        val key = ActiveItemSelectionKey.make(item.id, store)
        return quantityByKey[key] ?: 0.0
    }

    val isSelected: Boolean get() = quantityByKey.isNotEmpty()
}
