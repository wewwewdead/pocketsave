package com.pocketsave.core.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.pocketsave.common.util.ColorOption
import com.pocketsave.core.paywall.ProChip
import com.pocketsave.core.service.VaultService
import com.pocketsave.core.vault.icons.AppIcon
import com.pocketsave.core.vault.picker.ColorSwatchAndHsvPicker
import com.pocketsave.core.vault.picker.IconGridPicker
import com.pocketsave.data.local.entity.CategoryEntity
import com.pocketsave.domain.model.GroceryCategory
import kotlinx.coroutines.launch

/**
 * Port of `PocketSave/Core/Vault/Views/CategoriesManager/CategoriesManagerSheet.swift`
 * (with `CreateCategorySheet` + `EditCategorySheet` rolled in). The full iOS
 * sheet also exposes visible-category toggling and drag-to-reorder; Phase 4
 * ships the CRUD surface and leaves reordering / hidden-tab behaviour for the
 * Menu phase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesManagerSheet(
    vaultService: VaultService,
    onDismiss: () -> Unit,
    /**
     * Invoked when the user taps **New** to create a custom category. The
     * sheet forwards a callback that actually opens the create form; the
     * caller either runs it or shows the paywall. Defaults to "always
     * allow" so the sheet remains previewable without billing wiring.
     */
    onAddCategoryRequested: (onAllowed: () -> Unit) -> Unit = { it() },
    /**
     * Free-tier custom-category cap. Null means "unlimited" (Pro users) —
     * in that case no status line or Pro badge is drawn. When non-null, the
     * sheet renders `N of M custom categories` and a Pro pill on the New
     * button once the user is at the cap.
     */
    customCategoryCap: Int? = null,
) {
    val state by vaultService.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var showCreateSheet by remember { mutableStateOf(false) }

    val defaultTitles = remember { GroceryCategory.entries.map { it.title }.toSet() }
    val ordered = state.categories.sortedBy { it.sortOrder }
    val defaults = ordered.filter { it.name in defaultTitles }
    val custom = ordered.filter { it.name !in defaultTitles }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val customCount = custom.size
            val atCap = customCategoryCap != null && customCount >= customCategoryCap

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Manage categories", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Button(onClick = { onAddCategoryRequested { showCreateSheet = true } }) {
                    Text("New")
                    if (atCap) {
                        Spacer(Modifier.width(6.dp))
                        ProChip()
                    }
                }
            }
            if (customCategoryCap != null) {
                // Only surfaces a status line once the free user is actually
                // at the cap — under the limit the happy path stays silent.
                if (atCap) {
                    Text(
                        text = "$customCount of $customCategoryCap custom categories. Upgrade for unlimited.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                if (custom.isNotEmpty()) {
                    item { SectionLabel("Custom") }
                    items(custom, key = { it.uid }) { category ->
                        CategoryRow(
                            category = category,
                            iconKey = vaultService.displayIconKeyForCategory(category),
                            deletable = true,
                            onEdit = { editingCategory = category },
                            onDelete = { categoryToDelete = category },
                        )
                    }
                }
                item { SectionLabel("Default") }
                items(defaults, key = { it.uid }) { category ->
                    CategoryRow(
                        category = category,
                        iconKey = vaultService.displayIconKeyForCategory(category),
                        deletable = false,
                        onEdit = null,
                        onDelete = null,
                    )
                }
            }
        }
    }

    if (showCreateSheet) {
        CategoryFormSheet(
            initial = null,
            takenNames = ordered.map { it.name },
            onDismiss = { showCreateSheet = false },
            onSubmit = { name, iconKey, color ->
                scope.launch {
                    vaultService.createCustomCategory(name = name, iconKey = iconKey, colorHex = color)
                    showCreateSheet = false
                }
            },
        )
    }

    editingCategory?.let { cat ->
        CategoryFormSheet(
            initial = cat,
            takenNames = ordered.filter { it.uid != cat.uid }.map { it.name },
            onDismiss = { editingCategory = null },
            onSubmit = { name, iconKey, color ->
                scope.launch {
                    vaultService.updateCustomCategory(
                        originalName = cat.name,
                        newName = name,
                        iconKey = iconKey,
                        colorHex = color,
                    )
                    editingCategory = null
                }
            },
        )
    }

    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        vaultService.deleteCustomCategory(cat.name)
                        categoryToDelete = null
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) { Text("Cancel") }
            },
            title = { Text("Delete category?") },
            text = { Text("Items in ${cat.name} will move to trash.") },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun CategoryRow(
    category: CategoryEntity,
    iconKey: String,
    deletable: Boolean,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            com.pocketsave.core.vault.icons.CategoryEmojiTile(
                iconKey = iconKey,
                colorHex = category.colorHex,
                size = 44.dp,
                cornerRadius = 12.dp,
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = category.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit ${category.name}")
                }
            }
            if (deletable && onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete ${category.name}",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        HorizontalDivider()
    }
}

/**
 * Create / edit dialog. An [AlertDialog] wrapper keeps the form inside the
 * existing manager sheet without stacking a second bottom sheet. Three
 * sections: name, icon grid, colour picker (swatches + optional HSV sliders).
 */
@Composable
private fun CategoryFormSheet(
    initial: CategoryEntity?,
    takenNames: List<String>,
    onDismiss: () -> Unit,
    onSubmit: (name: String, iconKey: String?, color: String?) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var iconKey by remember {
        mutableStateOf(
            initial?.iconKey ?: initial?.name?.let { title ->
                GroceryCategory.entries
                    .firstOrNull { it.title.equals(title, ignoreCase = true) }
                    ?.defaultIconKey
            },
        )
    }
    var colorHex by remember { mutableStateOf(initial?.colorHex) }

    val trimmedName = name.trim()
    val takenLower = takenNames.map { it.trim().lowercase() }.toSet()
    val nameError = when {
        trimmedName.isEmpty() -> null
        trimmedName.lowercase() in takenLower -> "A category with that name already exists"
        else -> null
    }
    val canSubmit = trimmedName.isNotEmpty() && nameError == null

    // Tint for the icon grid preview: the currently-selected colour, or the
    // primary theme colour as a sensible fallback.
    val previewTint: Color = colorHex
        ?.let { ColorOption.byHex(it)?.color ?: ColorOption.parseHex(it) }
        ?: MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New category" else "Edit category") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = { if (nameError != null) Text(nameError) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Icon", style = MaterialTheme.typography.labelLarge)
                    IconGridPicker(
                        selectedKey = iconKey,
                        tintColor = previewTint,
                        onSelect = { iconKey = it },
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Colour", style = MaterialTheme.typography.labelLarge)
                    ColorSwatchAndHsvPicker(
                        selectedHex = colorHex,
                        onHexChange = { colorHex = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSubmit(
                        trimmedName,
                        iconKey?.trim()?.takeIf { it.isNotEmpty() },
                        colorHex?.trim()?.takeIf { it.isNotEmpty() },
                    )
                },
                enabled = canSubmit,
            ) { Text(if (initial == null) "Create" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
