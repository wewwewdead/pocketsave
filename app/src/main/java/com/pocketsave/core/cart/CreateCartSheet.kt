package com.pocketsave.core.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pocketsave.core.currency.LocalCurrencyFormatter
import com.pocketsave.core.haptics.AppHaptic
import com.pocketsave.core.haptics.rememberAppHaptics
import com.pocketsave.core.service.VaultService
import com.pocketsave.data.prefs.LocalAppPreferences
import com.pocketsave.domain.model.CartStatus
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Port of the iOS "create cart" confirmation surfaced from
 * `Core/Detail Cart/Views/CartConfirmationPopover.swift`. Ports only the
 * name + budget inputs; the Pro-tier wording and active-cart-limit sheets are
 * omitted. On submit the Android flow calls `createCartWithActiveItems` so the
 * user's pending `VaultSelectionStore` entries land as planned cart items.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCartSheet(
    vaultService: VaultService,
    selectionStore: VaultSelectionStore,
    selectedItemCount: Int,
    onDismiss: () -> Unit,
    onCreated: (cartId: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptics = rememberAppHaptics()
    val appPreferences = LocalAppPreferences.current
    val formatter = LocalCurrencyFormatter.current
    val monthlyBudget by appPreferences.monthlyBudget.collectAsState(initial = 0.0)
    val snapshot by vaultService.state.collectAsState()
    var name by remember { mutableStateOf("") }
    var budgetText by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var pendingOverBudget by remember { mutableStateOf<OverBudgetWarning?>(null) }

    val trimmedName = name.trim()
    val budget = budgetText.toDoubleOrNull()

    // Sum of cart budgets for non-deleted carts created this month, treating
    // each cart's `budget` field as the projected spend. Completed trips that
    // have no budget set still contribute zero — we don't fall back to actual
    // because the user's mental model is "the number I planned to spend".
    val monthlySoFar = remember(snapshot.carts) { sumCartBudgetsThisMonth(snapshot.carts) }

    val attemptCreate: () -> Unit = submit@{
        if (trimmedName.isEmpty()) {
            nameError = "Cart name is required"
            haptics.perform(AppHaptic.Reject)
            return@submit
        }
        val newBudget = budget ?: 0.0
        val projected = monthlySoFar + newBudget
        if (monthlyBudget > 0.0 && newBudget > 0.0 && projected > monthlyBudget) {
            // Reject buzz to mark the surface as "blocked" — the dialog
            // still lets the user override and proceed.
            haptics.perform(AppHaptic.Reject)
            pendingOverBudget = OverBudgetWarning(
                monthlyBudget = monthlyBudget,
                monthlySoFar = monthlySoFar,
                newBudget = newBudget,
            )
            return@submit
        }
        scope.launch {
            isSubmitting = true
            val cart = vaultService.createCartWithActiveItems(
                name = trimmedName,
                budget = newBudget,
                activeItems = selectionStore.activeCartItems.value,
            )
            isSubmitting = false
            if (cart != null) {
                haptics.perform(AppHaptic.Confirm)
                selectionStore.clearAll()
                onCreated(cart.id)
            } else {
                haptics.perform(AppHaptic.Reject)
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("New cart", style = MaterialTheme.typography.titleLarge)
            Text(
                text = if (selectedItemCount > 0)
                    "$selectedItemCount item${if (selectedItemCount == 1) "" else "s"} will be added."
                else "Start with an empty cart — you can add items later.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = null },
                label = { Text("Cart name") },
                singleLine = true,
                isError = nameError != null,
                supportingText = { if (nameError != null) Text(nameError!!) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = budgetText,
                onValueChange = { raw ->
                    if (raw.isEmpty() || raw.matches(Regex("^\\d*[.,]?\\d{0,2}$"))) {
                        budgetText = raw.replace(',', '.')
                    }
                },
                label = { Text("Budget (optional)") },
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = attemptCreate,
                    enabled = trimmedName.isNotEmpty() && !isSubmitting,
                    modifier = Modifier.weight(1f),
                ) { Text("Create cart") }
            }
        }
    }

    val warning = pendingOverBudget
    if (warning != null) {
        val projected = warning.monthlySoFar + warning.newBudget
        AlertDialog(
            onDismissRequest = { pendingOverBudget = null },
            title = {
                Text(
                    text = "Out of monthly budget",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            },
            text = {
                Text(
                    text = buildString {
                        append("This trip's budget of ")
                        append(formatter.format(warning.newBudget))
                        append(" would push this month to ")
                        append(formatter.format(projected))
                        append(", over your ceiling of ")
                        append(formatter.format(warning.monthlyBudget))
                        append(" (")
                        append(formatter.format(warning.monthlySoFar))
                        append(" planned so far). Create it anyway?")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val approved = warning
                    pendingOverBudget = null
                    scope.launch {
                        isSubmitting = true
                        val cart = vaultService.createCartWithActiveItems(
                            name = trimmedName,
                            budget = approved.newBudget,
                            activeItems = selectionStore.activeCartItems.value,
                        )
                        isSubmitting = false
                        if (cart != null) {
                            haptics.perform(AppHaptic.Confirm)
                            selectionStore.clearAll()
                            onCreated(cart.id)
                        } else {
                            haptics.perform(AppHaptic.Reject)
                        }
                    }
                }) {
                    Text(
                        text = "Create anyway",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingOverBudget = null }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

private data class OverBudgetWarning(
    val monthlyBudget: Double,
    val monthlySoFar: Double,
    val newBudget: Double,
)

private fun sumCartBudgetsThisMonth(
    carts: List<com.pocketsave.data.local.entity.CartEntity>,
): Double {
    val cal = Calendar.getInstance()
    val nowYear = cal.get(Calendar.YEAR)
    val nowMonth = cal.get(Calendar.MONTH)
    var total = 0.0
    for (cart in carts) {
        if (cart.isDeleted) continue
        // Completed trips already past don't change the user's planning room
        // for *new* trips this month — but a finished trip in the same month
        // very much does still count against the ceiling.
        cal.time = cart.createdAt
        if (cal.get(Calendar.YEAR) != nowYear || cal.get(Calendar.MONTH) != nowMonth) continue
        // Skip silly statuses and cart budgets <= 0 (unset).
        if (CartStatus.fromRaw(cart.status) == CartStatus.PLANNING && cart.budget <= 0.0) continue
        total += cart.budget
    }
    return total
}
