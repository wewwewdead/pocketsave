package com.pocketsave.core.home.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pocketsave.billing.SubscriptionManager
import com.pocketsave.common.ui.PocketSaveColors
import com.pocketsave.core.more.MoreScreen
import com.pocketsave.core.service.VaultService
import com.pocketsave.data.prefs.AppPreferences

/**
 * The content of the left-drawer side menu. iOS reference: Core/Menu/MenuView.swift.
 *
 * For now this wraps the existing `MoreScreen` composable — it contains all
 * the settings / trash / reset rows we want. Full iOS-parity styling (pastel
 * rows with emoji prefixes, dashed dividers, sticky profile header, currency
 * submenu) is a future refactor — the spec tracks this at
 * UI_REFACTOR_SPEC.md §2.2.
 *
 * @param onClose not used yet — provided so callers pass through the gesture
 *                close action when the iOS-parity version is wired up later.
 */
@Composable
fun SideMenu(
    vaultService: VaultService,
    preferences: AppPreferences,
    subscriptionManager: SubscriptionManager,
    onOpenTrash: () -> Unit,
    onOpenPaywall: () -> Unit,
    onResetApp: suspend () -> Unit,
    onResetCompleted: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(PocketSaveColors.MenuBackdropOpen),
    ) {
        MoreScreen(
            vaultService = vaultService,
            preferences = preferences,
            subscriptionManager = subscriptionManager,
            onOpenTrash = onOpenTrash,
            onOpenPaywall = onOpenPaywall,
            onResetApp = onResetApp,
            onResetCompleted = onResetCompleted,
        )
    }
}
