package com.pocketsave.core.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pocketsave.domain.model.GroceryCategory
import com.pocketsave.domain.semantics.UnitMenuCatalog
import com.pocketsave.domain.semantics.UnitMenuOption
import com.pocketsave.domain.semantics.UnitSemantics
import kotlinx.coroutines.flow.drop

/**
 * Port of `OnboardingFirstItemView` + its `FirstItemForm`. Provides the minimal
 * inputs needed to persist the first vault item: name, category, price, unit,
 * and (for non-continuous units) package size.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingFirstItemScreen(viewModel: OnboardingViewModel) {
    val form = viewModel.formViewModel

    // Debounced duplicate check whenever the item name changes (mirrors iOS
    // `.onChange(of: formViewModel.itemName)`). Drop(1) so we ignore the initial
    // empty-state emission that Compose fires when the snapshotFlow starts.
    LaunchedEffect(Unit) {
        snapshotFlow { form.itemName }.drop(1).collect { name ->
            if (viewModel.duplicateError != null) viewModel.clearDuplicateError()
            viewModel.checkForDuplicateItemName(name)
        }
    }

    // Match iOS default-unit promotion from "" → "g" on first appear.
    LaunchedEffect(Unit) {
        if (form.unit.isEmpty()) form.unit = "g"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Back header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = viewModel::navigateBack) { Text("Back") }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = viewModel.questionText,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            ItemNameField(
                value = form.itemName,
                onChange = { form.itemName = it },
                duplicateError = viewModel.duplicateError,
            )

            CategoryPicker(
                selected = form.selectedCategoryName,
                onSelect = { form.selectedCategoryName = it.title },
            )

            PriceField(
                value = form.itemPrice,
                onChange = { form.itemPrice = it },
            )

            UnitPicker(
                selected = form.unit,
                onSelect = { canonical ->
                    form.unit = canonical
                    form.resetPackageSize(soldAsUnit = canonical)
                },
            )

            if (!UnitSemantics.isContinuous(form.unit)) {
                PackageSizeFields(
                    soldAsUnit = form.unit,
                    value = form.packageSizeValue,
                    unit = form.packageSizeUnit,
                    onValueChange = { form.packageSizeValue = it },
                    onUnitChange = { form.packageSizeUnit = it },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        BottomBar(
            total = viewModel.calculatedTotal,
            enabled = viewModel.isFormValidForCompletion,
            onFinish = {
                if (form.attemptSubmission()) viewModel.finishOnboarding()
            },
        )
    }
}

@Composable
private fun ItemNameField(
    value: String,
    onChange: (String) -> Unit,
    duplicateError: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text("Item name") },
            placeholder = { Text("e.g. Apples") },
            singleLine = true,
            isError = duplicateError != null,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (duplicateError != null) {
            Text(
                text = duplicateError,
                color = Color(0xFFFF6F71),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(
    selected: String?,
    onSelect: (GroceryCategory) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            placeholder = { Text("Select a category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            GroceryCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.title) },
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            imageVector = com.pocketsave.core.vault.icons.AppIcon.resolveIcon(category.defaultIconKey),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        onSelect(category)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PriceField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            // Allow digits and a single decimal separator, matching the numeric
            // keyboard's permissive semantics on iOS.
            if (input.isEmpty() || input.matches(Regex("^\\d*[.,]?\\d{0,2}$"))) {
                onChange(input.replace(',', '.'))
            }
        },
        label = { Text("Price") },
        placeholder = { Text("0.00") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitPicker(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options: List<UnitMenuOption> = UnitMenuCatalog.continuousOptions + UnitMenuCatalog.discreteOptions
    val displayLabel = options.firstOrNull { it.abbr == selected }?.displayText ?: selected

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = displayLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Unit") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayText) },
                    onClick = {
                        onSelect(UnitSemantics.canonicalUnit(option.abbr))
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackageSizeFields(
    soldAsUnit: String,
    value: String,
    unit: String,
    onValueChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                if (input.isEmpty() || input.matches(Regex("^\\d*[.,]?\\d*$"))) {
                    onValueChange(input.replace(',', '.'))
                }
            },
            label = { Text("Package size") },
            placeholder = { Text("0") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )

        var expanded by remember { mutableStateOf(false) }
        val options = UnitSemantics.packageSizeUnitOptions(soldAsUnit)

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.width(140.dp),
        ) {
            OutlinedTextField(
                value = unit,
                onValueChange = {},
                readOnly = true,
                label = { Text("Package unit") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onUnitChange(UnitSemantics.canonicalUnit(option))
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBar(total: Double, enabled: Boolean, onFinish: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Total: %.2f".format(total),
            style = MaterialTheme.typography.titleMedium,
        )
        Button(onClick = onFinish, enabled = enabled) { Text("Finish") }
    }
}
