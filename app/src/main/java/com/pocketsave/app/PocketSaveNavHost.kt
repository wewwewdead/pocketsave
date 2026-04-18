package com.pocketsave.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pocketsave.core.cart.CartDetailScreen
import com.pocketsave.core.cart.VaultSelectionStore
import com.pocketsave.core.history.HistoryScreen
import com.pocketsave.core.home.HomeScreen
import com.pocketsave.core.onboarding.OnboardingContainer
import com.pocketsave.core.onboarding.OnboardingViewModel
import com.pocketsave.core.scanner.TextRecognitionService
import com.pocketsave.core.scanner.classifier.PackagingClassifier
import com.pocketsave.core.service.VaultService
import com.pocketsave.core.trash.TrashScreen
import com.pocketsave.core.tripshare.TripShareScreen
import com.pocketsave.core.vault.VaultScreen
import com.pocketsave.data.prefs.AppPreferences
import com.pocketsave.data.prefs.CartBackgroundStore

/**
 * Navigation entry — picks the start destination based on the onboarding flag,
 * mirroring the iOS `ContentView` branch in `PocketSave/App/GrockApp.swift`.
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val VAULT = "vault"
    const val CART_DETAIL = "cartDetail/{cartId}"
    const val TRIP_SHARE = "tripShare/{cartId}"
    const val HISTORY = "history"
    const val TRASH = "trash"
    fun cartDetail(cartId: String) = "cartDetail/$cartId"
    fun tripShare(cartId: String) = "tripShare/$cartId"
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
    startDestination: String,
) {
    val navController = rememberNavController()
    val pendingDeepLink by appContainer.pendingDeepLink.collectAsState()

    // Route deep links as they arrive. The detail screen reads the same
    // pending value to decide whether to auto-open the finish-trip sheet or
    // pre-stage a quick-add selection key.
    LaunchedEffect(pendingDeepLink) {
        val pending = pendingDeepLink ?: return@LaunchedEffect
        navController.navigate(Routes.cartDetail(pending.cartId)) {
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
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
                onOpenVault = { navController.navigate(Routes.VAULT) },
                onOpenCart = { cartId -> navController.navigate(Routes.cartDetail(cartId)) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenTrash = { navController.navigate(Routes.TRASH) },
                onOpenCurrencyPicker = null, // wired inline on Home via sheet state
                preferences = preferences,
            )
        }
        composable(Routes.VAULT) {
            VaultScreen(
                vaultService = vaultService,
                selectionStore = selectionStore,
                textRecognitionService = textRecognitionService,
                packagingClassifier = packagingClassifier,
                onBack = { navController.popBackStack() },
                onCreateCartRequested = { cartId ->
                    navController.navigate(Routes.cartDetail(cartId)) {
                        popUpTo(Routes.VAULT) { inclusive = true }
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
                cartId = cartId,
                onBack = { navController.popBackStack() },
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
                onBack = { navController.popBackStack() },
                onOpenCart = { id -> navController.navigate(Routes.cartDetail(id)) },
            )
        }
        composable(Routes.TRASH) {
            TrashScreen(
                vaultService = vaultService,
                cartBackgroundStore = backgroundStore,
                onBack = { navController.popBackStack() },
            )
        }
    }
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
