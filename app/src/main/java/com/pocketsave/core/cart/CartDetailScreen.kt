package com.pocketsave.core.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocketsave.billing.PremiumFeature
import com.pocketsave.billing.SubscriptionManager
import com.pocketsave.billing.rememberPaywallGate
import com.pocketsave.common.util.ColorOption
import com.pocketsave.core.haptics.AppHaptic
import com.pocketsave.core.haptics.rememberAppHaptics
import com.pocketsave.core.paywall.ProChip
import com.pocketsave.core.service.VaultService
import com.pocketsave.data.prefs.CartBackgroundStore
import com.pocketsave.domain.model.CartStatus
import com.pocketsave.domain.semantics.UnitMenuCatalog
import com.pocketsave.domain.semantics.UnitSemantics
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color

/**
 * Port of `PocketSave/Core/Detail Cart/CartDetailScreen.swift` — Planning,
 * Shopping, and Completed modes. Sub-composables mirror the iOS breakdown
 * (`HeaderView`, `ItemsListView`, `StoreSectionView`, `CartItemRowListView`,
 * `Shopping Cart` subfolder, `FinishTripSheet`).
 *
 * Paywall-specific affordances (locked CTA, free active-cart limit popovers)
 * are omitted per the no-paywall rule.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartDetailScreen(
    vaultService: VaultService,
    backgroundStore: CartBackgroundStore,
    subscriptionManager: SubscriptionManager,
    cartId: String,
    onBack: () -> Unit,
    onOpenPaywall: (PremiumFeature) -> Unit,
    onCompleteTripDone: () -> Unit = {},
    onShareTrip: (String) -> Unit = {},
    onOpenCart: (String) -> Unit = {},
    pendingDeepLink: com.pocketsave.app.PendingDeepLink? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val paywallGate = rememberPaywallGate(subscriptionManager, vaultService, onOpenPaywall)
    val viewModel: CartDetailViewModel = viewModel(
        factory = CartDetailViewModel.Factory(vaultService, cartId),
    )
    val ui by viewModel.uiState.collectAsState()
    val snapshot by vaultService.state.collectAsState()
    val bgColorHex by backgroundStore.colorHex(cartId).collectAsState(initial = null)
    val bgImageUri by backgroundStore.imageUri(cartId).collectAsState(initial = null)
    val haptics = rememberAppHaptics()

    var showOverflow by remember { mutableStateOf(false) }
    var showRenameSheet by remember { mutableStateOf(false) }
    var showBudgetSheet by remember { mutableStateOf(false) }
    var showManageSheet by remember { mutableStateOf(false) }
    var showShoppingOnlySheet by remember { mutableStateOf(false) }
    var showFinishSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showReturnToPlanningConfirm by remember { mutableStateOf(false) }
    var showBackgroundPicker by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // React to deep links (`pocketsave://…`). `FinishTrip` auto-opens the
    // finish-trip sheet once the cart is in shopping; `QuickAdd` opens the
    // manage sheet so the user can pick quantity for the item the widget
    // tapped into. After the one-shot action is staged we tell the host to
    // clear the pending value so the same link doesn't re-fire on
    // recomposition.
    LaunchedEffect(pendingDeepLink, ui.status) {
        val pending = pendingDeepLink ?: return@LaunchedEffect
        if (pending.cartId != cartId) return@LaunchedEffect
        when (pending) {
            is com.pocketsave.app.PendingDeepLink.FinishTrip -> {
                if (ui.status == CartStatus.SHOPPING) {
                    showFinishSheet = true
                    onDeepLinkConsumed()
                }
            }
            is com.pocketsave.app.PendingDeepLink.QuickAdd -> {
                if (ui.status == CartStatus.SHOPPING) {
                    showShoppingOnlySheet = true
                } else if (ui.status == CartStatus.PLANNING) {
                    showManageSheet = true
                }
                onDeepLinkConsumed()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = ui.name.ifEmpty { "Cart" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    StatusPill(status = ui.status)
                    IconButton(onClick = { showOverflow = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
                        DropdownMenuItem(
                            text = { Text("Rename cart") },
                            onClick = { showOverflow = false; showRenameSheet = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Edit budget") },
                            onClick = { showOverflow = false; showBudgetSheet = true },
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Customize background")
                                    if (!paywallGate.isAllowed(PremiumFeature.CartTheme)) {
                                        Spacer(Modifier.width(8.dp))
                                        ProChip()
                                    }
                                }
                            },
                            onClick = {
                                showOverflow = false
                                paywallGate.check(PremiumFeature.CartTheme) {
                                    showBackgroundPicker = true
                                }
                            },
                        )
                        if (ui.status == CartStatus.COMPLETED) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Share trip")
                                        if (!paywallGate.isAllowed(PremiumFeature.TripShareCard)) {
                                            Spacer(Modifier.width(8.dp))
                                            ProChip()
                                        }
                                    }
                                },
                                onClick = {
                                    showOverflow = false
                                    paywallGate.check(PremiumFeature.TripShareCard) {
                                        onShareTrip(cartId)
                                    }
                                },
                            )
                        }
                        if (ui.status == CartStatus.SHOPPING) {
                            DropdownMenuItem(
                                text = { Text("Return to planning") },
                                onClick = { showOverflow = false; showReturnToPlanningConfirm = true },
                            )
                        }
                        if (ui.status == CartStatus.COMPLETED) {
                            DropdownMenuItem(
                                text = { Text("Reopen cart") },
                                onClick = {
                                    showOverflow = false
                                    viewModel.reopenCart(onOpenCart)
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete cart") },
                            onClick = { showOverflow = false; showDeleteConfirm = true },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            when (ui.status) {
                CartStatus.PLANNING -> ExtendedFloatingActionButton(
                    onClick = { showManageSheet = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add items") },
                )
                CartStatus.SHOPPING -> ExtendedFloatingActionButton(
                    onClick = { showShoppingOnlySheet = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Quick add") },
                )
                CartStatus.COMPLETED -> Unit // no FAB
            }
        },
        bottomBar = {
            BottomActionBar(
                status = ui.status,
                itemCount = ui.itemCount,
                fulfilledCount = ui.fulfilledCount,
                // Confirm for start-shopping and reopen-cart — both are
                // meaningful lifecycle flips the user will remember doing.
                onStartShopping = {
                    haptics.perform(AppHaptic.Confirm)
                    viewModel.startShopping()
                },
                onFinishTrip = { showFinishSheet = true },
                onReopenCart = {
                    haptics.perform(AppHaptic.Confirm)
                    viewModel.reopenCart(onOpenCart)
                },
            )
        },
    ) { inner ->
        Column(modifier = Modifier.padding(inner).fillMaxSize()) {
            CartHeaderWithBackground(
                ui = ui,
                backgroundColorHex = bgColorHex,
                backgroundImageUri = bgImageUri,
            )
            when {
                !ui.isReady -> LoadingState()
                ui.itemCount == 0 -> EmptyCartState(status = ui.status)
                else -> CartSections(
                    ui = ui,
                    snapshot = snapshot,
                    onPlanningQuantityChange = viewModel::updatePlannedQuantity,
                    onPlanningRemove = viewModel::removeItem,
                    onPlanningPriceChange = viewModel::updatePlannedPrice,
                    onPlanningUnitChange = viewModel::updatePlannedUnit,
                    onPlanningStoreChange = viewModel::changeStore,
                    // Light ticks for shopping-mode state flips. Fulfilled
                    // and skip are the two per-item gestures shoppers do
                    // dozens of times — Light keeps the feedback quiet but
                    // present so the action still feels acknowledged.
                    onToggleFulfilled = { itemId ->
                        haptics.perform(AppHaptic.Light)
                        viewModel.toggleFulfilled(itemId)
                    },
                    onSkip = { itemId ->
                        haptics.perform(AppHaptic.Light)
                        viewModel.skipDuringShopping(itemId)
                    },
                    onUnskip = viewModel::unskip,
                    onUpdateActual = viewModel::updateActual,
                    onActualStoreChange = viewModel::changeActualStore,
                )
            }
        }
    }

    if (showRenameSheet) {
        TextEditSheet(
            title = "Rename cart",
            initial = ui.name,
            placeholder = "Cart name",
            validate = { it.trim().isNotEmpty() },
            onDismiss = { showRenameSheet = false },
            onSubmit = { viewModel.rename(it); showRenameSheet = false },
        )
    }

    if (showBudgetSheet) {
        BudgetEditSheet(
            initial = ui.budget,
            onDismiss = { showBudgetSheet = false },
            onSubmit = { viewModel.updateBudget(it); showBudgetSheet = false },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteCart(onDeleted = {
                        // Clean up the per-cart background (iOS `deleteCart` also
                        // wipes UserDefaults color key + CartBackgroundImageManager file).
                        coroutineScope.launch { backgroundStore.clear(cartId) }
                        onBack()
                    })
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
            title = { Text("Delete cart?") },
            text = {
                Text(
                    if (ui.status == CartStatus.COMPLETED) "${ui.name} will move to trash."
                    else "${ui.name} will be removed permanently.",
                )
            },
        )
    }

    if (showReturnToPlanningConfirm) {
        AlertDialog(
            onDismissRequest = { showReturnToPlanningConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    showReturnToPlanningConfirm = false
                    viewModel.returnToPlanning()
                }) { Text("Return") }
            },
            dismissButton = {
                TextButton(onClick = { showReturnToPlanningConfirm = false }) { Text("Cancel") }
            },
            title = { Text("Return to planning?") },
            text = {
                Text(
                    "Items added during shopping will be removed and actual prices will be cleared. " +
                        "Planned quantities are restored.",
                )
            },
        )
    }

    if (showManageSheet && ui.status == CartStatus.PLANNING) {
        ManageCartSheet(
            vaultService = vaultService,
            cart = ui.cart,
            onAddItem = { item, qty, store -> viewModel.addVaultItemToCart(item, qty, store) },
            onDismiss = { showManageSheet = false },
        )
    }

    if (showShoppingOnlySheet && ui.status == CartStatus.SHOPPING) {
        AddShoppingOnlyItemSheet(
            vaultService = vaultService,
            onDismiss = { showShoppingOnlySheet = false },
            onAddVaultItem = { item, store, price, unit, qty ->
                viewModel.addVaultItemDuringShopping(item, store, price, unit, qty)
                showShoppingOnlySheet = false
            },
            onAddShoppingOnly = { name, store, price, unit, qty, category, imageUri ->
                viewModel.addShoppingOnlyItem(
                    name = name,
                    store = store,
                    price = price,
                    unit = unit,
                    quantity = qty,
                    categoryName = category,
                    imageUri = imageUri,
                )
                showShoppingOnlySheet = false
            },
        )
    }

    if (showFinishSheet) {
        // Intentionally NOT guarded on `ui.status == SHOPPING`: the moment
        // `completeShoppingNow()` flips the cart to COMPLETED, the snapshot
        // re-emits and `ui.status` changes. If this guard removed the sheet
        // mid-completion, its `rememberCoroutineScope` would be cancelled
        // and the post-delay `onConfirm()` (which fires the navigation)
        // would never run — the user would be stuck on the detail screen.
        // The sheet only appears in response to the SHOPPING-only "Finish
        // trip" button, so a status guard here is redundant anyway.
        FinishTripSheet(
            viewModel = viewModel,
            onDismiss = { showFinishSheet = false },
            onConfirm = {
                // One Confirm haptic on the milestone, then drop straight
                // to Home via the wrap-up-return transition. The sheet's
                // own CelebrationBurst + that transition carry the moment.
                haptics.perform(AppHaptic.Confirm)
                showFinishSheet = false
                onCompleteTripDone()
            },
        )
    }

    if (showBackgroundPicker) {
        CartBackgroundPicker(
            cartId = cartId,
            backgroundStore = backgroundStore,
            onDismiss = { showBackgroundPicker = false },
        )
    }
}

@Composable
private fun StatusPill(status: CartStatus) {
    val label = status.displayName
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun CartHeaderWithBackground(
    ui: CartDetailUiState,
    backgroundColorHex: String?,
    backgroundImageUri: String?,
) {
    val backgroundColor = ColorOption.byHex(backgroundColorHex ?: "")
        ?: ColorOption.defaultColor
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor.color),
    ) {
        // Image + dim use matchParentSize so they track the Box's height
        // without influencing it; CartHeader's intrinsic height is what sizes
        // the Box. Using fillMaxSize here instead caused the header to swallow
        // the whole content column and hide the items list, FAB, and Finish
        // Trip button.
        if (backgroundImageUri != null) {
            AsyncImage(
                model = backgroundImageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Box(modifier = Modifier.matchParentSize().background(Color(0x33000000)))
        }
        CartHeader(ui = ui)
    }
}

@Composable
private fun CartHeader(ui: CartDetailUiState) {
    val formatter = com.pocketsave.core.currency.LocalCurrencyFormatter.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Budget",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatter.format(ui.budget),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val label = when (ui.status) {
                CartStatus.PLANNING -> "Planned total"
                CartStatus.SHOPPING -> "Spent so far"
                CartStatus.COMPLETED -> "Actual total"
            }
            val value = when (ui.status) {
                CartStatus.SHOPPING -> ui.fulfilledAmount
                else -> ui.totalSpent
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatter.format(value),
                style = MaterialTheme.typography.titleMedium,
                color = if (ui.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
        LinearProgressIndicator(
            progress = ui.progress.toFloat(),
            modifier = Modifier.fillMaxWidth().height(6.dp),
        )
        val subtitle = when (ui.status) {
            CartStatus.PLANNING -> when {
                ui.budget <= 0.0 -> "No budget set"
                ui.isOverBudget -> "Over by ${formatter.format(-ui.remaining)}"
                else -> "${formatter.format(ui.remaining)} remaining"
            }
            CartStatus.SHOPPING -> "${ui.fulfilledCount} of ${ui.itemCount} checked off" +
                if (ui.skippedCount > 0) " • ${ui.skippedCount} skipped" else ""
            CartStatus.COMPLETED -> {
                val diff = ui.totalSpent - ui.budget
                when {
                    ui.budget <= 0.0 -> "Trip completed"
                    diff > 0.0 -> "${formatter.format(diff)} over budget"
                    else -> "${formatter.format(-diff)} under budget"
                }
            }
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CartSections(
    ui: CartDetailUiState,
    snapshot: VaultService.Snapshot,
    onPlanningQuantityChange: (String, Double) -> Unit,
    onPlanningRemove: (String) -> Unit,
    onPlanningPriceChange: (String, Double?) -> Unit,
    onPlanningUnitChange: (String, String) -> Unit,
    onPlanningStoreChange: (String, String) -> Unit,
    onToggleFulfilled: (String) -> Unit,
    onSkip: (String) -> Unit,
    onUnskip: (String) -> Unit,
    onUpdateActual: (String, Double?, Double?, String?, String?) -> Unit,
    onActualStoreChange: (String, String) -> Unit,
) {
    val knownStores = snapshot.stores.map { it.name }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp),
    ) {
        ui.sections.forEach { section ->
            item(key = "header-${section.store}") {
                StoreSectionHeader(store = section.store, total = section.storeTotal)
            }
            items(items = section.rows, key = { it.cartItem.uid }) { row ->
                when (ui.status) {
                    CartStatus.PLANNING -> PlanningRow(
                        row = row,
                        knownStores = knownStores,
                        onQuantityChange = onPlanningQuantityChange,
                        onRemove = onPlanningRemove,
                        onPriceChange = onPlanningPriceChange,
                        onUnitChange = onPlanningUnitChange,
                        onStoreChange = onPlanningStoreChange,
                    )
                    CartStatus.SHOPPING -> ShoppingRow(
                        row = row,
                        knownStores = knownStores,
                        onToggleFulfilled = onToggleFulfilled,
                        onSkip = onSkip,
                        onUnskip = onUnskip,
                        onUpdateActual = onUpdateActual,
                        onActualStoreChange = onActualStoreChange,
                    )
                    CartStatus.COMPLETED -> CompletedRow(row = row)
                }
            }
        }
    }
}

@Composable
private fun StoreSectionHeader(store: String, total: Double) {
    val formatter = com.pocketsave.core.currency.LocalCurrencyFormatter.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Store, contentDescription = null)
        Spacer(modifier = Modifier.padding(4.dp))
        Text(
            text = store.ifBlank { "No store" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatter.format(total),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanningRow(
    row: CartDetailItemRow,
    knownStores: List<String>,
    onQuantityChange: (String, Double) -> Unit,
    onRemove: (String) -> Unit,
    onPriceChange: (String, Double?) -> Unit,
    onUnitChange: (String, String) -> Unit,
    onStoreChange: (String, String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val formatter = com.pocketsave.core.currency.LocalCurrencyFormatter.current

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val subtitle = buildString {
                        append(formatter.format(row.price))
                        if (row.unit.isNotEmpty()) append(" / ${row.unit}")
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                QuantityStepper(
                    quantity = row.cartItem.quantity,
                    onDecrement = {
                        val next = (row.cartItem.quantity - 1.0).coerceAtLeast(0.0)
                        if (next == 0.0) onRemove(row.cartItem.itemId)
                        else onQuantityChange(row.cartItem.itemId, next)
                    },
                    onIncrement = { onQuantityChange(row.cartItem.itemId, row.cartItem.quantity + 1.0) },
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit ${row.displayName}")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Subtotal ${formatter.format(row.lineTotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onRemove(row.cartItem.itemId) }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.padding(2.dp))
                    Text("Remove")
                }
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                PlannedValuesEditor(
                    row = row,
                    knownStores = knownStores,
                    onPriceChange = { p -> onPriceChange(row.cartItem.itemId, p) },
                    onUnitChange = { u -> onUnitChange(row.cartItem.itemId, u) },
                    onStoreChange = { s -> onStoreChange(row.cartItem.itemId, s) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingRow(
    row: CartDetailItemRow,
    knownStores: List<String>,
    onToggleFulfilled: (String) -> Unit,
    onSkip: (String) -> Unit,
    onUnskip: (String) -> Unit,
    onUpdateActual: (String, Double?, Double?, String?, String?) -> Unit,
    onActualStoreChange: (String, String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val formatter = com.pocketsave.core.currency.LocalCurrencyFormatter.current
    val item = row.cartItem
    val isFulfilled = item.isFulfilled && !item.isSkippedDuringShopping
    val isSkipped = item.isSkippedDuringShopping

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSkipped) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onToggleFulfilled(item.itemId) },
                    enabled = !isSkipped,
                ) {
                    Icon(
                        imageVector = if (isFulfilled) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (isFulfilled) "Mark unfulfilled" else "Mark fulfilled",
                        tint = if (isFulfilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (isFulfilled || isSkipped) TextDecoration.LineThrough else null,
                    )
                    val subtitle = buildString {
                        append(formatter.format(row.price))
                        if (row.unit.isNotEmpty()) append(" / ${row.unit}")
                        append(" × ")
                        append(
                            if (row.displayQuantity % 1.0 == 0.0) row.displayQuantity.toInt().toString()
                            else "%.2f".format(row.displayQuantity),
                        )
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = formatter.format(row.lineTotal),
                    style = MaterialTheme.typography.titleSmall,
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit actuals")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSkipped) {
                    TextButton(onClick = { onUnskip(item.itemId) }) {
                        Icon(Icons.Default.Restore, contentDescription = null)
                        Spacer(modifier = Modifier.padding(2.dp))
                        Text("Un-skip")
                    }
                } else {
                    TextButton(onClick = { onSkip(item.itemId) }) {
                        Icon(Icons.Default.SkipNext, contentDescription = null)
                        Spacer(modifier = Modifier.padding(2.dp))
                        Text("Skip")
                    }
                }
                if (item.wasEditedDuringShopping && !isFulfilled) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Edited",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                ActualValuesEditor(
                    row = row,
                    knownStores = knownStores,
                    onPriceChange = { p -> onUpdateActual(item.itemId, p, null, null, null) },
                    onQuantityChange = { q -> onUpdateActual(item.itemId, null, q, null, null) },
                    onUnitChange = { u -> onUpdateActual(item.itemId, null, null, u, null) },
                    onStoreChange = { s -> onActualStoreChange(item.itemId, s) },
                )
            }
        }
    }
}

@Composable
private fun CompletedRow(row: CartDetailItemRow) {
    val formatter = com.pocketsave.core.currency.LocalCurrencyFormatter.current
    val item = row.cartItem
    val isSkipped = item.isSkippedDuringShopping

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (!isSkipped) Icons.Default.CheckCircle else Icons.Default.SkipNext,
                contentDescription = null,
                tint = if (!isSkipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.padding(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isSkipped) TextDecoration.LineThrough else null,
                )
                val subtitle = if (isSkipped) "Skipped" else buildString {
                    append(formatter.format(row.price))
                    if (row.unit.isNotEmpty()) append(" / ${row.unit}")
                    append(" × ")
                    append(
                        if (row.displayQuantity % 1.0 == 0.0) row.displayQuantity.toInt().toString()
                        else "%.2f".format(row.displayQuantity),
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = formatter.format(row.lineTotal),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
private fun QuantityStepper(quantity: Double, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onDecrement, enabled = quantity > 0) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease quantity")
        }
        Text(
            text = if (quantity % 1.0 == 0.0) "${quantity.toInt()}" else "%.2f".format(quantity),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        IconButton(onClick = onIncrement) {
            Icon(Icons.Default.Add, contentDescription = "Increase quantity")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlannedValuesEditor(
    row: CartDetailItemRow,
    knownStores: List<String>,
    onPriceChange: (Double?) -> Unit,
    onUnitChange: (String) -> Unit,
    onStoreChange: (String) -> Unit,
) {
    var priceText by remember(row.cartItem.uid, row.price) {
        mutableStateOf(if (row.price > 0.0) "%.2f".format(row.price) else "")
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = priceText,
            onValueChange = { raw ->
                if (raw.isEmpty() || raw.matches(Regex("^\\d*[.,]?\\d{0,2}$"))) {
                    priceText = raw.replace(',', '.')
                    onPriceChange(priceText.toDoubleOrNull())
                }
            },
            label = { Text("Planned price") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        UnitDropdown(selected = row.unit, label = "Planned unit", onSelect = onUnitChange)
        StoreDropdown(selected = row.displayStore, options = knownStores, label = "Planned store", onSelect = onStoreChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActualValuesEditor(
    row: CartDetailItemRow,
    knownStores: List<String>,
    onPriceChange: (Double?) -> Unit,
    onQuantityChange: (Double?) -> Unit,
    onUnitChange: (String) -> Unit,
    onStoreChange: (String) -> Unit,
) {
    var priceText by remember(row.cartItem.uid, row.price) {
        mutableStateOf(if (row.price > 0.0) "%.2f".format(row.price) else "")
    }
    var qtyText by remember(row.cartItem.uid, row.displayQuantity) {
        mutableStateOf(
            if (row.displayQuantity % 1.0 == 0.0) row.displayQuantity.toInt().toString()
            else "%.2f".format(row.displayQuantity),
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = priceText,
            onValueChange = { raw ->
                if (raw.isEmpty() || raw.matches(Regex("^\\d*[.,]?\\d{0,2}$"))) {
                    priceText = raw.replace(',', '.')
                    onPriceChange(priceText.toDoubleOrNull())
                }
            },
            label = { Text("Actual price") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = qtyText,
            onValueChange = { raw ->
                if (raw.isEmpty() || raw.matches(Regex("^\\d*[.,]?\\d*$"))) {
                    qtyText = raw.replace(',', '.')
                    onQuantityChange(qtyText.toDoubleOrNull())
                }
            },
            label = { Text("Actual quantity") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        UnitDropdown(selected = row.unit, label = "Actual unit", onSelect = onUnitChange)
        StoreDropdown(selected = row.displayStore, options = knownStores, label = "Actual store", onSelect = onStoreChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(selected: String, label: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = UnitMenuCatalog.continuousOptions + UnitMenuCatalog.discreteOptions
    val displayLabel = options.firstOrNull { it.abbr == selected }?.displayText ?: selected

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = displayLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayText) },
                    onClick = {
                        onSelect(UnitSemantics.canonicalUnit(option.abbr))
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreDropdown(selected: String, options: List<String>, label: String, onSelect: (String) -> Unit) {
    if (options.isEmpty()) {
        OutlinedTextField(
            value = selected,
            onValueChange = { onSelect(it) },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun BottomActionBar(
    status: CartStatus,
    itemCount: Int,
    fulfilledCount: Int,
    onStartShopping: () -> Unit,
    onFinishTrip: () -> Unit,
    onReopenCart: () -> Unit,
) {
    when (status) {
        CartStatus.PLANNING -> {
            if (itemCount == 0) return
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Button(onClick = onStartShopping, modifier = Modifier.fillMaxWidth()) {
                    Text("Start shopping")
                }
            }
        }
        CartStatus.SHOPPING -> {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Button(
                    onClick = onFinishTrip,
                    enabled = fulfilledCount > 0,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Finish trip") }
            }
        }
        CartStatus.COMPLETED -> {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                OutlinedButton(onClick = onReopenCart, modifier = Modifier.fillMaxWidth()) {
                    Text("Reopen cart")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextEditSheet(
    title: String,
    initial: String,
    placeholder: String,
    validate: (String) -> Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var value by remember { mutableStateOf(initial) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = { onSubmit(value.trim()) },
                    enabled = validate(value),
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetEditSheet(
    initial: Double,
    onDismiss: () -> Unit,
    onSubmit: (Double) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf(if (initial > 0.0) "%.2f".format(initial) else "") }
    val parsed = text.toDoubleOrNull()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Edit budget", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = text,
                onValueChange = { raw ->
                    if (raw.isEmpty() || raw.matches(Regex("^\\d*[.,]?\\d{0,2}$"))) {
                        text = raw.replace(',', '.')
                    }
                },
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = { parsed?.let(onSubmit) },
                    enabled = parsed != null && parsed >= 0.0,
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
        }
    }
}

@Composable
private fun EmptyCartState(status: CartStatus) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = when (status) {
                CartStatus.PLANNING -> "This cart is empty. Tap Add items to start planning."
                CartStatus.SHOPPING -> "No items in cart. Tap Quick add to record a purchase."
                CartStatus.COMPLETED -> "Completed with no items."
            },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Loading cart...")
    }
}
