package com.pocketsave.core.active

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketsave.billing.FeatureLimits
import com.pocketsave.billing.PremiumFeature
import com.pocketsave.billing.SubscriptionManager
import com.pocketsave.billing.rememberPaywallGate
import com.pocketsave.common.ui.CardShadowColor
import com.pocketsave.common.ui.Motion
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.core.cart.CreateCartSheet
import com.pocketsave.core.cart.VaultSelectionStore
import com.pocketsave.core.paywall.CapHintBanner
import com.pocketsave.core.currency.LocalCurrencyFormatter
import com.pocketsave.core.home.pressScale
import com.pocketsave.core.service.VaultService
import com.pocketsave.data.local.entity.CartEntity
import com.pocketsave.domain.model.CartStatus
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Full-page listing of every non-completed cart (planning + shopping), sorted
 * newest first. Shares the home screen's soft visual vocabulary so navigating
 * in from the Active-trips pill doesn't feel like a style shift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveTripsScreen(
    vaultService: VaultService,
    selectionStore: VaultSelectionStore,
    subscriptionManager: SubscriptionManager,
    onBack: () -> Unit,
    onOpenCart: (String) -> Unit,
    onOpenPaywall: (PremiumFeature) -> Unit,
) {
    val state by vaultService.state.collectAsState()
    val selectedItems by selectionStore.activeCartItems.collectAsState()
    val formatter = LocalCurrencyFormatter.current
    var showCreateCartSheet by remember { mutableStateOf(false) }

    val paywallGate = rememberPaywallGate(subscriptionManager, vaultService, onOpenPaywall)
    val requestCreateCart: () -> Unit = {
        paywallGate.check(PremiumFeature.CreateActiveTrip) { showCreateCartSheet = true }
    }

    val active = remember(state.carts) {
        state.carts
            .filter { CartStatus.fromRaw(it.status) != CartStatus.COMPLETED }
            .sortedByDescending { it.updatedAt }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Active trips",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 4.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "summary") {
                ActiveSummaryCard(
                    count = active.size,
                    totalSpent = active.sumOf { cart ->
                        val items = state.cartItemsByCart[cart.id].orEmpty()
                        vaultService.computeTotalSpent(CartStatus.fromRaw(cart.status), items)
                    },
                    totalBudget = active.sumOf { if (it.budget > 0.0) it.budget else 0.0 },
                    spentLabelFor = { formatter.format(it) },
                    onCreateTrip = requestCreateCart,
                )
            }

            if (!paywallGate.isAllowed(PremiumFeature.CreateActiveTrip)) {
                item(key = "trip-cap-banner") {
                    CapHintBanner(
                        label = "${active.size} of ${FeatureLimits.FREE_ACTIVE_CARTS} trips running.",
                        onUpgrade = { onOpenPaywall(PremiumFeature.CreateActiveTrip) },
                    )
                }
            }

            if (active.isEmpty()) {
                item(key = "empty") {
                    EmptyActiveState(onCreateTrip = requestCreateCart)
                }
            } else {
                items(active, key = { it.id }) { cart ->
                    val items = state.cartItemsByCart[cart.id].orEmpty()
                    val status = CartStatus.fromRaw(cart.status)
                    val spent = vaultService.computeTotalSpent(status, items)
                    ActiveTripRowCard(
                        cart = cart,
                        status = status,
                        itemCount = items.size,
                        spentLabel = formatter.format(spent),
                        budgetLabel = cart.budget.takeIf { it > 0.0 }?.let { formatter.format(it) },
                        progress = if (cart.budget > 0.0 && spent > 0.0) {
                            (spent / cart.budget).toFloat().coerceIn(0f, 1f)
                        } else 0f,
                        onClick = { onOpenCart(cart.id) },
                    )
                }
            }
        }
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
}

@Composable
private fun ActiveSummaryCard(
    count: Int,
    totalSpent: Double,
    totalBudget: Double,
    spentLabelFor: (Double) -> String,
    onCreateTrip: () -> Unit,
) {
    val pastels = PocketSaveTokens.pastels
    val interaction = remember { MutableInteractionSource() }
    Surface(
        color = pastels.mintSoft,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "ONGOING",
                style = MaterialTheme.typography.labelSmall,
                color = pastels.mintDeep.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$count trip${if (count == 1) "" else "s"} in progress",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = pastels.mintDeep,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Spent so far",
                        style = MaterialTheme.typography.labelSmall,
                        color = pastels.mintDeep.copy(alpha = 0.78f),
                    )
                    Text(
                        text = spentLabelFor(totalSpent),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = pastels.mintDeep,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total budget",
                        style = MaterialTheme.typography.labelSmall,
                        color = pastels.mintDeep.copy(alpha = 0.78f),
                    )
                    Text(
                        text = if (totalBudget > 0.0) spentLabelFor(totalBudget) else "—",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = pastels.mintDeep,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .pressScale(interaction)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onCreateTrip,
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Start a new trip",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveTripRowCard(
    cart: CartEntity,
    status: CartStatus,
    itemCount: Int,
    spentLabel: String,
    budgetLabel: String?,
    progress: Float,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pastels = PocketSaveTokens.pastels
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(Motion.MediumMs),
        label = "active-row-progress",
    )
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = CardShadowColor,
                spotColor = CardShadowColor,
            )
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(status = status)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(pastels.canvasTint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = cart.name.ifBlank { "Untitled trip" },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$itemCount item${if (itemCount == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            BudgetBar(progress = animatedProgress, hasBudget = budgetLabel != null)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = spentLabel,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (budgetLabel != null) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "of $budgetLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                } else {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "tracked",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: CartStatus) {
    val pastels = PocketSaveTokens.pastels
    val (tint, ink) = when (status) {
        CartStatus.SHOPPING -> pastels.mintSoft to pastels.mintDeep
        CartStatus.PLANNING -> pastels.lavenderSoft to pastels.lavenderDeep
        CartStatus.COMPLETED -> pastels.canvasTint to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = tint,
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(ink),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = status.displayName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = ink,
            )
        }
    }
}

@Composable
private fun BudgetBar(progress: Float, hasBudget: Boolean) {
    val pastels = PocketSaveTokens.pastels
    val track = pastels.canvasTint
    val fill = when {
        !hasBudget -> pastels.mintDeep.copy(alpha = 0.25f)
        // Over/near-budget fill uses the clay warning — sage for everything else.
        progress >= 0.85f -> pastels.blushDeep
        else -> pastels.mintDeep
    }
    val display = if (hasBudget) progress else 0.18f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(track),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(display)
                .clip(RoundedCornerShape(999.dp))
                .background(fill),
        )
    }
}

@Composable
private fun EmptyActiveState(onCreateTrip: () -> Unit) {
    val pastels = PocketSaveTokens.pastels
    val interaction = remember { MutableInteractionSource() }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onCreateTrip,
            ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(pastels.peachSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddShoppingCart,
                    contentDescription = null,
                    tint = pastels.peachDeep,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "No trips in progress yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tap to start a new trip — set a budget, pull from your vault, and we'll track it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
