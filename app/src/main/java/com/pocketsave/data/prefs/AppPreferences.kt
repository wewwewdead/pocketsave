package com.pocketsave.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
 * Paywall-related keys (hasShownPostUpdatePaywall, isPro, upgrade reminder state,
 * etc.) are intentionally omitted.
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

    suspend fun hasCompletedOnboardingNow(): Boolean =
        hasCompletedOnboarding.first()

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
}
