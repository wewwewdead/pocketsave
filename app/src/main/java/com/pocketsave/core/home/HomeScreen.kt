package com.pocketsave.core.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsave.billing.PremiumFeature
import com.pocketsave.billing.SubscriptionManager
import com.pocketsave.billing.rememberPaywallGate
import com.pocketsave.common.ui.PocketSaveColors
import com.pocketsave.common.ui.PocketSaveDimens
import com.pocketsave.core.cart.CreateCartSheet
import com.pocketsave.core.cart.VaultSelectionStore
import com.pocketsave.core.currency.LocalCurrencyFormatter
import com.pocketsave.core.home.binder.BinderPage
import com.pocketsave.core.home.binder.BinderSidebar
import com.pocketsave.core.home.components.CategoryChipDisplay
import com.pocketsave.core.home.components.HomeCartRow
import com.pocketsave.core.home.components.HomeCartRowItem
import com.pocketsave.core.home.components.HomePrimaryHeader
import com.pocketsave.core.home.components.HomeTab
import com.pocketsave.core.home.components.HomeTabsBar
import com.pocketsave.core.home.menu.MenuDrawer
import com.pocketsave.core.home.menu.SideMenu
import com.pocketsave.core.scanner.TextRecognitionService
import com.pocketsave.core.scanner.classifier.PackagingClassifier
import com.pocketsave.core.service.VaultService
import com.pocketsave.core.vault.VaultScreen
import com.pocketsave.data.prefs.AppPreferences
import com.pocketsave.data.prefs.CartBackgroundStore
import com.pocketsave.domain.model.CartStatus
import java.util.Calendar
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * iOS-parity Home.
 *
 * This is a single-screen composable that internally handles:
 *   - binder sidebar (Home / Insights(soon) pages)
 *   - Active / History tabs via HorizontalPager
 *   - side menu drawer (settings, trash, reset)
 *   - vault sheet presentation
 *   - create cart sheet
 *
 * The prior VAULT / HISTORY / ACTIVE_TRIPS / MORE routes from the Android graph
 * fold into this single screen.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    vaultService: VaultService,
    selectionStore: VaultSelectionStore,
    backgroundStore: CartBackgroundStore,
    preferences: AppPreferences,
    subscriptionManager: SubscriptionManager,
    textRecognitionService: TextRecognitionService,
    packagingClassifier: PackagingClassifier,
    onOpenCart: (cartId: String) -> Unit,
    onOpenTrash: () -> Unit,
    onOpenPaywall: (PremiumFeature) -> Unit,
    onResetApp: suspend () -> Unit,
    onResetCompleted: () -> Unit,
) {
    val state by vaultService.state.collectAsState()
    val formatter = LocalCurrencyFormatter.current
    val active = state.carts.filter { CartStatus.fromRaw(it.status) != CartStatus.COMPLETED }
    val completed = state.carts.filter { CartStatus.fromRaw(it.status) == CartStatus.COMPLETED }
    val scope = rememberCoroutineScope()

    val netSavings = remember(completed, state.cartItemsByCart) {
        completed.sumOf { cart ->
            if (cart.budget <= 0.0) 0.0
            else {
                val items = state.cartItemsByCart[cart.id].orEmpty()
                cart.budget - vaultService.computeTotalSpent(CartStatus.COMPLETED, items)
            }
        }
    }

    val selectedItems by selectionStore.activeCartItems.collectAsState()
    val selectedItemCount = selectedItems.values.count { it > 0.0 }

    // UI state.
    var isMenuOpen by remember { mutableStateOf(false) }
    var isVaultOpen by remember { mutableStateOf(false) }
    var binderPage by remember { mutableStateOf(BinderPage.HOME) }
    var showCreateCartSheet by remember { mutableStateOf(false) }

    val paywallGate = rememberPaywallGate(subscriptionManager, vaultService, onOpenPaywall)
    val requestCreateCart: () -> Unit = {
        paywallGate.check(PremiumFeature.CreateActiveTrip) { showCreateCartSheet = true }
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val selectedTab = if (pagerState.currentPage == 0) HomeTab.ACTIVE else HomeTab.HISTORY
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()

    // Map each cart into the iOS-style card view model.
    val pastels = com.pocketsave.common.ui.PocketSaveTokens.pastels
    val chipPalette = listOf(
        pastels.mintSoft to pastels.mintDeep,
        pastels.peachSoft to pastels.peachDeep,
        pastels.butterSoft to pastels.butterDeep,
        pastels.lavenderSoft to pastels.lavenderDeep,
        pastels.skySoft to pastels.skyDeep,
    )

    fun cartToRowItem(cart: com.pocketsave.data.local.entity.CartEntity): HomeCartRowItem {
        val status = CartStatus.fromRaw(cart.status)
        val items = state.cartItemsByCart[cart.id].orEmpty()
        val spent = vaultService.computeTotalSpent(status, items)
        val hasBudget = cart.budget > 0.0
        val over = hasBudget && spent > cart.budget
        val remainingLabel = when {
            !hasBudget -> null
            over -> formatter.format(spent - cart.budget)
            else -> formatter.format((cart.budget - spent).coerceAtLeast(0.0))
        }

        // Build category chips from items' categories.
        val categoryById = state.categories.associateBy { it.uid }
        val itemsByCategory = items
            .asSequence()
            .mapNotNull { cartItem -> cartItem.itemId?.let { id -> state.items.firstOrNull { it.id == id } } }
            .groupBy { it.categoryUid }
        val chips = itemsByCategory.entries
            .mapIndexedNotNull { idx, (catUid, _) ->
                val cat = catUid?.let { categoryById[it] } ?: return@mapIndexedNotNull null
                val tint = chipPalette[idx % chipPalette.size].first
                CategoryChipDisplay(
                    name = cat.name,
                    emoji = cat.iconKey,
                    tint = tint,
                )
            }

        return HomeCartRowItem(
            cartId = cart.id,
            name = cart.name,
            isShopping = status == CartStatus.SHOPPING,
            fulfilledCount = items.count { it.isFulfilled && !it.isSkippedDuringShopping },
            totalItemCount = items.count { !it.isSkippedDuringShopping },
            budget = cart.budget,
            totalSpent = spent,
            spentLabel = formatter.format(spent),
            budgetLabel = cart.budget.takeIf { it > 0.0 }?.let { formatter.format(it) },
            remainingLabel = remainingLabel,
            overBudget = over,
            categoryChips = chips,
        )
    }

    val activeRows: List<HomeCartRowItem> = remember(active, state.cartItemsByCart, state.categories, state.items) {
        active.map { cartToRowItem(it) }
    }

    val historyRows: List<HomeCartRowItem> = remember(completed, state.cartItemsByCart, state.categories, state.items) {
        completed.map { cartToRowItem(it) }
    }

    // Drawer wraps everything.
    MenuDrawer(
        isOpen = isMenuOpen,
        onClose = { isMenuOpen = false },
        menuContent = {
            SideMenu(
                vaultService = vaultService,
                preferences = preferences,
                subscriptionManager = subscriptionManager,
                onOpenTrash = {
                    isMenuOpen = false
                    onOpenTrash()
                },
                onOpenPaywall = {
                    isMenuOpen = false
                    onOpenPaywall(PremiumFeature.CreateActiveTrip)
                },
                onResetApp = onResetApp,
                onResetCompleted = {
                    isMenuOpen = false
                    onResetCompleted()
                },
                onClose = { isMenuOpen = false },
            )
        },
        homeContent = {
            HomeContentRoot(
                netSavings = netSavings,
                currencyFormatter = { formatter.format(it) },
                statusBarPadding = statusBarPadding,
                binderPage = binderPage,
                onSelectBinderPage = { binderPage = it },
                onOpenVault = { isVaultOpen = true },
                onOpenCart = onOpenCart,
                onOpenMenu = { isMenuOpen = true },
                isMenuOpen = isMenuOpen,
                onToggleMenu = { isMenuOpen = !isMenuOpen },
                selectedTab = selectedTab,
                pagerState = pagerState,
                onTabSelected = { tab ->
                    scope.launch {
                        pagerState.animateScrollToPage(if (tab == HomeTab.ACTIVE) 0 else 1)
                    }
                },
                activeRows = activeRows,
                historyRows = historyRows,
                currentYear = currentYear,
                onCreateCart = requestCreateCart,
            )
        },
    )

    // Vault sheet. iOS: presentationCornerRadius(32), default drag indicator.
    if (isVaultOpen) {
        ModalBottomSheet(
            onDismissRequest = { isVaultOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(
                topStart = PocketSaveDimens.VaultSheetCornerRadius,
                topEnd = PocketSaveDimens.VaultSheetCornerRadius,
            ),
            dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() },
        ) {
            VaultScreen(
                vaultService = vaultService,
                selectionStore = selectionStore,
                textRecognitionService = textRecognitionService,
                packagingClassifier = packagingClassifier,
                subscriptionManager = subscriptionManager,
                onBack = { isVaultOpen = false },
                onCreateCartRequested = { cartId ->
                    isVaultOpen = false
                    onOpenCart(cartId)
                },
                onOpenPaywall = onOpenPaywall,
            )
        }
    }

    // Create cart sheet.
    if (showCreateCartSheet) {
        CreateCartSheet(
            vaultService = vaultService,
            selectionStore = selectionStore,
            selectedItemCount = selectedItemCount,
            onDismiss = { showCreateCartSheet = false },
            onCreated = { cartId ->
                showCreateCartSheet = false
                onOpenCart(cartId)
            },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun HomeContentRoot(
    netSavings: Double,
    currencyFormatter: (Double) -> String,
    statusBarPadding: androidx.compose.ui.unit.Dp,
    binderPage: BinderPage,
    onSelectBinderPage: (BinderPage) -> Unit,
    onOpenVault: () -> Unit,
    onOpenCart: (cartId: String) -> Unit,
    onOpenMenu: () -> Unit,
    isMenuOpen: Boolean,
    onToggleMenu: () -> Unit,
    selectedTab: HomeTab,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onTabSelected: (HomeTab) -> Unit,
    activeRows: List<HomeCartRowItem>,
    historyRows: List<HomeCartRowItem>,
    currentYear: String,
    onCreateCart: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(PocketSaveColors.HomeBaseGrey),
    ) {
        Row(Modifier.fillMaxSize()) {
            // Binder sidebar.
            Box(
                Modifier
                    .width(PocketSaveDimens.BinderWidth)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            if (dragAmount > 0 && abs(change.position.x) > 0) {
                                // Right-swipe over the binder opens the side menu.
                                if (dragAmount > 20f) onOpenMenu()
                            }
                        }
                    },
            ) {
                BinderSidebar(
                    selected = binderPage,
                    onSelect = onSelectBinderPage,
                    safeAreaTopPadding = statusBarPadding,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Main page surface.
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(Color.White),
            ) {
                when (binderPage) {
                    BinderPage.HOME -> HomePageContent(
                        netSavings = netSavings,
                        currencyFormatter = currencyFormatter,
                        statusBarPadding = statusBarPadding,
                        onOpenVault = onOpenVault,
                        onOpenCart = onOpenCart,
                        selectedTab = selectedTab,
                        pagerState = pagerState,
                        onTabSelected = onTabSelected,
                        activeRows = activeRows,
                        historyRows = historyRows,
                        currentYear = currentYear,
                        onCreateCart = onCreateCart,
                    )
                    BinderPage.STATISTICS -> StatisticsPageContent(statusBarPadding = statusBarPadding)
                }
            }
        }

        // Floating settings chevron — vertically centered on the left edge.
        // Faces left when closed; rotates 180° to face right when the menu is open.
        SideMenuChevron(
            isMenuOpen = isMenuOpen,
            onToggle = onToggleMenu,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = PocketSaveDimens.BinderWidth + 4.dp),
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun HomePageContent(
    netSavings: Double,
    currencyFormatter: (Double) -> String,
    statusBarPadding: androidx.compose.ui.unit.Dp,
    onOpenVault: () -> Unit,
    onOpenCart: (cartId: String) -> Unit,
    selectedTab: HomeTab,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onTabSelected: (HomeTab) -> Unit,
    activeRows: List<HomeCartRowItem>,
    historyRows: List<HomeCartRowItem>,
    currentYear: String,
    onCreateCart: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            HomePrimaryHeader(
                netSavings = netSavings,
                currencyFormatter = currencyFormatter,
                safeAreaTopPadding = statusBarPadding,
                onOpenVault = onOpenVault,
            )

            HomeTabsBar(
                selected = selectedTab,
                onTabSelected = onTabSelected,
                trailingTitle = if (selectedTab == HomeTab.HISTORY) currentYear else null,
            )

            Spacer(Modifier.height(8.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    0 -> ActivePage(
                        rows = activeRows,
                        onOpenCart = onOpenCart,
                        onCreateCart = onCreateCart,
                    )
                    1 -> HistoryPage(
                        rows = historyRows,
                        onOpenCart = onOpenCart,
                    )
                }
            }
        }

        CreateCartFab(
            onClick = onCreateCart,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 32.dp),
        )
    }
}

@Composable
private fun ActivePage(
    rows: List<HomeCartRowItem>,
    onOpenCart: (cartId: String) -> Unit,
    onCreateCart: () -> Unit,
) {
    if (rows.isEmpty()) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No active carts yet — tap ⊕ to start",
                color = PocketSaveColors.TextMuted,
                fontSize = 15.sp,
            )
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 10.dp,
                end = 10.dp,
                top = 4.dp,
                bottom = 100.dp, // clear the FAB
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items = rows, key = { it.cartId }) { row ->
                HomeCartRow(
                    item = row,
                    onOpen = { onOpenCart(row.cartId) },
                )
            }
        }
    }
}

