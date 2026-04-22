package com.pocketsave.core.currency

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.pocketsave.data.prefs.AppPreferences
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Android port of iOS `Service/CurrencyManager.swift`.
 *
 * iOS lets the user override the locale-default currency via Settings; the
 * rest of the app reads from a single formatter helper. We mirror that with:
 *
 *   - [CurrencyPreference] — the resolved (code, symbol) pair
 *   - [CurrencyFormatter]  — an immutable wrapper that renders amounts
 *   - [LocalCurrencyFormatter] — a CompositionLocal so screens don't have to
 *     thread preferences down manually
 *   - [currencyProvider]    — the bridge that reads [AppPreferences] and keeps
 *     the composition-local in sync with saved overrides
 *
 * Use [CurrencyFormatter.format] everywhere in the UI instead of
 * `NumberFormat.getCurrencyInstance(Locale.getDefault())`.
 */
@Immutable
data class CurrencyPreference(
    val code: String,
    val symbol: String,
) {
    companion object {
        fun fromLocale(locale: Locale = Locale.getDefault()): CurrencyPreference {
            val currency = runCatching { Currency.getInstance(locale) }.getOrNull()
            val code = currency?.currencyCode ?: "USD"
            val symbol = currency?.getSymbol(locale) ?: "$"
            return CurrencyPreference(code = code, symbol = symbol)
        }
    }
}

@Immutable
class CurrencyFormatter(val preference: CurrencyPreference) {
    private val formatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        runCatching { Currency.getInstance(preference.code) }.getOrNull()?.let { currency = it }
    }

    fun format(amount: Double): String = formatter.format(amount)

    fun formatOrDash(amount: Double?): String =
        if (amount == null) "—" else format(amount)
}

val LocalCurrencyFormatter = staticCompositionLocalOf {
    CurrencyFormatter(CurrencyPreference.fromLocale())
}

/**
 * Composition-local provider that watches [AppPreferences] and rebuilds the
 * formatter whenever the saved override changes. Wrap the app root (typically
 * inside the theme) with this.
 */
@Composable
fun CurrencyFormatterProvider(
    preferences: AppPreferences,
    content: @Composable () -> Unit,
) {
    val storedCode by preferences.selectedCurrencyCode.collectAsState(initial = null)
    val storedSymbol by preferences.selectedCurrencySymbol.collectAsState(initial = null)
    val formatter = remember(storedCode, storedSymbol) {
        val preference = if (storedCode != null) {
            CurrencyPreference(code = storedCode!!, symbol = storedSymbol ?: storedCode!!)
        } else {
            CurrencyPreference.fromLocale()
        }
        CurrencyFormatter(preference)
    }
    CompositionLocalProvider(LocalCurrencyFormatter provides formatter) { content() }
}

/**
 * Every ISO 4217 currency the JVM knows about, surfaced for the picker.
 *
 * Built lazily from [Currency.getAvailableCurrencies] so the list stays in
 * sync with whatever locale data is shipped with the installed Android
 * version. Display names are looked up via [Currency.getDisplayName] against
 * the user's current locale; symbol via [Currency.getSymbol].
 *
 * [deviceDefault] exposes the currency tied to the device's region (if any)
 * so the picker can pin it to the top with a "Device default" badge.
 */
object CurrencyCatalog {
    data class Option(val code: String, val symbol: String, val name: String)

    val allOptions: List<Option> by lazy { loadAllOptions(Locale.getDefault()) }

    val deviceDefault: Option? by lazy {
        val locale = Locale.getDefault()
        val currency = runCatching { Currency.getInstance(locale) }.getOrNull() ?: return@lazy null
        Option(
            code = currency.currencyCode,
            symbol = currency.getSymbol(locale) ?: currency.currencyCode,
            name = currency.getDisplayName(locale) ?: currency.currencyCode,
        )
    }

    private fun loadAllOptions(displayLocale: Locale): List<Option> =
        Currency.getAvailableCurrencies()
            .asSequence()
            .filter { it.currencyCode.length == 3 }      // drop legacy 4-letter codes
            .map { currency ->
                Option(
                    code = currency.currencyCode,
                    symbol = currency.getSymbol(displayLocale) ?: currency.currencyCode,
                    name = currency.getDisplayName(displayLocale) ?: currency.currencyCode,
                )
            }
            .distinctBy { it.code }
            .sortedBy { it.name.lowercase(displayLocale) }
            .toList()

    /** Returns `allOptions` filtered by code / name / symbol substring match. */
    fun search(query: String): List<Option> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return allOptions
        val needle = trimmed.lowercase(Locale.getDefault())
        return allOptions.filter { option ->
            option.code.lowercase(Locale.getDefault()).contains(needle) ||
                option.name.lowercase(Locale.getDefault()).contains(needle) ||
                option.symbol.lowercase(Locale.getDefault()).contains(needle)
        }
    }
}
