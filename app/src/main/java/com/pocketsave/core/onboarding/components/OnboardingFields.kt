package com.pocketsave.core.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pocketsave.core.vault.icons.CategoryEmoji
import com.pocketsave.domain.model.GroceryCategory
import com.pocketsave.domain.semantics.UnitMenuCatalog
import com.pocketsave.domain.semantics.UnitMenuOption
import com.pocketsave.domain.semantics.UnitSemantics

/**
 * Reusable onboarding field primitives. Same visual language used by the Item
 * step, the Store step, and the Trip step — keeps padding, label and error
 * treatment identical across all three data screens.
 */

@Composable
fun ItemNameField(
    value: String,
    onChange: (String) -> Unit,
    duplicateError: String?,
    label: String = "Item name",
    placeholder: String = "e.g. Apples",
    imeAction: ImeAction = ImeAction.Next,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            isError = duplicateError != null,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = imeAction,
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

/**
 * Decimal input sharing the same regex + comma-to-dot normalisation used by
 * the pre-existing onboarding code, so both price and budget feel identical.
 */
@Composable
fun PriceField(
    value: String,
    onChange: (String) -> Unit,
    label: String = "Price",
    placeholder: String = "0.00",
    imeAction: ImeAction = ImeAction.Next,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.isEmpty() || input.matches(Regex("^\\d*[.,]?\\d{0,2}$"))) {
                onChange(input.replace(',', '.'))
            }
        },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = imeAction,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPicker(
    selected: String?,
    onSelect: (GroceryCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategory = selected?.let { GroceryCategory.fromTitle(it) }
        ?.takeIf { it.title == selected }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            placeholder = { Text("Select a category") },
            leadingIcon = selectedCategory?.let { category ->
                {
                    Text(
                        text = CategoryEmoji.resolve(category.defaultIconKey),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            },
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
                        Text(
                            text = CategoryEmoji.resolve(category.defaultIconKey),
                            style = MaterialTheme.typography.titleMedium,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitPicker(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val options: List<UnitMenuOption> =
        UnitMenuCatalog.continuousOptions + UnitMenuCatalog.discreteOptions
    val displayLabel = options.firstOrNull { it.abbr == selected }?.displayText ?: selected

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
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
fun PackageSizeFields(
    soldAsUnit: String,
    value: String,
    unit: String,
    onValueChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
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
