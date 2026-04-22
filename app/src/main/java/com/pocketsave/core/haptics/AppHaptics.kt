package com.pocketsave.core.haptics

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/**
 * A deliberately tiny haptic vocabulary. Every touch-level buzz the app
 * produces has to map to one of these four intents — which forces every
 * callsite to justify *why* a haptic is there before firing one. Adding
 * more types makes the feedback louder, not better.
 *
 *  - [Light]      — tiny state flip (item bought/skipped, subtle tick)
 *  - [Confirm]    — a successful, earned action the user will feel good about
 *  - [Reject]     — a blocked or destructive confirmation — calm, not punishing
 *  - [LongPress]  — entering an interaction / edit / drag mode
 */
@Immutable
sealed interface AppHaptic {
    data object Light : AppHaptic
    data object Confirm : AppHaptic
    data object Reject : AppHaptic
    data object LongPress : AppHaptic
}

/**
 * Platform-agnostic handle for firing haptics. Tests / previews can swap in
 * [NoOpAppHaptics] so the UI code doesn't have to know anything about
 * Android's [View] at callsites.
 */
@Immutable
fun interface AppHaptics {
    fun perform(haptic: AppHaptic)
}

/** Null implementation for previews, tests, and anywhere a view isn't available. */
val NoOpAppHaptics: AppHaptics = AppHaptics { /* no-op */ }

/**
 * Real implementation backed by [View.performHapticFeedback]. Using the
 * view's haptic path means the OS's haptic-feedback user setting is
 * respected automatically — we never force a buzz on someone who has
 * haptics turned off.
 *
 * API ≥ 30 gets the modern CONFIRM / REJECT constants which have proper
 * two-part waveforms on capable hardware; older API levels fall back to
 * LONG_PRESS / VIRTUAL_KEY which are the closest rough matches.
 */
internal class ComposeAppHaptics(private val view: View) : AppHaptics {
    override fun perform(haptic: AppHaptic) {
        val constant = when (haptic) {
            // Firm quick tick — noticeably stronger than KEYBOARD_TAP, which
            // is nearly imperceptible on most phones. VIRTUAL_KEY gets you a
            // real "I felt that" pulse on almost every device.
            AppHaptic.Light -> HapticFeedbackConstants.VIRTUAL_KEY

            // API 30+ has proper dual-pulse CONFIRM / REJECT waveforms on
            // hardware that supports them. On older devices we fall back to
            // LONG_PRESS because VIRTUAL_KEY is often too subtle for a
            // milestone-like success confirmation.
            AppHaptic.Confirm -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }

            AppHaptic.Reject -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }

            AppHaptic.LongPress -> HapticFeedbackConstants.LONG_PRESS
        }
        // `performHapticFeedback` returns false when the view or the OS
        // setting has haptics disabled. We intentionally don't retry with
        // FLAG_IGNORE_VIEW_SETTING — respecting the device setting is part
        // of the contract of a "premium, calm" app.
        view.performHapticFeedback(constant)
    }
}

/**
 * CompositionLocal seed. Every [PocketSaveTheme]-wrapped tree can read a
 * real [AppHaptics] via [rememberAppHaptics] without threading it through
 * every screen signature.
 */
val LocalAppHaptics = staticCompositionLocalOf<AppHaptics> { NoOpAppHaptics }

/**
 * Provides a view-backed [AppHaptics] to the composition subtree. Place at
 * or near the theme root so all screens can resolve haptics without each
 * one grabbing [LocalView] on its own.
 */
@Composable
fun ProvideAppHaptics(content: @Composable () -> Unit) {
    val view = LocalView.current
    val haptics = remember(view) { ComposeAppHaptics(view) }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalAppHaptics provides haptics,
        content = content,
    )
}

/** Convenience accessor — shorter than reading [LocalAppHaptics] everywhere. */
@Composable
@ReadOnlyComposable
fun rememberAppHaptics(): AppHaptics = LocalAppHaptics.current
