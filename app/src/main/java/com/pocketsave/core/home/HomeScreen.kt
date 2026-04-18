package com.pocketsave.core.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.material3.AlertDialog
import com.pocketsave.common.util.ColorOption
import com.pocketsave.core.cart.CreateCartSheet
import com.pocketsave.core.cart.VaultSelectionStore
import com.pocketsave.core.currency.CurrencyPickerSheet
import com.pocketsave.core.service.VaultService
import com.pocketsave.data.local.entity.CartEntity
import com.pocketsave.data.prefs.AppPreferences
import com.pocketsave.data.prefs.CartBackgroundStore
import com.pocketsave.domain.model.CartStatus
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Phase 5+8 Home. Surfaces the Vault entry point plus a list of carts. Each
 * cart card paints its customised background — iOS does the same via
 * `HomeCartRowView` + `HomeCartRowContentCardView` consulting `CartBackgroundImageManager`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vaultService: VaultService,
    selectionStore: VaultSelectionStore,
    backgroundStore: CartBackgroundStore,
    preferences: AppPreferences,
    onOpenVault: () -> Unit,
    onOpenCart: (cartId: String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenTrash: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onOpenCurrencyPicker: (() -> Unit)? = null,
) {
    val state by vaultService.state.collectAsState()
    val selectedItems by selectionStore.activeCartItems.collectAsState()
    val userName = state.user?.name ?: "Default User"
    val active = state.carts.filter { CartStatus.fromRaw(it.status) != CartStatus.COMPLETED }
    val completed = state.carts.filter { CartStatus.fromRaw(it.status) == CartStatus.COMPLETED }

    var showOverflow by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showCreateCartSheet by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CartEntity?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PocketSave") },
                actions = {
                    IconButton(onClick = { showOverflow = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                            text = { Text("History") },
                            onClick = { showOverflow = false; onOpenHistory() },
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                            text = { Text("Currency") },
                            onClick = { showOverflow = false; showCurrencyPicker = true },
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            text = { Text("Trash") },
                            onClick = { showOverflow = false; onOpenTrash() },
                        )
                    }
                },
            )
        },
    ) { inner ->
    LazyColumn(
        modifier = Modifier.padding(inner).fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(text = "PocketSave", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text(text = "Signed in as $userName", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${state.items.size} items across ${state.categories.size} categories",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onOpenVault, modifier = Modifier.weight(1f)) { Text("Open vault") }
                Button(onClick = { showCreateCartSheet = true }, modifier = Modifier.weight(1f)) { Text("New cart") }
            }
        }
        item {
            Text("Active carts", style = MaterialTheme.typography.titleMedium)
        }
        if (active.isEmpty()) {
            item { Text("No active carts yet. Tap New cart to start empty, or open vault to pick items first.") }
        } else {
            items(active, key = { it.id }) { cart ->
                CartSummaryCard(
                    cart = cart,
                    totalSpent = vaultService.computeTotalSpent(
                        CartStatus.fromRaw(cart.status),
                        state.cartItemsByCart[cart.id].orEmpty(),
                    ),
                    itemCount = state.cartItemsByCart[cart.id]?.size ?: 0,
                    backgroundStore = backgroundStore,
                    onClick = { onOpenCart(cart.id) },
                    onRequestDelete = { pendingDelete = cart },
                )
            }
        }
        if (completed.isNotEmpty()) {
            item {
                Text(
                    "Completed",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(completed, key = { it.id }) { cart ->
                CartSummaryCard(
                    cart = cart,
                    totalSpent = vaultService.computeTotalSpent(
                        CartStatus.fromRaw(cart.status),
                        state.cartItemsByCart[cart.id].orEmpty(),
                    ),
                    itemCount = state.cartItemsByCart[cart.id]?.size ?: 0,
                    backgroundStore = backgroundStore,
                    onClick = { onOpenCart(cart.id) },
                    onRequestDelete = { pendingDelete = cart },
                )
            }
        }
    }
    }

    if (showCurrencyPicker) {
        CurrencyPickerSheet(
            preferences = preferences,
            onDismiss = { showCurrencyPicker = false },
        )
    }

    if (showCreateCartSheet) {
        CreateCartSheet(
            vaultService = vaultService,
            selectionStore = selectionStore,
            selectedItemCount = selectedItems.size,
            onDismiss = { showCreateCartSheet = false },
            onCreated = { cartId ->
                showCreateCartSheet = false
                onOpenCart(cartId)
            },
        )
    }

    pendingDelete?.let { target ->
        val isCompleted = CartStatus.fromRaw(target.status) == CartStatus.COMPLETED
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    scope.launch {
                        vaultService.deleteCart(target.id)
                        if (!isCompleted) {
                            // Planning / shopping carts are hard-deleted, so the
                            // per-cart background prefs can go too.
                            backgroundStore.clear(target.id)
                        }
                    }
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
            title = { Text("Delete ${target.name}?") },
            text = {
                Text(
                    if (isCompleted)
                        "Completed trips move to Trash and can be restored later."
                    else
                        "This cart and its planned items will be removed permanently.",
                )
            },
        )
    }
}

@Composable
private fun CartSummaryCard(
    cart: CartEntity,
    totalSpent: Double,
    itemCount: Int,
    backgroundStore: CartBackgroundStore,
    onClick: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    val formatter = com.pocketsave.core.currency.LocalCurrencyFormatter.current
    val bgHex by backgroundStore.colorHex(cart.id).collectAsState(initial = null)
    val bgImage by backgroundStore.imageUri(cart.id).collectAsState(initial = null)
    val backgroundColor = ColorOption.byHex(bgHex ?: "")?.color ?: MaterialTheme.colorScheme.surface
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor),
        ) {
            if (bgImage != null) {
                AsyncImage(
                    model = bgImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
                Box(modifier = Modifier.matchParentSize().background(Color(0x33000000)))
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = cart.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = CartStatus.fromRaw(cart.status).displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "More actions",
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                },
                                text = { Text("Delete") },
                                onClick = {
                                    menuExpanded = false
                                    onRequestDelete()
                                },
                            )
                        }
                    }
                }
                Text(
                    text = "$itemCount item${if (itemCount == 1) "" else "s"} • ${formatter.format(totalSpent)}" +
                        if (cart.budget > 0.0) " / ${formatter.format(cart.budget)}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
