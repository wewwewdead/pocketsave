package com.pocketsave.core.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pocketsave.billing.FeatureLimits
import com.pocketsave.billing.PremiumFeature
import com.pocketsave.billing.SubscriptionManager
import com.pocketsave.billing.rememberPaywallGate
import com.pocketsave.common.ui.AppShapes
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.common.ui.components.AffectionateEmpty
import com.pocketsave.common.ui.components.StickerChip
import com.pocketsave.common.ui.decor.grainOverlay
import com.pocketsave.common.util.ColorOption
import com.pocketsave.core.cart.VaultSelectionStore
import com.pocketsave.core.paywall.CapHintBanner
import com.pocketsave.core.scanner.TextRecognitionService
import com.pocketsave.core.scanner.classifier.PackagingClassifier
import com.pocketsave.core.service.VaultService
import com.pocketsave.data.local.entity.ItemEntity
import com.pocketsave.data.local.entity.CategoryEntity
import kotlinx.coroutines.delay
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
    subscriptionManager: SubscriptionManager,
    onBack: () -> Unit,
    onCreateCartRequested: (cartId: String) -> Unit,
    onOpenPaywall: (PremiumFeature) -> Unit,
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

    val paywallGate = rememberPaywallGate(subscriptionManager, vaultService, onOpenPaywall)
    val isPro by subscriptionManager.isPro.collectAsState()
    val requestAddItem: () -> Unit = {
        paywallGate.check(PremiumFeature.AddVaultItem) { showAddItemSheet = true }
    }
    val requestCreateCartFromSelection: () -> Unit = {
        paywallGate.check(PremiumFeature.CreateActiveTrip) { showCreateCartSheet = true }
    }
    // Scanner sits inside ItemFormSheet but is gated here so the sheet itself
    // remains billing-agnostic: it just delegates the "should I open scan?"
    // decision back to the caller's wrapper.
    val requestScanner: (onAllowed: () -> Unit) -> Unit = { onAllowed ->
        paywallGate.check(PremiumFeature.Scanner, onAllowed)
    }
    // Pass-through hints for the ItemFormSheet / CategoriesManagerSheet so
    // their locked affordances render a "PRO" pill before the user taps.
    val scannerLocked = !isPro
    val customCategoryCap = if (isPro) null else FeatureLimits.FREE_CUSTOM_CATEGORIES
    // Item-cap banner visibility. Only shown when a free user is *at* the
    // cap — below it the happy path stays uncluttered.
    val activeItemCount = ui.totalItems
    val showItemCapBanner = !isPro && activeItemCount >= FeatureLimits.FREE_VAULT_ITEMS
    val listState = rememberLazyListState()
    val flightState = rememberVaultFlightState()
    // The LazyColumn's own top-left in root coordinates. Captured via
    // `onGloballyPositioned` on the LazyColumn and read by the flight overlay
    // to compute the target row's landing point accurately. Starts at Zero;
    // the first layout pass fills it in before the 320 ms flight lead-in
    // finishes, so the ghost's first visible frame already targets correctly.
    var listRootOrigin by remember { mutableStateOf(Offset.Zero) }

    // When a flight is starting, scroll the LazyColumn so the target row is
    // on-screen by the time the ghost begins moving. We compute the absolute
    // LazyColumn index by walking the sections, accounting for the header
    // row per non-empty section. If the item isn't in the current sections
    // (e.g. filtered out by search), skip the scroll — the flight will still
    // run, just without a visible landing point.
    LaunchedEffect(flightState.flight?.itemId, ui.sections) {
        val targetId = flightState.flight?.itemId ?: return@LaunchedEffect
        val targetIndex = indexOfItemInSections(ui.sections, targetId)
        if (targetIndex >= 0) listState.animateScrollToItem(targetIndex)
    }

    // Auto-clear the "just landed" pulse after the hold expires. Owning the
    // delay job here (rather than inside VaultFlightState) keeps it bound to
    // this screen's composition scope — navigating away cancels cleanly.
    LaunchedEffect(flightState.justLandedId) {
        if (flightState.justLandedId != null) {
            delay(LANDED_PULSE_HOLD_MS)
            flightState.clearJustLanded()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    onContinue = requestCreateCartFromSelection,
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = requestAddItem,
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
            if (showItemCapBanner) {
                // Gentle inline notice — only visible at the cap, never below
                // it. Tapping anywhere opens the paywall with the AddVaultItem
                // trigger so the hero copy matches the reason.
                CapHintBanner(
                    label = "$activeItemCount of ${FeatureLimits.FREE_VAULT_ITEMS} items saved.",
                    onUpgrade = { onOpenPaywall(PremiumFeature.AddVaultItem) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    !ui.isReady -> LoadingState()
                    ui.totalItems == 0 -> EmptyState(search = ui.searchQuery)
                    else -> VaultItemList(
                        ui = ui,
                        listState = listState,
                        hiddenItemId = flightState.flight?.itemId,
                        justLandedItemId = flightState.justLandedId,
                        onListPositioned = { listRootOrigin = it },
                        onEdit = viewModel::requestEditItem,
                        onDelete = viewModel::requestDeleteItem,
                        onQuantityChange = viewModel::updateSelectionQuantity,
                    )
                }
            }
        }
    }

    // Flight overlay above the Scaffold — positioned in the same Box so its
    // root coordinates match the ones captured for the Save button.
    flightState.flight?.let { flight ->
        FlightGhost(
            flight = flight,
            listState = listState,
            listRootOrigin = listRootOrigin,
            // markLanded both clears the in-air flight AND signals the row
            // to run its arrival pulse. The row re-materialises at alpha=1
            // and scales up briefly as the ghost fades out on top of it.
            onFinished = { flightState.markLanded(flight.itemId) },
        )
    }
    } // end outer Box

    if (showAddItemSheet) {
        ItemFormSheet(
            vaultService = vaultService,
            existing = null,
            initialCategoryName = ui.selectedCategoryName,
            onDismiss = { showAddItemSheet = false },
            onSaved = { hint ->
                showAddItemSheet = false
                hint?.let {
                    flightState.start(
                        VaultFlight(
                            itemId = it.insertedItem.id,
                            itemName = it.insertedItem.name,
                            imageUri = it.insertedItem.imageUri,
                            originCenter = it.saveButtonCenter,
                        ),
                    )
                }
            },
            textRecognitionService = textRecognitionService,
            packagingClassifier = packagingClassifier,
            onScanRequested = requestScanner,
            scannerLocked = scannerLocked,
        )
    }

    viewModel.itemToEdit?.let { editing ->
        ItemFormSheet(
            vaultService = vaultService,
            existing = editing,
            initialCategoryName = null,
            onDismiss = viewModel::clearEditRequest,
            onSaved = { _ -> viewModel.clearEditRequest() },
            textRecognitionService = textRecognitionService,
            packagingClassifier = packagingClassifier,
            onScanRequested = requestScanner,
            scannerLocked = scannerLocked,
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
            onAddCategoryRequested = { onAllowed ->
                paywallGate.check(PremiumFeature.AddCustomCategory, onAllowed)
            },
            customCategoryCap = customCategoryCap,
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
        title = {
            Text(
                text = "Vault",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
                ),
            )
        },
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
    val pastels = PocketSaveTokens.pastels
    Surface(
        color = pastels.canvasTint,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = "Search your vault",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(
                        MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryStrip(
    categories: List<CategoryEntity>,
    selectedName: String?,
    activeCount: (String) -> Int,
    iconKey: (CategoryEntity) -> String,
    onSelect: (String) -> Unit,
) {
    // iOS CategoryStrip: 48×48 rounded 12dp tiles. Pastel background, emoji or
    // icon centered. Selected tile has a 2dp black inset outline.
    // Source: Core/Vault/Views/VaultCategorySectionView.swift (strip portion).
    val pastels = PocketSaveTokens.pastels
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(categories, key = { it.uid }) { category ->
            val selected = category.name == selectedName
            val index = categories.indexOf(category)
            val (softTint, _) = com.pocketsave.core.home.components.VaultPaletteCycle
                .tintFor(pastels, index, category.colorHex)
            CategoryTile(
                emoji = com.pocketsave.core.vault.icons.CategoryEmoji.resolve(iconKey(category)),
                tint = softTint,
                selected = selected,
                onClick = { onSelect(category.name) },
            )
        }
    }
}

@Composable
private fun CategoryTile(
    emoji: String,
    tint: androidx.compose.ui.graphics.Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val haptics = com.pocketsave.core.haptics.rememberAppHaptics()
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tint)
            .let { m ->
                if (selected) {
                    m.border(
                        width = 2.dp,
                        color = com.pocketsave.common.ui.PocketSaveColors.DarkPrimary,
                        shape = RoundedCornerShape(12.dp),
                    )
                } else {
                    m
                }
            }
            .clickable {
                haptics.perform(com.pocketsave.core.haptics.AppHaptic.Light)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun VaultItemList(
    ui: VaultUiState,
    listState: LazyListState,
    hiddenItemId: String?,
    justLandedItemId: String?,
    onListPositioned: (Offset) -> Unit,
    onEdit: (ItemEntity) -> Unit,
    onDelete: (ItemEntity) -> Unit,
    onQuantityChange: (String, String?, Double) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            // Report our top-left in root coords so the flight overlay can
            // translate `listState`'s viewport-local offsets to the screen.
            // onGloballyPositioned only fires when the position actually
            // changes, so this doesn't churn per frame.
            .onGloballyPositioned { onListPositioned(it.positionInRoot()) },
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
                    hidden = row.item.id == hiddenItemId,
                    justLanded = row.item.id == justLandedItemId,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onQuantityChange = onQuantityChange,
                )
            }
        }
    }
}

