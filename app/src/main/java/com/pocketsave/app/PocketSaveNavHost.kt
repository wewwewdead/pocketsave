package com.pocketsave.app

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pocketsave.billing.PremiumFeature
import com.pocketsave.core.active.ActiveTripsScreen
import com.pocketsave.core.cart.CartDetailScreen
import com.pocketsave.core.cart.VaultSelectionStore
import com.pocketsave.core.history.HistoryScreen
import com.pocketsave.core.home.HomeScreen
import com.pocketsave.core.more.MoreScreen
import com.pocketsave.core.nav.BottomNavBar
import com.pocketsave.core.nav.NavTab
import com.pocketsave.core.onboarding.OnboardingContainer
import com.pocketsave.core.onboarding.OnboardingViewModel
import com.pocketsave.core.paywall.PaywallScreen
import com.pocketsave.core.scanner.TextRecognitionService
import com.pocketsave.core.scanner.classifier.PackagingClassifier
import com.pocketsave.core.service.VaultService
import com.pocketsave.core.trash.TrashScreen
import com.pocketsave.core.tripshare.TripShareScreen
import com.pocketsave.core.vault.VaultScreen
import com.pocketsave.data.prefs.AppPreferences
import com.pocketsave.data.prefs.LocalAppPreferences
import com.pocketsave.data.prefs.CartBackgroundStore

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val VAULT = "vault"
    const val CART_DETAIL = "cartDetail/{cartId}"
    const val TRIP_SHARE = "tripShare/{cartId}"
    const val HISTORY = "history"
    const val TRASH = "trash"
    const val ACTIVE_TRIPS = "activeTrips"
    const val MORE = "more"

    /**
     * Paywall is parameterised by the trigger that opened it so the hero
     * subtitle can read "here's why you're seeing this now". `none` is the
     * sentinel used when opened from ambient entry points (e.g. the More
     * tab's Pro row), where no feature-specific context applies.
     */
    const val PAYWALL = "paywall/{trigger}"

    fun cartDetail(cartId: String) = "cartDetail/$cartId"
    fun tripShare(cartId: String) = "tripShare/$cartId"
    fun paywall(triggerKey: String? = null): String =
        "paywall/${triggerKey?.takeIf { it.isNotBlank() } ?: "none"}"
}

// Routes that own a tab in the bottom nav. When the current destination is
// one of these the BottomNavBar is visible; everything else (cart detail,
// trip share, trash, onboarding) hides it so back-nav flows feel focused.
private val BOTTOM_NAV_ROUTES: Set<String> = setOf(
    Routes.HOME,
    Routes.VAULT,
    Routes.ACTIVE_TRIPS,
    Routes.HISTORY,
    Routes.MORE,
)

// ------- Page-transition specs (Duolingo-flavored) -----------------------
//
// Two character profiles share one NavHost:
//
//  - Lateral tab switches (Home ↔ Vault ↔ Trips ↔ History ↔ More) —
//    soft cross-fade + a tiny scale-up so it reads as "I'm in a different
//    room, not deeper into one."
//
//  - Push/pop (Home → CartDetail → back) — springy horizontal slide with a
//    firm-but-bouncy curve so detail screens "arrive" rather than simply
//    translate in linearly.

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean {
    val from = initialState.destination.route
    val to = targetState.destination.route
    return from in BOTTOM_NAV_ROUTES && to in BOTTOM_NAV_ROUTES
}

