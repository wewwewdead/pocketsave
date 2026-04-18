package com.pocketsave.core.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketsave.core.service.VaultService
import kotlinx.coroutines.launch

/**
 * Port of `PocketSave/Core/Detail Cart/Shopping Cart/Finish Trip/FinishTripSheet.swift`.
 *
 * Shows a planned-vs-actual summary derived from `VaultService.getCartInsights`,
 * then calls `completeShopping` on confirm. The iOS sheet also includes the
 * rating prompt and Pro celebration hooks — those are intentionally omitted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishTripSheet(
    viewModel: CartDetailViewModel,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val formatter = com.pocketsave.core.currency.LocalCurrencyFormatter.current
    val scope = rememberCoroutineScope()

    var insights by remember { mutableStateOf<VaultService.CartInsights?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { insights = viewModel.loadInsights() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Finish trip", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Saving the actuals updates the vault with the latest store prices for the items you marked fulfilled.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val data = insights
            if (data == null) {
                Text("Computing insights...")
            } else {
                SummaryCard(
                    planned = data.plannedTotal,
                    actual = data.actualTotal,
                    difference = data.totalDifference,
                    formatter = formatter,
                )
                if (data.priceChanges.isNotEmpty()) {
                    Text(
                        text = "Price changes (${data.priceChanges.size})",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                    ) {
                        items(data.priceChanges, key = { it.itemName + it.plannedPrice + it.actualPrice }) { change ->
                            PriceChangeRow(change = change, formatter = formatter)
                            HorizontalDivider()
                        }
                    }
                }
            }

            if (submitError != null) {
                Text(
                    text = submitError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    modifier = Modifier.weight(1f),
                ) { Text("Cancel") }
                Button(
                    onClick = {
                        scope.launch {
                            submitError = null
                            isSubmitting = true
                            val completed = viewModel.completeShoppingNow()
                            isSubmitting = false
                            if (completed) {
                                onConfirm()
                            } else {
                                submitError = "Couldn't complete trip. Try again."
                            }
                        }
                    },
                    enabled = insights != null && !isSubmitting,
                    modifier = Modifier.weight(1f),
                ) { Text(if (isSubmitting) "Completing..." else "Complete trip") }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    planned: Double,
    actual: Double,
    difference: Double,
    formatter: com.pocketsave.core.currency.CurrencyFormatter,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SummaryRow(label = "Planned total", value = formatter.format(planned))
            SummaryRow(label = "Actual total", value = formatter.format(actual))
            HorizontalDivider()
            val diffLabel = when {
                difference > 0.0 -> "Overspent"
                difference < 0.0 -> "Saved"
                else -> "No change"
            }
            val diffValue = formatter.format(kotlin.math.abs(difference))
            SummaryRow(
                label = diffLabel,
                value = diffValue,
                emphasize = true,
                tint = when {
                    difference > 0.0 -> MaterialTheme.colorScheme.error
                    difference < 0.0 -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    emphasize: Boolean = false,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal,
            color = tint,
        )
    }
}

@Composable
private fun PriceChangeRow(
    change: VaultService.PriceChange,
    formatter: com.pocketsave.core.currency.CurrencyFormatter,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(change.itemName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${formatter.format(change.plannedPrice)} → ${formatter.format(change.actualPrice)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = if (change.difference >= 0)
                "+${formatter.format(change.difference)}"
            else
                "−${formatter.format(kotlin.math.abs(change.difference))}",
            color = if (change.difference > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
