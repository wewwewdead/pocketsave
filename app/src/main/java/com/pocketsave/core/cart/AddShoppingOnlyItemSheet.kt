package com.pocketsave.core.cart

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pocketsave.common.util.ImageStorage
import com.pocketsave.core.service.VaultService
import com.pocketsave.data.local.entity.ItemEntity
import com.pocketsave.domain.model.GroceryCategory
import com.pocketsave.domain.semantics.UnitMenuCatalog
import com.pocketsave.domain.semantics.UnitSemantics
import kotlinx.coroutines.launch

/**
 * Port of iOS `Shopping Cart/ShoppingAddItemCard.swift` + `AddNewItemToCartSheet.swift`.
 *
 * iOS presents a single sheet during shopping that can both (a) pull a vault
 * item into the cart with tapped-right-now data and (b) create a
 * shopping-only item. Phase 6 keeps the two flows under one sheet with a
 * segmented-button toggle, matching the iOS mental model.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShoppingOnlyItemSheet(
    vaultService: VaultService,
    onDismiss: () -> Unit,
    onAddVaultItem: (ItemEntity, String, Double, String, Double) -> Unit,
    onAddShoppingOnly: (name: String, store: String, price: Double, unit: String, qty: Double, category: String?, imageUri: String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snapshot by vaultService.state.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Add to cart", style = MaterialTheme.typography.titleLarge)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val labels = listOf("From vault", "Shopping only")
                labels.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = labels.size),
                    ) { Text(label) }
                }
            }

            when (selectedTabIndex) {
                0 -> VaultPicker(
                    vaultItems = snapshot.items,
                    priceOptionsByItem = snapshot.priceOptionsByItem,
                    onAdd = { item, store, price, unit, qty ->
                        onAddVaultItem(item, store, price, unit, qty)
                    },
                )
                else -> ShoppingOnlyForm(
                    storeOptions = snapshot.stores.map { it.name },
                    onSubmit = { name, store, price, unit, qty, category, imageUri ->
                        onAddShoppingOnly(name, store, price, unit, qty, category, imageUri)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultPicker(
    vaultItems: List<ItemEntity>,
    priceOptionsByItem: Map<String, List<com.pocketsave.data.local.entity.PriceOptionEntity>>,
    onAdd: (ItemEntity, String, Double, String, Double) -> Unit,
) {
    val formatter = com.pocketsave.core.currency.LocalCurrencyFormatter.current
    var search by remember { mutableStateOf("") }
    val trimmed = search.trim()
    val filtered = if (trimmed.isEmpty()) vaultItems.sortedByDescending { it.createdAt }
    else vaultItems.filter { it.name.contains(trimmed, ignoreCase = true) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search vault") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (filtered.isEmpty()) {
            Text(
                text = if (trimmed.isEmpty()) "Your vault is empty." else "No matches.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 360.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                for (item in filtered) {
                    val options = priceOptionsByItem[item.id].orEmpty()
                    val primary = options.firstOrNull()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (primary != null) {
                                val ppu = primary.pricePerUnit
                                val subtitle = buildString {
                                    append(primary.store)
                                    append(" • ")
                                    append(formatter.format(ppu.priceValue))
                                    if (ppu.unit.isNotEmpty()) append(" / ${ppu.unit}")
                                }
                                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = {
                            val ppu = primary?.pricePerUnit
                            onAdd(
                                item,
                                primary?.store ?: "Unknown Store",
                                ppu?.priceValue ?: 0.0,
                                ppu?.unit ?: "pc",
                                1.0,
                            )
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add ${item.name}")
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingOnlyForm(
    storeOptions: List<String>,
    onSubmit: (name: String, store: String, price: Double, unit: String, qty: Double, category: String?, imageUri: String?) -> Unit,
) {
    val context = LocalContext.current
    val imageStorage = remember { ImageStorage(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var store by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pc") }
    var quantity by remember { mutableStateOf("1") }
    var category by remember { mutableStateOf<String?>(null) }
    var imageUri by remember { mutableStateOf<String?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val stored = imageStorage.saveFromUri(uri)
            if (stored != null) {
                val previous = imageUri
                imageUri = stored
                if (previous != null) imageStorage.deleteByUri(previous)
            }
        }
    }

    val isValid = name.trim().isNotEmpty() &&
        store.trim().isNotEmpty() &&
        (price.toDoubleOrNull() ?: 0.0) > 0.0 &&
        (quantity.toDoubleOrNull() ?: 0.0) > 0.0

    Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ImageChooser(
            uri = imageUri,
            onPick = { pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onClear = {
                val previous = imageUri
                imageUri = null
                if (previous != null) imageStorage.deleteByUri(previous)
            },
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        StoreInput(store = store, options = storeOptions, onChange = { store = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = price,
                onValueChange = { raw ->
                    if (raw.isEmpty() || raw.matches(Regex("^\\d*[.,]?\\d{0,2}$"))) {
                        price = raw.replace(',', '.')
                    }
                },
                label = { Text("Price") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = quantity,
                onValueChange = { raw ->
                    if (raw.isEmpty() || raw.matches(Regex("^\\d*[.,]?\\d*$"))) {
                        quantity = raw.replace(',', '.')
                    }
                },
                label = { Text("Qty") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier.weight(1f),
            )
        }
        UnitDropdown(selected = unit, onSelect = { unit = it })
        CategoryDropdown(selected = category, onSelect = { category = it })

        Button(
            onClick = {
                onSubmit(
                    name.trim(),
                    store.trim(),
                    price.toDoubleOrNull() ?: 0.0,
                    unit,
                    quantity.toDoubleOrNull() ?: 1.0,
                    category,
                    imageUri,
                )
            },
            enabled = isValid,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Add to cart") }
    }
}

@Composable
private fun ImageChooser(uri: String?, onPick: () -> Unit, onClear: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onPick),
            contentAlignment = Alignment.Center,
        ) {
            if (uri != null) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Default.CameraAlt, contentDescription = "Add photo")
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            TextButton(onClick = onPick) { Text(if (uri == null) "Add photo" else "Replace photo") }
            if (uri != null) TextButton(onClick = onClear) { Text("Remove photo") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreInput(store: String, options: List<String>, onChange: (String) -> Unit) {
    if (options.isEmpty()) {
        OutlinedTextField(
            value = store,
            onValueChange = onChange,
            label = { Text("Store") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = store,
            onValueChange = onChange,
            label = { Text("Store") },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onChange(option); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = UnitMenuCatalog.continuousOptions + UnitMenuCatalog.discreteOptions
    val displayLabel = options.firstOrNull { it.abbr == selected }?.displayText ?: selected
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = displayLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Unit") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayText) },
                    onClick = { onSelect(UnitSemantics.canonicalUnit(option.abbr)); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(selected: String?, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Category (optional)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("None") }, onClick = { onSelect(null); expanded = false })
            GroceryCategory.entries.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.title) },
                    leadingIcon = {
                        Icon(
                            imageVector = com.pocketsave.core.vault.icons.AppIcon.resolveIcon(cat.defaultIconKey),
                            contentDescription = null,
                        )
                    },
                    onClick = { onSelect(cat.title); expanded = false },
                )
            }
        }
    }
}
