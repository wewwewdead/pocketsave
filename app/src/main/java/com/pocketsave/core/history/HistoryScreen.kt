package com.pocketsave.core.history

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pocketsave.common.util.ColorOption
import com.pocketsave.core.currency.LocalCurrencyFormatter
import com.pocketsave.core.service.VaultService
import com.pocketsave.data.local.entity.CartEntity
import com.pocketsave.data.prefs.CartBackground
import com.pocketsave.data.prefs.CartBackgroundStore
import com.pocketsave.domain.model.CartStatus
import java.text.DateFormat

/**
 * Filtered view over completed trips with a rollup summary on top.
 *
 * Android-specific piece — iOS surfaces completed carts through
 * `Core/History/` but uses a more elaborate trip-detail drill-down.
 * Phase-follow-up scope keeps this lean: totals across trips, savings vs
 * budget, and a scrolling list that taps through to [CartDetailScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    vaultService: VaultService,
    backgroundStore: CartBackgroundStore,
    onBack: () -> Unit,
    onOpenCart: (String) -> Unit,
) {
    val state by vaultService.state.collectAsState()
    // Single bulk collection; rows read from the map below.
    val backgrounds by backgroundStore.allBackgrounds.collectAsState(initial = emptyMap())
    val completed = state.carts
        .filter { CartStatus.fromRaw(it.status) == CartStatus.COMPLETED }
        .sortedByDescending { it.completedAt ?: it.updatedAt }

    val rollup = remember(completed, state.cartItemsByCart) {
        computeRollup(vaultService, completed, state.cartItemsByCart)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            item { RollupCard(rollup = rollup) }

            if (completed.isEmpty()) {
                item { EmptyState() }
            } else {
                item { Text("Trips", style = MaterialTheme.typography.titleMedium) }
                items(completed, key = { it.id }) { cart ->
                    TripRow(
                        cart = cart,
                        totalSpent = vaultService.computeTotalSpent(
                            CartStatus.fromRaw(cart.status),
                            state.cartItemsByCart[cart.id].orEmpty(),
                        ),
                        background = backgrounds[cart.id] ?: CartBackground.EMPTY,
                        onClick = { onOpenCart(cart.id) },
                    )
                }
            }
        }
    }
}

private data class Rollup(
    val tripCount: Int,
    val totalSpent: Double,
    val totalBudget: Double,
) {
    val overBudget: Double get() = (totalSpent - totalBudget).coerceAtLeast(0.0)
    val underBudget: Double get() = (totalBudget - totalSpent).coerceAtLeast(0.0)
}

private fun computeRollup(
    vaultService: VaultService,
    completedCarts: List<CartEntity>,
    cartItemsByCart: Map<String, List<com.pocketsave.data.local.entity.CartItemEntity>>,
): Rollup {
    var totalSpent = 0.0
    var totalBudget = 0.0
    for (cart in completedCarts) {
        val items = cartItemsByCart[cart.id].orEmpty()
        totalSpent += vaultService.computeTotalSpent(CartStatus.fromRaw(cart.status), items)
        if (cart.budget > 0.0) totalBudget += cart.budget
    }
    return Rollup(tripCount = completedCarts.size, totalSpent = totalSpent, totalBudget = totalBudget)
}

@Composable
private fun RollupCard(rollup: Rollup) {
    val formatter = LocalCurrencyFormatter.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "${rollup.tripCount} completed trip${if (rollup.tripCount == 1) "" else "s"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total spent", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = formatter.format(rollup.totalSpent),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total budget", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = if (rollup.totalBudget > 0.0) formatter.format(rollup.totalBudget) else "—",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            if (rollup.totalBudget > 0.0) {
                val colour = if (rollup.overBudget > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                val label = if (rollup.overBudget > 0.0) {
                    "${formatter.format(rollup.overBudget)} over budget overall"
                } else {
                    "${formatter.format(rollup.underBudget)} under budget overall"
                }
                Text(label, color = colour, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TripRow(
    cart: CartEntity,
    totalSpent: Double,
    background: CartBackground,
    onClick: () -> Unit,
) {
    val formatter = LocalCurrencyFormatter.current
    val dateFormatter = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    val bgImage = background.imageUri
    val backgroundColor = ColorOption.byHex(background.colorHex ?: "")?.color
        ?: MaterialTheme.colorScheme.surface

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
                Text(
                    text = cart.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val completedLabel = cart.completedAt?.let { dateFormatter.format(it) } ?: "Completed"
                Text(
                    text = "$completedLabel · ${formatter.format(totalSpent)}" +
                        if (cart.budget > 0.0) " / ${formatter.format(cart.budget)}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("Completed trips will appear here.", style = MaterialTheme.typography.bodyLarge)
    }
}

