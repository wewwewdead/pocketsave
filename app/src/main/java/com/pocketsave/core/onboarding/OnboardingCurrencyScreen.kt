package com.pocketsave.core.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketsave.core.currency.CurrencyCatalog
import com.pocketsave.core.onboarding.motion.OnboardingScaffold
import com.pocketsave.core.onboarding.motion.OnboardingSection

/**
 * Full-screen currency picker for onboarding. Pre-selects the device default on
 * first entry so Continue is immediately enabled; user can search or scroll to
 * pick a different one. Writes to [com.pocketsave.data.prefs.AppPreferences]
 * only when the user confirms with Continue — backing up and changing it
 * costs nothing.
 */
@Composable
fun OnboardingCurrencyScreen(viewModel: OnboardingViewModel) {
    val deviceDefault = remember { CurrencyCatalog.deviceDefault }

    // Pre-select the device default the first time the screen appears, so the
    // Continue button is ready without forcing a tap on an already-correct row.
    LaunchedEffect(Unit) {
        if (viewModel.currencyCode == null && deviceDefault != null) {
            viewModel.selectCurrency(deviceDefault.code, deviceDefault.symbol)
        }
    }

    var query by remember { mutableStateOf("") }
    val filtered by remember(query) {
        derivedStateOf { CurrencyCatalog.search(query) }
    }

    OnboardingScaffold(
        progress = viewModel.progressForStep,
        onBack = { viewModel.navigateBack() },
        primaryCta = {
            Button(
                onClick = { viewModel.commitCurrencyAndContinue() },
                enabled = viewModel.currencyCode != null,
            ) {
                Text("Continue", modifier = Modifier.padding(horizontal = 8.dp))
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OnboardingSection(delayMs = 40) {
                Text(
                    text = "Pick your currency",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
            Spacer(Modifier.height(6.dp))
            OnboardingSection(delayMs = 140) {
                Text(
                    text = "So prices and budgets look right everywhere.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    ),
                )
            }

            Spacer(Modifier.height(16.dp))

            OnboardingSection(delayMs = 220) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search currency…") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
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
            }

            Spacer(Modifier.height(12.dp))

            // LazyColumn lives as a direct weighted child of the body Column so
            // it gets a bounded max height. Wrapping it in a staggered-reveal
            // container would pile two layout contexts on a scroll container.
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
            ) {
                if (query.isBlank() && deviceDefault != null) {
                    item(key = "device-default") {
                        CurrencyRow(
                            option = deviceDefault,
                            selected = viewModel.currencyCode == deviceDefault.code,
                            showDeviceBadge = true,
                            onClick = {
                                viewModel.selectCurrency(deviceDefault.code, deviceDefault.symbol)
                            },
                        )
                    }
                }

                items(filtered, key = { it.code }) { option ->
                    if (query.isBlank() && option.code == deviceDefault?.code) return@items
                    CurrencyRow(
                        option = option,
                        selected = viewModel.currencyCode == option.code,
                        showDeviceBadge = false,
                        onClick = {
                            viewModel.selectCurrency(option.code, option.symbol)
                        },
                    )
                }

                if (filtered.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = "No matches for \"${query.trim()}\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 12.dp),
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
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = option.symbol,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                ),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = option.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
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
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
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
