package com.pocketsave.core.currency

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketsave.data.prefs.AppPreferences
import kotlinx.coroutines.launch

/**
 * Port of the iOS currency picker surfaced from `Service/CurrencyManager.swift`.
 *
 * Shows every ISO 4217 currency the JVM knows about (~180 entries), with:
 *  - a search box that filters by code, display name, or symbol;
 *  - the device-region currency pinned to the top with a "Device default" chip
 *    so it's always one tap away regardless of the stored override.
 *
 * The user's choice persists via [AppPreferences.setCurrency]. Screens read
 * through [LocalCurrencyFormatter] and rerender when the preference changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerSheet(
    preferences: AppPreferences,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val currentCode by preferences.selectedCurrencyCode.collectAsState(initial = null)

    var query by remember { mutableStateOf("") }
    val filtered by remember(query) {
        derivedStateOf { CurrencyCatalog.search(query) }
    }
    val deviceDefault = CurrencyCatalog.deviceDefault

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Currency", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Amounts across the app format with this currency's symbol.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search currency…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            TextButton(
                onClick = {
                    scope.launch {
                        val fallback = CurrencyPreference.fromLocale()
                        preferences.setCurrency(fallback.code, fallback.symbol)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Use device default") }

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
            ) {
                // Pin the device-region currency to the top when it exists and
                // the user hasn't typed a search yet (a typed search should
                // show matches in their canonical sorted order).
                if (query.isBlank() && deviceDefault != null) {
                    item(key = "device-default") {
                        CurrencyRow(
                            option = deviceDefault,
                            selected = currentCode == deviceDefault.code,
                            showDeviceBadge = true,
                            onClick = {
                                scope.launch {
                                    preferences.setCurrency(deviceDefault.code, deviceDefault.symbol)
                                    onDismiss()
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }

                items(filtered, key = { it.code }) { option ->
                    // Skip the device default when it's already shown up top.
                    if (query.isBlank() && option.code == deviceDefault?.code) return@items
                    CurrencyRow(
                        option = option,
                        selected = currentCode == option.code,
                        showDeviceBadge = false,
                        onClick = {
                            scope.launch {
                                preferences.setCurrency(option.code, option.symbol)
                                onDismiss()
                            }
                        },
                    )
                    HorizontalDivider()
                }

                if (filtered.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = "No matches for \"${query.trim()}\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyRow(
    option: CurrencyCatalog.Option,
    selected: Boolean,
    showDeviceBadge: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${option.symbol}  ${option.name}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (showDeviceBadge) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Text(
                            text = "Device default",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                text = option.code,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
