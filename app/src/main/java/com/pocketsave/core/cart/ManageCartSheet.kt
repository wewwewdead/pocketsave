package com.pocketsave.core.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketsave.core.service.VaultService
import com.pocketsave.data.local.entity.CartEntity
import com.pocketsave.data.local.entity.ItemEntity
import com.pocketsave.data.local.entity.PriceOptionEntity

/**
 * Port of `Core/Detail Cart/Views/ManageCartSheet.swift` + `ManageCartBrowseViewModel.swift`
 * + `Shopping Cart/BrowseVaultView.swift`. Phase 5 collapses the iOS two-pane
 * manage / browse pair into a single sheet that adds vault items to an
 * existing cart via `addVaultItemToCart`.
 *
 * Matches iOS quantity semantics: a single tap on the `+` increments the
 * stored `CartItem.quantity` by 1 (via `VaultService.addVaultItemToCart`),
 * which in turn bumps the existing row rather than duplicating it. Store
 * selection honours the iOS `selectedStore` parameter so carts can stage the
 * same item at different stores side-by-side.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCartSheet(
    vaultService: VaultService,
    cart: CartEntity?,
    onAddItem: (ItemEntity, Double, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by vaultService.state.collectAsState()
    var search by remember { mutableStateOf("") }

    val trimmedSearch = search.trim()
    val filteredItems = remember(state.items, trimmedSearch) {
        if (trimmedSearch.isEmpty()) state.items.sortedByDescending { it.createdAt }
        else state.items.filter { it.name.contains(trimmedSearch, ignoreCase = true) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Add to ${cart?.name ?: "cart"}",
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search vault") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (filteredItems.isEmpty()) {
                Text(
                    text = if (trimmedSearch.isEmpty())
                        "Your vault is empty — add items first."
                    else
                        "No items match \"$trimmedSearch\".",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 480.dp),
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        val options = state.priceOptionsByItem[item.id].orEmpty()
                        BrowseRow(
                            item = item,
                            priceOptions = options,
                            onAdd = { qty, store -> onAddItem(item, qty, store) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowseRow(
    item: ItemEntity,
    priceOptions: List<PriceOptionEntity>,
    onAdd: (Double, String?) -> Unit,
) {
    val formatter = com.pocketsave.core.currency.LocalCurrencyFormatter.current
    var selectedStore by remember(item.id, priceOptions) {
        mutableStateOf(priceOptions.firstOrNull()?.store)
    }

    val selectedOption = priceOptions.firstOrNull { it.store == selectedStore }
        ?: priceOptions.firstOrNull()

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (selectedOption != null) {
                val ppu = selectedOption.pricePerUnit
                val subtitle = buildString {
                    append(selectedOption.store)
                    append(" • ")
                    append(formatter.format(ppu.priceValue))
                    if (ppu.unit.isNotEmpty()) append(" / ${ppu.unit}")
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (priceOptions.size > 1) {
            StoreSelector(
                stores = priceOptions.map { it.store },
                selected = selectedStore,
                onSelect = { selectedStore = it },
            )
            Spacer(modifier = Modifier.padding(4.dp))
        }
        IconButton(onClick = { onAdd(1.0, selectedOption?.store) }) {
            Icon(Icons.Default.Add, contentDescription = "Add ${item.name} to cart")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreSelector(
    stores: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Store") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            stores.forEach { store ->
                DropdownMenuItem(
                    text = { Text(store) },
                    onClick = { onSelect(store); expanded = false },
                )
            }
        }
    }
}
