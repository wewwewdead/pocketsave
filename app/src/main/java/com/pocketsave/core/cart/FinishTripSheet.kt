package com.pocketsave.core.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsave.common.ui.AppShapes
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.common.ui.components.CelebrationBurst
import com.pocketsave.common.ui.decor.UnderlineSwoosh
import com.pocketsave.common.ui.decor.grainOverlay
import com.pocketsave.core.haptics.AppHaptic
import com.pocketsave.core.haptics.rememberAppHaptics
import com.pocketsave.core.service.VaultService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Finish-trip confirmation. Shows a planned-vs-actual summary, then fires a
 * short confetti burst + Confirm haptic on completion before calling
 * [onConfirm]. The burst is rare by design — only a genuine trip completion
 * earns it, so it always feels like a small event.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishTripSheet(
    viewModel: CartDetailViewModel,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val pastels = PocketSaveTokens.pastels
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val formatter = com.pocketsave.core.currency.LocalCurrencyFormatter.current
    val scope = rememberCoroutineScope()
    val haptics = rememberAppHaptics()

    var insights by remember { mutableStateOf<VaultService.CartInsights?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var celebrationTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { insights = viewModel.loadInsights() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Confetti overlay — sits on top of the sheet so the burst reads
            // above the content. Transparent until `celebrationTrigger` flips.
            CelebrationBurst(trigger = celebrationTrigger)

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "wrap it up",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.9.sp,
                        ),
                        color = pastels.peachDeep,
                    )
                    Spacer(Modifier.width(6.dp))
                    UnderlineSwoosh(color = pastels.peachDeep.copy(alpha = 0.55f))
                }
                Text(
                    text = "Finish this trip",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = pastels.inkBerry,
                )
                Text(
                    text = "We'll tuck today's actual prices into your vault so it stays accurate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val data = insights
                if (data == null) {
                    Text(
                        text = "Gathering the numbers…",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    SummaryCard(
                        planned = data.plannedTotal,
                        actual = data.actualTotal,
                        difference = data.totalDifference,
                        formatter = formatter,
                    )
                    if (data.priceChanges.isNotEmpty()) {
                        Text(
                            text = "Price moves (${data.priceChanges.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        ) {
                            items(data.priceChanges, key = { it.itemName + it.plannedPrice + it.actualPrice }) { change ->
                                PriceChangeRow(change = change, formatter = formatter)
                                HorizontalDivider(color = pastels.hairline)
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
                    ) { Text("Not yet") }
                    Button(
                        onClick = {
                            scope.launch {
                                submitError = null
                                isSubmitting = true
                                val completed = viewModel.completeShoppingNow()
                                isSubmitting = false
                                if (completed) {
                                    haptics.perform(AppHaptic.Confirm)
                                    celebrationTrigger += 1
                                    // Hold the sheet open just long enough for
                                    // the burst to register in peripheral
                                    // vision before handing back to the caller.
                                    delay(620)
                                    onConfirm()
                                } else {
                                    submitError = "Couldn't complete trip. Try again."
                                }
                            }
                        },
                        enabled = insights != null && !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = if (isSubmitting) "Wrapping…" else "Wrap it up",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
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
    val pastels = PocketSaveTokens.pastels
    Surface(
        color = pastels.canvasTint,
        shape = AppShapes.SoftCard,
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.SoftCard)
            .grainOverlay(tint = pastels.grain, density = 0.5f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryRow(label = "Planned", value = formatter.format(planned))
            SummaryRow(label = "Actual", value = formatter.format(actual))
            HorizontalDivider(color = pastels.hairline)
            val diffLabel = when {
                difference > 0.0 -> "Over plan"
                difference < 0.0 -> "Tucked away"
                else -> "On the nose"
            }
            val diffValue = formatter.format(kotlin.math.abs(difference))
            SummaryRow(
                label = diffLabel,
                value = diffValue,
                emphasize = true,
                tint = when {
                    difference > 0.0 -> pastels.blushDeep
                    difference < 0.0 -> pastels.mintDeep
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
    val pastels = PocketSaveTokens.pastels
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = change.itemName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            )
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
            color = if (change.difference > 0.0) pastels.blushDeep else pastels.mintDeep,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}
