package com.pocketsave.core.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pocketsave.common.util.ColorOption
import com.pocketsave.core.cart.VaultSelectionStore
import com.pocketsave.core.scanner.TextRecognitionService
import com.pocketsave.core.scanner.classifier.PackagingClassifier
import com.pocketsave.core.service.VaultService
import com.pocketsave.data.local.entity.ItemEntity
import com.pocketsave.data.local.entity.CategoryEntity
import kotlinx.coroutines.launch

/**
 * Port of `PocketSave/Core/Vault/VaultView.swift` (+ `VaultMainContent`,
 * `VaultToolbarView`, `VaultCategoryStripView`, `VaultCategoryContentView`,
 * `VaultItemRow`). Collapsed into a single file to match the Android feature
 * layout; sub-composables follow the same breakdown as iOS private views.
 *
 * Paywall-specific affordances (locked icon, upgrade popover, plan-suppressed
 * item dimming, celebration overlay) are omitted — this phase is monetization
 * free by design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    vaultService: VaultService,
    selectionStore: VaultSelectionStore,
    textRecognitionService: TextRecognitionService,
    packagingClassifier: PackagingClassifier,
    onBack: () -> Unit,
    onCreateCartRequested: (cartId: String) -> Unit,
) {
    val viewModel: VaultViewModel = viewModel(
        factory = VaultViewModel.Factory(vaultService, selectionStore),
    )
    val ui by viewModel.uiState.collectAsState()
    val searchText by viewModel.searchText.collectAsState()
    val selected by viewModel.selectedCategoryName.collectAsState()

    var showAddItemSheet by remember { mutableStateOf(false) }
    var showCategoryManager by remember { mutableStateOf(false) }
    var showCreateCartSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            VaultTopBar(
                onBack = onBack,
                onManageCategories = { showCategoryManager = true },
            )
        },
        bottomBar = {
            // Route the selection bar through Scaffold's bottom-bar slot so it
            // lives in a dedicated band instead of overlapping the FAB. When
            // no items are picked the slot collapses to zero-height so the
            // layout still feels empty.
            if (ui.activeSelectionCount > 0) {
                BottomSelectionBar(
                    count = ui.activeSelectionCount,
                    onClear = viewModel::clearSelection,
                    onContinue = { showCreateCartSheet = true },
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddItemSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add item") },
            )
        },
    ) { inner ->
        Column(modifier = Modifier.padding(inner).fillMaxSize()) {
            SearchField(
                value = searchText,
                onChange = viewModel::onSearchTextChange,
            )
            CategoryStrip(
                categories = ui.categories,
                selectedName = ui.selectedCategoryName ?: selected,
                activeCount = { name -> ui.sections.firstOrNull { it.category.name == name }?.activeItemCount ?: 0 },
                iconKey = { cat ->
                    ui.sections.firstOrNull { it.category.uid == cat.uid }?.iconKey
                        ?: vaultService.displayIconKeyForCategory(cat)
                },
                onSelect = viewModel::selectCategory,
            )
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    !ui.isReady -> LoadingState()
                    ui.totalItems == 0 -> EmptyState(search = ui.searchQuery)
                    else -> VaultItemList(
                        ui = ui,
                        onEdit = viewModel::requestEditItem,
                        onDelete = viewModel::requestDeleteItem,
                        onQuantityChange = viewModel::updateSelectionQuantity,
                    )
                }
            }
        }
    }

    if (showAddItemSheet) {
        ItemFormSheet(
            vaultService = vaultService,
            existing = null,
            initialCategoryName = ui.selectedCategoryName,
            onDismiss = { showAddItemSheet = false },
            onSaved = { showAddItemSheet = false },
            textRecognitionService = textRecognitionService,
            packagingClassifier = packagingClassifier,
        )
    }

    viewModel.itemToEdit?.let { editing ->
        ItemFormSheet(
            vaultService = vaultService,
            existing = editing,
            initialCategoryName = null,
            onDismiss = viewModel::clearEditRequest,
            onSaved = viewModel::clearEditRequest,
            textRecognitionService = textRecognitionService,
            packagingClassifier = packagingClassifier,
        )
    }

    viewModel.itemToDelete?.let { deleting ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch { viewModel.confirmDeleteItem() }
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) { Text("Cancel") }
            },
            title = { Text("Remove item") },
            text = {
                Text("Remove ${deleting.name} from your vault? It stays available for completed carts.")
            },
        )
    }

    if (showCategoryManager) {
        CategoriesManagerSheet(
            vaultService = vaultService,
            onDismiss = { showCategoryManager = false },
        )
    }

    if (showCreateCartSheet) {
        com.pocketsave.core.cart.CreateCartSheet(
            vaultService = vaultService,
            selectionStore = selectionStore,
            selectedItemCount = ui.activeSelectionCount,
            onDismiss = { showCreateCartSheet = false },
            onCreated = { cartId ->
                showCreateCartSheet = false
                onCreateCartRequested(cartId)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultTopBar(onBack: () -> Unit, onManageCategories: () -> Unit) {
    TopAppBar(
        title = { Text("Vault") },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onManageCategories) {
                Icon(Icons.Default.Category, contentDescription = "Manage categories")
            }
        },
    )
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text("Search items") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun CategoryStrip(
    categories: List<CategoryEntity>,
    selectedName: String?,
    activeCount: (String) -> Int,
    iconKey: (CategoryEntity) -> String,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories, key = { it.uid }) { category ->
            val selected = category.name == selectedName
            val count = activeCount(category.name)
            val labelText = if (count > 0) "${category.name} • $count" else category.name
            val tint = category.colorHex
                ?.let { ColorOption.byHex(it)?.color ?: ColorOption.parseHex(it) }
                ?: MaterialTheme.colorScheme.onSurface
            FilterChip(
                selected = selected,
                onClick = { onSelect(category.name) },
                label = {
                    Text(
                        text = labelText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = com.pocketsave.core.vault.icons.AppIcon.resolveIcon(iconKey(category)),
                        contentDescription = null,
                        tint = tint,
                    )
                },
            )
        }
    }
}

@Composable
private fun VaultItemList(
    ui: VaultUiState,
    onEdit: (ItemEntity) -> Unit,
    onDelete: (ItemEntity) -> Unit,
    onQuantityChange: (String, String?, Double) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp),
    ) {
        ui.sections.forEach { section ->
            if (section.items.isEmpty()) return@forEach
            item(key = "header-${section.category.uid}") {
                CategoryHeader(
                    iconKey = section.iconKey,
                    colorHex = section.category.colorHex,
                    name = section.category.name,
                    count = section.items.size,
                )
            }
            items(items = section.items, key = { it.item.id }) { row ->
                VaultItemRowView(
                    row = row,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onQuantityChange = onQuantityChange,
                )
            }
        }
    }
}

@Composable
private fun CategoryHeader(iconKey: String, colorHex: String?, name: String, count: Int) {
    val tint = colorHex
        ?.let { ColorOption.byHex(it)?.color ?: ColorOption.parseHex(it) }
        ?: MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = com.pocketsave.core.vault.icons.AppIcon.resolveIcon(iconKey),
            contentDescription = null,
            tint = tint,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun VaultItemRowView(
    row: VaultItemRow,
    onEdit: (ItemEntity) -> Unit,
    onDelete: (ItemEntity) -> Unit,
    onQuantityChange: (String, String?, Double) -> Unit,
) {
    val primaryStore = row.primaryStore
    val currentQty = row.quantityForStore(primaryStore)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onEdit(row.item) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ItemThumbnail(uri = row.item.imageUri, fallbackEmoji = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val price = row.primaryPrice
                val unit = row.primaryUnit
                val subtitle = buildString {
                    if (primaryStore != null) append(primaryStore)
                    if (price != null) {
                        if (isNotEmpty()) append(" • ")
                        append("%.2f".format(price))
                        if (!unit.isNullOrEmpty()) append(" / $unit")
                    }
                }
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            QuantityStepper(
                quantity = currentQty,
                onDecrement = {
                    if (primaryStore != null) onQuantityChange(row.item.id, primaryStore, (currentQty - 1).coerceAtLeast(0.0))
                },
                onIncrement = {
                    if (primaryStore != null) onQuantityChange(row.item.id, primaryStore, currentQty + 1.0)
                },
                enabled = primaryStore != null,
            )
            IconButton(onClick = { onDelete(row.item) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete ${row.item.name}",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ItemThumbnail(uri: String?, fallbackEmoji: String?) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (fallbackEmoji != null) {
            Text(fallbackEmoji)
        } else {
            Text(
                text = "\uD83D\uDCE6", // 📦
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: Double,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    enabled: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onDecrement, enabled = enabled && quantity > 0) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease")
        }
        Text(
            text = if (quantity <= 0) "" else "${quantity.toInt()}",
            modifier = Modifier.width(24.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        IconButton(onClick = onIncrement, enabled = enabled) {
            Icon(Icons.Default.Add, contentDescription = "Increase")
        }
    }
}

@Composable
private fun BottomSelectionBar(
    count: Int,
    onClear: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Lives inside Scaffold's `bottomBar` slot — edge to edge and short so the
    // FAB can sit comfortably above it. The "Create cart" action is a filled
    // Button to stay readable against the primary background when the FAB
    // overlaps it visually.
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$count selected",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClear) { Text("Clear", color = Color.White) }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) { Text("Create cart") }
            // Reserve room on the right so the Scaffold's FAB doesn't float
            // directly over the CTA text.
            Spacer(Modifier.width(96.dp))
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Loading vault...")
    }
}

@Composable
private fun EmptyState(search: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (search.isNotBlank()) "No items match \"$search\"." else "Your vault is empty. Tap Add item to get started.",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
