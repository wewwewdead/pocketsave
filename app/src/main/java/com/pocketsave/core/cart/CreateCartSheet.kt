package com.pocketsave.core.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pocketsave.core.service.VaultService
import kotlinx.coroutines.launch

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
    var name by remember { mutableStateOf("") }
    var budgetText by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val trimmedName = name.trim()
    val budget = budgetText.toDoubleOrNull()

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
                    onClick = {
                        if (trimmedName.isEmpty()) {
                            nameError = "Cart name is required"
                            return@Button
                        }
                        scope.launch {
                            isSubmitting = true
                            val cart = vaultService.createCartWithActiveItems(
                                name = trimmedName,
                                budget = budget ?: 0.0,
                                activeItems = selectionStore.activeCartItems.value,
                            )
                            isSubmitting = false
                            if (cart != null) {
                                selectionStore.clearAll()
                                onCreated(cart.id)
                            }
                        }
                    },
                    enabled = trimmedName.isNotEmpty() && !isSubmitting,
                    modifier = Modifier.weight(1f),
                ) { Text("Create cart") }
            }
        }
    }
}