@Composable
private fun HistoryPage(
    rows: List<HomeCartRowItem>,
    onOpenCart: (cartId: String) -> Unit,
) {
    if (rows.isEmpty()) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No completed trips yet",
                color = PocketSaveColors.TextMuted,
                fontSize = 15.sp,
            )
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 10.dp,
                end = 10.dp,
                top = 4.dp,
                bottom = 100.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items = rows, key = { it.cartId }) { row ->
                HomeCartRow(
                    item = row,
                    onOpen = { onOpenCart(row.cartId) },
                )
            }
        }
    }
}

@Composable
private fun StatisticsPageContent(statusBarPadding: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .fillMaxSize()
            .background(PocketSaveColors.HomeBaseGrey)
            .padding(top = statusBarPadding + 24.dp, start = 20.dp, end = 20.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column {
            Text(
                text = "Insights",
                style = MaterialTheme.typography.displayMedium,
                color = PocketSaveColors.DarkPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Coming soon.",
                color = PocketSaveColors.TextMuted,
                fontSize = 15.sp,
            )
        }
    }
}

/**
 * Floating disclosure chevron that lives at the left-center edge of the home
 * surface. Tapping it toggles the side menu. Rotates 0° → 180° in a soft
 * overshoot spring so the arrow flips from `<` to `>` (and back) with a subtle
 * bounce that mirrors the menu drawer's own spring.
 */
@Composable
private fun SideMenuChevron(
    isMenuOpen: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isMenuOpen) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 260f),
        label = "sideMenuChevronRotation",
    )
    val scale by animateFloatAsState(
        targetValue = if (isMenuOpen) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 320f),
        label = "sideMenuChevronScale",
    )
    val haptics = com.pocketsave.core.haptics.rememberAppHaptics()
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(36.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.95f))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    haptics.perform(com.pocketsave.core.haptics.AppHaptic.Light)
                    onToggle()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.ChevronLeft,
            contentDescription = if (isMenuOpen) "Close settings menu" else "Open settings menu",
            tint = PocketSaveColors.DarkPrimary,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer { rotationZ = rotation },
        )
    }
}

@Composable
private fun CreateCartFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = com.pocketsave.core.haptics.rememberAppHaptics()
    Box(
        modifier
            .size(PocketSaveDimens.FabDiameter)
            .clip(CircleShape)
            .background(PocketSaveColors.DarkPrimary)
            .clickable {
                haptics.perform(com.pocketsave.core.haptics.AppHaptic.Light)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = "Create cart",
            tint = Color.White,
            modifier = Modifier.size(28.dp),
        )
    }
}

