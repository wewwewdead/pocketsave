package com.pocketsave

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.pocketsave.app.AppContainer
import com.pocketsave.app.DeepLink
import com.pocketsave.app.PocketSaveNavHost
import com.pocketsave.app.Routes
import com.pocketsave.common.ui.PocketSaveTheme
import com.pocketsave.core.currency.CurrencyFormatterProvider
import kotlinx.coroutines.runBlocking

/**
 * Hosts the Compose navigation graph and picks the start destination from the
 * persisted onboarding flag, mirroring the iOS `ContentView` branch in
 * `PocketSave/App/GrockApp.swift`.
 *
 * Also intercepts `pocketsave://…` deep-link intents (see [DeepLink]) and
 * forwards the parsed action to [AppContainer.submitDeepLink] so cart detail
 * can react on arrival.
 */
class MainActivity : ComponentActivity() {

    private val container: AppContainer by lazy {
        (application as PocketSaveApplication).container
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Match iOS `hadCompletedOnboardingAtLaunch` — one synchronous read at launch
        // before the first composition, so the initial route never flickers.
        val hasCompletedOnboarding = runBlocking {
            container.preferences.hasCompletedOnboardingNow()
        }
        val startDestination = if (hasCompletedOnboarding) Routes.HOME else Routes.ONBOARDING

        // Cold-launch deep link: stash the parsed action before Compose mounts
        // so the nav host reads a non-null pending intent on first composition.
        handleDeepLinkIntent(intent)

        setContent {
            PocketSaveTheme {
                CurrencyFormatterProvider(preferences = container.preferences) {
                    PocketSaveNavHost(
                        preferences = container.preferences,
                        vaultService = container.vaultService,
                        selectionStore = container.selectionStore,
                        backgroundStore = container.cartBackgroundStore,
                        textRecognitionService = container.textRecognitionService,
                        packagingClassifier = container.packagingClassifier,
                        appContainer = container,
                        startDestination = startDestination,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        val pending = DeepLink.parse(uri) ?: return
        container.submitDeepLink(pending)
    }
}