/**
 * Absolute LazyColumn index of the row whose item.id matches [itemId], or -1
 * when the item is absent from the current section list (e.g. filtered out by
 * search). Mirrors the loop structure in [VaultItemList]: one index per
 * non-empty section header, then one per item within it.
 */
private fun indexOfItemInSections(
    sections: List<VaultCategorySection>,
    itemId: String,
): Int {
    var index = 0
    for (section in sections) {
        if (section.items.isEmpty()) continue
        index++ // header
        for (row in section.items) {
            if (row.item.id == itemId) return index
            index++
        }
    }
    return -1
}

@Composable
private fun CategoryHeader(iconKey: String, colorHex: String?, name: String, count: Int) {
    val pastels = PocketSaveTokens.pastels
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.pocketsave.core.vault.icons.CategoryEmojiTile(
            iconKey = iconKey,
            colorHex = colorHex,
            size = 36.dp,
            cornerRadius = 10.dp,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Surface(
            color = pastels.canvasTint,
            shape = RoundedCornerShape(999.dp),
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun VaultItemRowView(
    row: VaultItemRow,
    hidden: Boolean,
    justLanded: Boolean,
    onEdit: (ItemEntity) -> Unit,
    onDelete: (ItemEntity) -> Unit,
    onQuantityChange: (String, String?, Double) -> Unit,
) {
    val primaryStore = row.primaryStore
    val currentQty = row.quantityForStore(primaryStore)

    // Arrival pulse: when the ghost hands off to this row, the target scale
    // briefly bumps to 1.15× and springs back — the "it landed here" beat.
    // MediumBouncy damping + MediumLow stiffness gives a clearly visible
    // rebound that registers even in peripheral vision, then settles without
    // a second oscillation. Held for ~500 ms (see LANDED_PULSE_HOLD_MS).
    val landingScale by animateFloatAsState(
        targetValue = if (justLanded) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "vaultRowLandingPulse",
    )

    // While the flight ghost is in the air, the real row reserves its layout
    // slot but paints nothing. The ghost lands on top of it; once the overlay
    // clears, this row fades back in at alpha = 1 and runs the pulse above.
    // iOS-parity: the receipt-ticket-shaped card with dashed tear edges top + bottom.
    // Source: Core/Vault/Views/VaultItemRow.swift — a torn receipt look with a
    // circular thumbnail, the item name, and a dotted leader between name and price.
    val ticketShape = androidx.compose.runtime.remember {
        com.pocketsave.common.ui.shapes.ReceiptTicketShape(
            notchRadius = 3.dp,
            flatWidth = 4.dp,
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .alpha(if (hidden) 0f else 1f)
            .scale(landingScale)
            .clip(ticketShape)
            .background(com.pocketsave.common.ui.PocketSaveColors.SurfaceSoft)
            .clickable(enabled = !hidden) { onEdit(row.item) },
    ) {
        com.pocketsave.common.ui.components.ReceiptGrunge(Modifier.fillMaxSize())
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ItemThumbnail(uri = row.item.imageUri, fallbackEmoji = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = row.item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = com.pocketsave.common.ui.FuzzyBubblesFamily,
                        fontWeight = FontWeight.Bold,
                        color = com.pocketsave.common.ui.PocketSaveColors.DarkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    // Dotted leader between name and price — receipt-style.
                    com.pocketsave.common.ui.components.DottedLeader(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                    )
                    val price = row.primaryPrice
                    if (price != null) {
                        Text(
                            text = "%.2f".format(price),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = com.pocketsave.common.ui.PocketSaveColors.DarkPrimary,
                        )
                    }
                }
                val unit = row.primaryUnit
                val subtitle = buildString {
                    if (primaryStore != null) append(primaryStore)
                    if (!unit.isNullOrEmpty()) {
                        if (isNotEmpty()) append(" · ")
                        append("per $unit")
                    }
                }
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = com.pocketsave.common.ui.PocketSaveColors.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
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
                    tint = com.pocketsave.common.ui.PocketSaveColors.AccentDanger,
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
    val pastels = PocketSaveTokens.pastels
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (search.isNotBlank()) {
            AffectionateEmpty(
                title = "Nothing under \"$search\".",
                body = "Try a different word, or tap Add item to start a new one.",
                icon = Icons.Default.Search,
                accent = pastels.lavenderDeep,
            )
        } else {
            AffectionateEmpty(
                title = "A little empty in here.",
                body = "Your vault starts bare. Tap Add item to save the things you buy often — prices, units, and all.",
                icon = Icons.Default.Category,
                accent = pastels.mintDeep,
            )
        }
    }
}
