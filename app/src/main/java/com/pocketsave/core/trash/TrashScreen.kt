package com.pocketsave.core.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketsave.core.service.VaultService
import com.pocketsave.data.local.entity.CartEntity
import com.pocketsave.data.local.entity.ItemEntity
import com.pocketsave.data.prefs.CartBackgroundStore
import com.pocketsave.domain.model.CartStatus
import kotlinx.coroutines.launch
import java.text.DateFormat

/**
 * Port of iOS trash browsing (the flows backed by
 * `VaultService+Domain.swift::restoreDeletedItem` + `permanentlyDeleteItemFromTrash`
 * and `VaultService+Carts.swift::restoreDeletedCart` + `permanentlyDeleteCartFromTrash`).
 *
 * Two segmented tabs — Items and Carts — with restore / delete-forever
 * confirmations. Reached from the Home overflow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    vaultService: VaultService,
    cartBackgroundStore: CartBackgroundStore,
    onBack: () -> Unit,
) {
    val state by vaultService.state.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(Tab.ITEMS) }
    var pendingItemDelete by remember { mutableStateOf<ItemEntity?>(null) }
    var pendingCartDelete by remember { mutableStateOf<CartEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { inner ->
        Column(modifier = Modifier.padding(inner).fillMaxSize()) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Tab.entries.forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = Tab.entries.size),
                        label = {
                            val count = when (tab) {
                                Tab.ITEMS -> state.deletedItems.size
                                Tab.CARTS -> state.deletedCarts.size
                            }
                            Text(if (count > 0) "${tab.displayName} · $count" else tab.displayName)
                        },
                    )
                }
            }

            when (selectedTab) {
                Tab.ITEMS -> DeletedItemsList(
                    items = state.deletedItems,
                    onRestore = { item ->
                        scope.launch { vaultService.restoreDeletedItem(item.id) }
                    },
                    onRequestPermanentDelete = { pendingItemDelete = it },
                )
                Tab.CARTS -> DeletedCartsList(
                    carts = state.deletedCarts,
                    onRestore = { cart ->
                        scope.launch { vaultService.restoreDeletedCart(cart.id) }
                    },
                    onRequestPermanentDelete = { pendingCartDelete = it },
                )
            }
        }
    }

    pendingItemDelete?.let { target ->
        ConfirmPermanentDelete(
            title = "Delete ${target.name}?",
            body = "This item will be removed permanently. Any completed carts that referenced it keep a copy of the name and price.",
            onDismiss = { pendingItemDelete = null },
            onConfirm = {
                pendingItemDelete = null
                scope.launch { vaultService.permanentlyDeleteItem(target.id) }
            },
        )
    }

    pendingCartDelete?.let { target ->
        ConfirmPermanentDelete(
            title = "Delete ${target.name}?",
            body = "Deletes the trip and its line items permanently. Vault items stay untouched.",
            onDismiss = { pendingCartDelete = null },
            onConfirm = {
                pendingCartDelete = null
                scope.launch {
                    vaultService.permanentlyDeleteCart(target.id)
                    // Clean up per-cart background prefs — mirrors iOS
                    // `CartBackgroundImageManager.deleteImage(forCartId:)` on the
                    // permanent-delete path.
                    cartBackgroundStore.clear(target.id)
                }
            },
        )
    }
}

private enum class Tab(val displayName: String) { ITEMS("Items"), CARTS("Carts") }

@Composable
private fun DeletedItemsList(
    items: List<ItemEntity>,
    onRestore: (ItemEntity) -> Unit,
    onRequestPermanentDelete: (ItemEntity) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyState("No deleted items.")
        return
    }
    val formatter = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
        items(items, key = { it.id }) { item ->
            TrashCard(
                title = item.name,
                subtitle = buildSubtitle(
                    item.deletedFromCategoryName,
                    item.deletedAt?.let { formatter.format(it) },
                ),
                onRestore = { onRestore(item) },
                onDelete = { onRequestPermanentDelete(item) },
            )
        }
    }
}

@Composable
private fun DeletedCartsList(
    carts: List<CartEntity>,
    onRestore: (CartEntity) -> Unit,
    onRequestPermanentDelete: (CartEntity) -> Unit,
) {
    if (carts.isEmpty()) {
        EmptyState("No deleted carts.")
        return
    }
    val formatter = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
        items(carts, key = { it.id }) { cart ->
            TrashCard(
                title = cart.name,
                subtitle = buildSubtitle(
                    CartStatus.fromRaw(cart.status).displayName,
                    cart.deletedAt?.let { formatter.format(it) },
                ),
                onRestore = { onRestore(cart) },
                onDelete = { onRequestPermanentDelete(cart) },
            )
        }
    }
}

@Composable
private fun TrashCard(
    title: String,
    subtitle: String,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = onRestore) {
                Icon(Icons.Default.RestoreFromTrash, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Restore")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Delete permanently",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ConfirmPermanentDelete(
    title: String,
    body: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(title) },
        text = { Text(body) },
    )
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun buildSubtitle(category: String?, deletedAt: String?): String {
    val parts = listOfNotNull(
        category?.takeIf { it.isNotBlank() },
        deletedAt?.let { "Deleted $it" },
    )
    return parts.joinToString(" · ")
}