// Page slides use a damped tween, not a bouncy spring. Bouncy springs on a
// full-screen slide overshoot past the resting position, which reads as
// "off-positioned then settles" — wrong for page navigation. Bounce stays
// where it belongs: press feedback and the nav-tab icon hop.
private const val PagePushMs = 340
private const val PagePopMs = 280
private const val TabFadeMs = 220

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enterSpec(): EnterTransition {
    return if (isTabSwitch()) {
        // Lateral feel: soft cross-fade + a whisper of scale.
        fadeIn(animationSpec = tween(TabFadeMs)) +
            scaleIn(
                initialScale = 0.98f,
                animationSpec = tween(TabFadeMs, easing = LinearOutSlowInEasing),
            )
    } else {
        // Deeper push: slide in from fully offscreen right using a damped
        // tween. No initialOffset override — defaults to { full } so the
        // page starts *entirely* off-screen and arrives cleanly at rest.
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(PagePushMs, easing = FastOutSlowInEasing),
        ) + fadeIn(animationSpec = tween(PagePushMs / 2))
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exitSpec(): ExitTransition {
    return if (isTabSwitch()) {
        fadeOut(animationSpec = tween(TabFadeMs - 60)) +
            scaleOut(
                targetScale = 1.02f,
                animationSpec = tween(TabFadeMs, easing = LinearOutSlowInEasing),
            )
    } else {
        // Outgoing page sits still under the incoming slide, just fades
        // slightly — no scale or slide so the layered motion stays calm.
        fadeOut(animationSpec = tween(PagePushMs / 2))
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnterSpec(): EnterTransition {
    return if (isTabSwitch()) {
        enterSpec()
    } else {
        // The previous screen should simply be *there* as the detail slides
        // away — fading it in would leave an empty frame underneath the
        // outgoing screen for a moment, which reads as a flicker.
        EnterTransition.None
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExitSpec(): ExitTransition {
    return if (isTabSwitch()) {
        exitSpec()
    } else {
        // Pop: detail slides off to the right fully and fades.
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(PagePopMs, easing = FastOutSlowInEasing),
        ) + fadeOut(animationSpec = tween(PagePopMs / 2))
    }
}

@Composable
fun PocketSaveNavHost(
    preferences: AppPreferences,
    vaultService: VaultService,
    selectionStore: VaultSelectionStore,
    backgroundStore: CartBackgroundStore,
    textRecognitionService: TextRecognitionService,
    packagingClassifier: PackagingClassifier,
    appContainer: AppContainer,
) {
    // Onboarding flag resolves off the main thread; render theme bg while it's
    // in flight so the splash carries continuously into the first frame.
    val hasCompletedOnboarding by produceState<Boolean?>(initialValue = null, preferences) {
        value = preferences.hasCompletedOnboardingNow()
    }

    if (hasCompletedOnboarding == null) {
        SplashBackground()
        return
    }

    val navController = rememberNavController()
    val pendingDeepLink by appContainer.pendingDeepLink.collectAsState()
    val startDestination = if (hasCompletedOnboarding == true) Routes.HOME else Routes.ONBOARDING

    LaunchedEffect(pendingDeepLink) {
        val pending = pendingDeepLink ?: return@LaunchedEffect
        navController.navigate(Routes.cartDetail(pending.cartId)) {
            launchSingleTop = true
        }
    }

    // Observe the current destination so the bottom nav can highlight the
    // active tab and hide itself on non-tab routes (cart detail, onboarding,
    // trip share, trash).
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomNav = currentRoute in BOTTOM_NAV_ROUTES

    CompositionLocalProvider(LocalAppPreferences provides preferences) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Bar transitions are size-aware: slide + measured-height animate
            // together, matched to the page-push duration. Without the
            // `expandVertically` / `shrinkVertically` pair, the bar's slot in
            // the Scaffold would snap from full-height to 0 at the end of the
            // exit animation, causing the destination page's bottom-aligned
            // content (finish-trip buttons, etc.) to shift the moment the
            // slide finished. Animating the measured height in lockstep with
            // the slide lets the outer container grow smoothly *during* the
            // page transition, so nothing re-lays-out when it ends.
            AnimatedVisibility(
                visible = showBottomNav,
                enter = slideInVertically(
                    animationSpec = tween(PagePushMs, easing = FastOutSlowInEasing),
                    initialOffsetY = { full -> full },
                ) + expandVertically(
                    animationSpec = tween(PagePushMs, easing = FastOutSlowInEasing),
                ) + fadeIn(
                    animationSpec = tween(PagePushMs / 2),
                ),
                exit = slideOutVertically(
                    animationSpec = tween(PagePushMs, easing = FastOutSlowInEasing),
                    targetOffsetY = { full -> full },
                ) + shrinkVertically(
                    animationSpec = tween(PagePushMs, easing = FastOutSlowInEasing),
                ) + fadeOut(
                    animationSpec = tween(PagePushMs / 2),
                ),
            ) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onTabSelected = { tab -> navigateToTab(navController, tab) },
                )
            }
        },
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(inner),
            enterTransition = { enterSpec() },
            exitTransition = { exitSpec() },
            popEnterTransition = { popEnterSpec() },
            popExitTransition = { popExitSpec() },
        ) {
            composable(Routes.ONBOARDING) {
                val onboardingVm: OnboardingViewModel = viewModel(
                    factory = OnboardingViewModel.Factory(
                        vaultService = vaultService,
                        preferences = preferences,
                    ),
                )
                OnboardingContainer(
                    viewModel = onboardingVm,
                    onComplete = { goToHome(navController) },
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    vaultService = vaultService,
                    selectionStore = selectionStore,
                    backgroundStore = backgroundStore,
                    preferences = preferences,
                    subscriptionManager = appContainer.subscriptionManager,
                    onOpenVault = { navigateToTab(navController, NavTab.VAULT) },
                    onOpenCart = { cartId -> navController.navigate(Routes.cartDetail(cartId)) },
                    onOpenHistory = { navigateToTab(navController, NavTab.HISTORY) },
                    onOpenTrash = { navController.navigate(Routes.TRASH) },
                    onOpenPaywall = { feature ->
                        navController.navigate(Routes.paywall(feature.key))
                    },
                    onOpenActiveTrips = { navigateToTab(navController, NavTab.TRIPS) },
                )
            }
            composable(Routes.VAULT) {
                VaultScreen(
                    vaultService = vaultService,
                    selectionStore = selectionStore,
                    textRecognitionService = textRecognitionService,
                    packagingClassifier = packagingClassifier,
                    subscriptionManager = appContainer.subscriptionManager,
                    onBack = { navController.popBackStack() },
                    onCreateCartRequested = { cartId ->
                        navController.navigate(Routes.cartDetail(cartId)) {
                            popUpTo(Routes.VAULT) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenPaywall = { feature ->
                        navController.navigate(Routes.paywall(feature.key))
                    },
                )
            }
            composable(
                route = Routes.CART_DETAIL,
                arguments = listOf(navArgument("cartId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val cartId = backStackEntry.arguments?.getString("cartId").orEmpty()
                CartDetailScreen(
                    vaultService = vaultService,
                    backgroundStore = backgroundStore,
                    subscriptionManager = appContainer.subscriptionManager,
                    cartId = cartId,
                    onBack = { navController.popBackStack() },
                    onOpenPaywall = { feature ->
                        navController.navigate(Routes.paywall(feature.key))
                    },
                    onCompleteTripDone = { returnToHome(navController) },
                    onShareTrip = { id -> navController.navigate(Routes.tripShare(id)) },
                    pendingDeepLink = pendingDeepLink?.takeIf { it.cartId == cartId },
                    onDeepLinkConsumed = { appContainer.consumeDeepLink() },
                )
            }
            composable(
                route = Routes.TRIP_SHARE,
                arguments = listOf(navArgument("cartId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val cartId = backStackEntry.arguments?.getString("cartId").orEmpty()
                TripShareScreen(
                    vaultService = vaultService,
                    backgroundStore = backgroundStore,
                    cartId = cartId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    vaultService = vaultService,
                    backgroundStore = backgroundStore,
                    subscriptionManager = appContainer.subscriptionManager,
                    onBack = { navController.popBackStack() },
                    onOpenCart = { id -> navController.navigate(Routes.cartDetail(id)) },
                    onOpenPaywall = { feature ->
                        navController.navigate(Routes.paywall(feature.key))
                    },
                )
            }
            composable(Routes.TRASH) {
                TrashScreen(
                    vaultService = vaultService,
                    cartBackgroundStore = backgroundStore,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ACTIVE_TRIPS) {
                ActiveTripsScreen(
                    vaultService = vaultService,
                    selectionStore = selectionStore,
                    subscriptionManager = appContainer.subscriptionManager,
                    onBack = { navController.popBackStack() },
                    onOpenCart = { id -> navController.navigate(Routes.cartDetail(id)) },
                    onOpenPaywall = { feature ->
                        navController.navigate(Routes.paywall(feature.key))
                    },
                )
            }
            composable(Routes.MORE) {
                MoreScreen(
                    vaultService = vaultService,
                    preferences = preferences,
                    subscriptionManager = appContainer.subscriptionManager,
                    onOpenTrash = { navController.navigate(Routes.TRASH) },
                    onOpenPaywall = { navController.navigate(Routes.paywall()) },
                    onResetApp = { appContainer.resetAllData() },
                    onResetCompleted = {
                        navController.navigate(Routes.ONBOARDING) {
                            popUpTo(Routes.MORE) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = Routes.PAYWALL,
                arguments = listOf(navArgument("trigger") { type = NavType.StringType }),
            ) { backStackEntry ->
                val triggerKey = backStackEntry.arguments?.getString("trigger")
                    ?.takeIf { it.isNotBlank() && it != "none" }
                PaywallScreen(
                    subscriptionManager = appContainer.subscriptionManager,
                    trigger = PremiumFeature.fromKey(triggerKey),
                    analytics = appContainer.billingAnalytics,
                    onDismiss = { navController.popBackStack() },
                )
            }
        }
    }
    }
}

/**
 * Standard bottom-nav navigation: pop back to the start destination (keeping
 * state so Home re-entries don't re-fetch), restore any saved scroll/state
 * for the target tab, single-top so re-tapping a tab is a no-op.
 */
private fun navigateToTab(navController: NavHostController, tab: NavTab) {
    navController.navigate(tab.route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun SplashBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    )
}

private fun goToHome(navController: NavHostController) {
    navController.navigate(Routes.HOME) {
        popUpTo(Routes.ONBOARDING) { inclusive = true }
        launchSingleTop = true
    }
}

private fun returnToHome(navController: NavHostController) {
    navController.navigate(Routes.HOME) {
        popUpTo(Routes.HOME) { inclusive = false }
        launchSingleTop = true
    }
}
