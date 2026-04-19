package com.pocketsave

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pocketsave.app.AppContainer
import com.pocketsave.app.DeepLink
import com.pocketsave.app.PocketSaveNavHost
import com.pocketsave.common.ui.PocketSaveTheme
import com.pocketsave.core.currency.CurrencyFormatterProvider
import com.pocketsave.core.haptics.ProvideAppHaptics

/**
 * Hosts the Compose navigation graph. The start destination is resolved
 * asynchronously inside [PocketSaveNavHost] from the persisted onboarding
 * flag, mirroring the iOS `ContentView` branch in
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
        // Android 15 (API 35) enforces edge-to-edge for targetSdk 35+; calling
        // this explicitly opts us in gracefully so Compose Material3 Scaffold
        // handles system-bar insets on all API levels.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Cold-launch deep link: stash the parsed action before Compose mounts
        // so the nav host reads a non-null pending intent on first composition.
        handleDeepLinkIntent(intent)

        setContent {
            PocketSaveTheme {
                ProvideAppHaptics {
                    CurrencyFormatterProvider(preferences = container.preferences) {
                        PocketSaveNavHost(
                            preferences = container.preferences,
                            vaultService = container.vaultService,
                            selectionStore = container.selectionStore,
                            backgroundStore = container.cartBackgroundStore,
                            textRecognitionService = container.textRecognitionService,
                            packagingClassifier = container.packagingClassifier,
                            appContainer = container,
                        )
                    }
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
