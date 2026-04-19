package com.pocketsave.data.prefs

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pocketsave_prefs")

/**
 * DataStore-backed replacement for the iOS `UserDefaults` keys that survive the
 * monetization-free port: onboarding completion, user name, currency selection,
 * and the custom-units catalog.
 *
 * The [isProCached] key is the one monetization-adjacent field that does live
 * here — it is **not** the source of truth (RevenueCat is) but a local mirror
 * of the last-known entitlement so the first frame on cold launch can make
 * a correct free-vs-Pro decision without waiting for the SDK. See
 * `billing/RevenueCatSubscriptionManager` for the write path.
 */
class AppPreferences(private val context: Context) {

    private object Keys {
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val USER_NAME = stringPreferencesKey("user_name")
        val CURRENCY_CODE = stringPreferencesKey("selected_currency_code")
        val CURRENCY_SYMBOL = stringPreferencesKey("selected_currency_symbol")
        val CUSTOM_UNITS_JSON = stringPreferencesKey("custom_units_json")
        val VISIBLE_CATEGORIES_JSON = stringPreferencesKey("visible_categories_json")
        val SHOULD_SHOW_FIRST_RUN_HINTS = booleanPreferencesKey("should_show_first_run_hints")
        val IS_PRO_CACHED = booleanPreferencesKey("is_pro_cached")
        val MONTHLY_BUDGET = doublePreferencesKey("monthly_budget")
    }

    val hasCompletedOnboarding: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.HAS_COMPLETED_ONBOARDING] ?: false }

    val userName: Flow<String?> =
        context.dataStore.data.map { it[Keys.USER_NAME] }

    val selectedCurrencyCode: Flow<String?> =
        context.dataStore.data.map { it[Keys.CURRENCY_CODE] }

    val selectedCurrencySymbol: Flow<String?> =
        context.dataStore.data.map { it[Keys.CURRENCY_SYMBOL] }

    val customUnitsJson: Flow<String> =
        context.dataStore.data.map { it[Keys.CUSTOM_UNITS_JSON] ?: "[]" }

    val visibleCategoriesJson: Flow<String?> =
        context.dataStore.data.map { it[Keys.VISIBLE_CATEGORIES_JSON] }

    /**
     * One-shot flag raised when onboarding finishes, cleared as soon as the
     * Home first-run hints are dismissed. Home observes this so it can render
     * light coach marks on the newly-created trip the first time Home mounts.
     */
    val shouldShowFirstRunHints: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.SHOULD_SHOW_FIRST_RUN_HINTS] ?: false }

    /**
     * Last-known Pro entitlement verdict, mirrored from RevenueCat's
     * `CustomerInfo`. Used only as a first-frame hint on cold launch — the
     * live verdict always comes from `SubscriptionManager.isPro`. Defaults
     * to `false` so first-time users never start in a Pro-assumed state.
     */
    val isProCached: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.IS_PRO_CACHED] ?: false }

    /**
     * Optional monthly spending ceiling in the user's selected currency. `0.0`
     * means "not set" — the trip-creation flow short-circuits the warning
     * dialog when the budget is unset so first-run users never see it.
     */
    val monthlyBudget: Flow<Double> =
        context.dataStore.data.map { it[Keys.MONTHLY_BUDGET] ?: 0.0 }

    suspend fun hasCompletedOnboardingNow(): Boolean =
        hasCompletedOnboarding.first()

    suspend fun isProCachedNow(): Boolean = isProCached.first()

    suspend fun setIsProCached(value: Boolean) {
        context.dataStore.edit { it[Keys.IS_PRO_CACHED] = value }
    }

    suspend fun setHasCompletedOnboarding(value: Boolean) {
        context.dataStore.edit { it[Keys.HAS_COMPLETED_ONBOARDING] = value }
    }

    suspend fun setUserName(value: String) {
        context.dataStore.edit { it[Keys.USER_NAME] = value }
    }

    suspend fun setCurrency(code: String, symbol: String) {
        context.dataStore.edit {
            it[Keys.CURRENCY_CODE] = code
            it[Keys.CURRENCY_SYMBOL] = symbol
        }
    }

    suspend fun setCustomUnitsJson(value: String) {
        context.dataStore.edit { it[Keys.CUSTOM_UNITS_JSON] = value }
    }

    suspend fun setVisibleCategoriesJson(value: String?) {
        context.dataStore.edit {
            if (value == null) it.remove(Keys.VISIBLE_CATEGORIES_JSON) else it[Keys.VISIBLE_CATEGORIES_JSON] = value
        }
    }

    suspend fun setShouldShowFirstRunHints(value: Boolean) {
        context.dataStore.edit { it[Keys.SHOULD_SHOW_FIRST_RUN_HINTS] = value }
    }

    suspend fun setMonthlyBudget(value: Double) {
        context.dataStore.edit {
            if (value <= 0.0) it.remove(Keys.MONTHLY_BUDGET) else it[Keys.MONTHLY_BUDGET] = value
        }
    }

    /**
     * Wipes every stored preference. Used by the Reset-App flow in settings so
     * the next launch routes through onboarding and all persisted choices
     * (name, currency, visible categories, custom units, first-run hints) go
     * back to defaults.
     */
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}

/**
 * Composition-local handle for [AppPreferences]. Provided once at the nav-host
 * root so deep composables (sheets, dialogs) can read prefs without every
 * intermediate screen needing a parameter for it. Defaults to throwing because
 * an unprovided prefs handle is always a wiring bug, not a survivable state.
 */
val LocalAppPreferences = staticCompositionLocalOf<AppPreferences> {
    error("LocalAppPreferences not provided — wrap the composition in a CompositionLocalProvider at the nav root.")
}
