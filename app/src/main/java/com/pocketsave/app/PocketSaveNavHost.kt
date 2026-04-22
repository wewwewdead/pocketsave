package com.pocketsave.app

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pocketsave.billing.PremiumFeature
import com.pocketsave.core.cart.CartDetailScreen
import com.pocketsave.core.cart.VaultSelectionStore
import com.pocketsave.core.home.HomeScreen
import com.pocketsave.core.onboarding.OnboardingContainer
import com.pocketsave.core.onboarding.OnboardingViewModel
import com.pocketsave.core.paywall.PaywallScreen
import com.pocketsave.core.scanner.TextRecognitionService
import com.pocketsave.core.scanner.classifier.PackagingClassifier
import com.pocketsave.core.service.VaultService
import com.pocketsave.core.trash.TrashScreen
import com.pocketsave.core.tripshare.TripShareScreen
import com.pocketsave.data.prefs.AppPreferences
import com.pocketsave.data.prefs.CartBackgroundStore
import com.pocketsave.data.prefs.LocalAppPreferences

/**
 * iOS-parity navigation.
 *
 * iOS has exactly ONE top-level screen — `HomeView` — which internally handles:
 *   - binder sidebar (Home / Statistics pages)
 *   - Active / History tabs via HorizontalPager
 *   - side menu drawer (settings, stores, trash, etc.)
 *   - vault sheet presentation
 *
 * There is no bottom nav. Detail screens (cart, trip share) push via navigation,
 * and onboarding / paywall live as full-screen covers. That's the shape we
 * model here.
 *
 * Removed from the prior Android graph: VAULT, HISTORY, ACTIVE_TRIPS, MORE
 * routes — their content now lives inside HomeScreen + MenuDrawer + VaultSheet.
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val CART_DETAIL = "cartDetail/{cartId}"
    const val TRIP_SHARE = "tripShare/{cartId}"
    const val TRASH = "trash"

    /**
     * Paywall parameterised by trigger so the hero subtitle can say "here's why
     * you're seeing this now". `none` is the sentinel for ambient entry points.
     */
    const val PAYWALL = "paywall/{trigger}"

    fun cartDetail(cartId: String) = "cartDetail/$cartId"
    fun tripShare(cartId: String) = "tripShare/$cartId"
    fun paywall(triggerKey: String? = null): String =
        "paywall/${triggerKey?.takeIf { it.isNotBlank() } ?: "none"}"
}

// -----------------------------------------------------------------------------
// Transitions — the wrap-up (cart → home) gets its own longer fade-scale because
// it marks the emotional end of a trip. Everything else uses a standard slide.
// -----------------------------------------------------------------------------

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isWrapUpReturn(): Boolean {
    val from = initialState.destination.route
    val to = targetState.destination.route
    return from == Routes.CART_DETAIL && to == Routes.HOME
}

private const val WrapUpMs = 460
private const val PagePushMs = 340
private const val PagePopMs = 280

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enterSpec(): EnterTransition =
    when {
        isWrapUpReturn() -> fadeIn(tween(WrapUpMs, easing = FastOutSlowInEasing)) +
            scaleIn(initialScale = 1.03f, animationSpec = tween(WrapUpMs, easing = FastOutSlowInEasing))
        else -> slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(PagePushMs, easing = FastOutSlowInEasing),
        ) + fadeIn(tween(PagePushMs / 2))
    }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exitSpec(): ExitTransition =
    when {
        isWrapUpReturn() -> fadeOut(tween(WrapUpMs, easing = FastOutSlowInEasing)) +
            scaleOut(targetScale = 0.97f, animationSpec = tween(WrapUpMs, easing = FastOutSlowInEasing))
        else -> fadeOut(tween(PagePushMs / 2))
    }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnterSpec(): EnterTransition =
    EnterTransition.None

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExitSpec(): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(PagePopMs, easing = FastOutSlowInEasing),
    ) + fadeOut(tween(PagePopMs / 2))

// -----------------------------------------------------------------------------
// NavHost.
// -----------------------------------------------------------------------------

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
    // Onboarding flag resolves off main thread; splash background bridges the gap.
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

    CompositionLocalProvider(LocalAppPreferences provides preferences) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
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
                        textRecognitionService = textRecognitionService,
                        packagingClassifier = packagingClassifier,
                        onOpenCart = { cartId -> navController.navigate(Routes.cartDetail(cartId)) },
                        onOpenTrash = { navController.navigate(Routes.TRASH) },
                        onOpenPaywall = { feature ->
                            navController.navigate(Routes.paywall(feature.key))
                        },
                        onResetApp = { appContainer.resetAllData() },
                        onResetCompleted = {
                            navController.navigate(Routes.ONBOARDING) {
                                popUpTo(Routes.HOME) { inclusive = true }
                                launchSingleTop = true
                            }
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
                        onOpenCart = { id ->
                            navController.navigate(Routes.cartDetail(id)) {
                                popUpTo(Routes.CART_DETAIL) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
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

                composable(Routes.TRASH) {
                    TrashScreen(
                        vaultService = vaultService,
                        cartBackgroundStore = backgroundStore,
                        onBack = { navController.popBackStack() },
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

@Composable
private fun SplashBackground() {
    Box(
        Modifier
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
