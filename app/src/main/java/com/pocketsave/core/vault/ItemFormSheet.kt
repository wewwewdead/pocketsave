package com.pocketsave.core.vault

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.pocketsave.common.util.ImageStorage
import com.pocketsave.core.haptics.AppHaptic
import com.pocketsave.core.haptics.rememberAppHaptics
import com.pocketsave.core.itemform.ItemFormViewModel
import com.pocketsave.core.paywall.ProChip
import com.pocketsave.core.scanner.ScannerScreen
import com.pocketsave.core.scanner.TextRecognitionService
import com.pocketsave.core.scanner.classifier.PackagingClassifier
import com.pocketsave.core.service.VaultService
import com.pocketsave.data.local.entity.ItemEntity
import com.pocketsave.domain.model.GroceryCategory
import com.pocketsave.domain.semantics.UnitMenuCatalog
import com.pocketsave.domain.semantics.UnitSemantics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Port of the add/edit Item Form used by iOS `VaultView` (`VaultPopoversOverlay`,
 * `EditItemSheet`, `ItemFormContent`). Backed by [ItemFormViewModel] so the
 * validation + packaging payload semantics stay in lockstep with iOS.
 */
/**
 * Hand-off payload emitted by [ItemFormSheet.onSaved] when the user has
 * successfully *added* (not edited) an item. Carries the inserted [ItemEntity]
 * plus the centre of the Save button in root coordinates so the caller can
 * start the fly-to-list animation. `null` means "no flight" — always the case
 * for edits and for any save that didn't complete.
 */
data class ItemFormFlightHint(
    val insertedItem: ItemEntity,
    val saveButtonCenter: Offset,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemFormSheet(
    vaultService: VaultService,
    existing: ItemEntity?,
    initialCategoryName: String?,
    onDismiss: () -> Unit,
    onSaved: (flightHint: ItemFormFlightHint?) -> Unit,
    textRecognitionService: TextRecognitionService? = null,
    packagingClassifier: PackagingClassifier? = null,
    /**
     * Called when the user taps **Scan**. The sheet forwards a callback that
     * actually opens the scanner; the caller either runs it immediately
     * (free action) or intercepts to show the paywall first. Default is
     * "always allow" so previews and tests don't need a gate.
     */
    onScanRequested: (onAllowed: () -> Unit) -> Unit = { it() },
    /**
     * When `true`, a small "PRO" badge renders next to the Scan button so
     * free users see the affordance is gated before they tap. Defaults to
     * `false` so previews / tests show the unlocked form.
     */
    scannerLocked: Boolean = false,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val imageStorage = remember { ImageStorage(context.applicationContext) }
    val formVm = remember(existing?.id) { ItemFormViewModel(requiresPortion = false, requiresStore = true) }
    val scope = rememberCoroutineScope()
    val haptics = rememberAppHaptics()
    var duplicateError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Reject whenever a distinct duplicate / validation message surfaces.
    // Keyed on the message string so the same message doesn't replay on
    // recomposition and so clearing the error (to null) doesn't fire.
    LaunchedEffect(duplicateError) {
        if (duplicateError != null) haptics.perform(AppHaptic.Reject)
    }
    var debounceJob: Job? by remember { mutableStateOf(null) }
    var showScanner by remember { mutableStateOf(false) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf<String?>(null) }
    var pendingCapture by remember { mutableStateOf<ImageStorage.CaptureTarget?>(null) }
    // Captured by `Modifier.onGloballyPositioned` on the Save button so the
    // add-item flight animation knows where the ghost should start from.
    var saveButtonCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    // Hydrate form state — on open (edit) or on pre-selected category (add).
    LaunchedEffect(existing?.id) {
        formVm.resetForm()
        if (existing == null) {
            formVm.selectedCategoryName = initialCategoryName
        } else {
            val priceOption = vaultService.getPriceOptionsForItem(existing.id).firstOrNull()
            val categoryName = vaultService.getCategoryNameForItem(existing.id)
            formVm.itemName = existing.name
            formVm.storeName = priceOption?.store.orEmpty()
            formVm.itemPrice = priceOption?.pricePerUnit?.priceValue?.toString().orEmpty()
            val unit = UnitSemantics.canonicalUnit(priceOption?.pricePerUnit?.unit ?: "pc")
            formVm.unit = unit
            if (!UnitSemantics.isContinuous(unit)) {
                formVm.packageSizeValue = priceOption?.pricePerUnit?.packageSizeValue?.toString().orEmpty()
                formVm.packageSizeUnit = priceOption?.pricePerUnit?.packageSizeUnit
                    ?.let(UnitSemantics::canonicalUnit)
                    ?: UnitSemantics.defaultPackageSizeUnit(unit)
            }
            formVm.selectedCategoryName = categoryName
            formVm.selectedImageUri = existing.imageUri
        }
    }

    // Debounced duplicate check on name/store/category changes (iOS `checkForDuplicateItemName`).
    LaunchedEffect(Unit) {
        snapshotFlow { Triple(formVm.itemName, formVm.storeName, formVm.selectedCategoryName) }
            .drop(1)
            .collect {
                debounceJob?.cancel()
                if (formVm.itemName.trim().isEmpty()) {
                    duplicateError = null
                    return@collect
                }
                debounceJob = scope.launch {
                    delay(400)
                    val result = vaultService.validateItemName(
                        name = formVm.itemName,
                        store = formVm.storeName,
                        categoryName = formVm.selectedCategoryName,
                        excludingItemId = existing?.id,
                    )
                    duplicateError = if (result.isValid) null else result.errorMessage
                }
            }
    }

    // Shared pipeline for a picked or captured Uri. Runs the subject-segmented
    // sticker save on Dispatchers.IO (inside ImageStorage) — lifts the
    // foreground, feathers edges, stamps the white outline, and writes PNG.
    // Falls back to a plain JPEG internally if ML Kit's model isn't available
    // yet, so one path covers every device. Swaps out any previous temp
    // image, surfaces an inline error on failure, and deletes the
    // camera-capture temp file afterwards if one was supplied.
    fun processPickedImage(source: Uri, captureFile: java.io.File? = null) {
        scope.launch {
            isProcessingImage = true
            imageError = null
            try {
                val stored = imageStorage.saveSubjectSticker(source)
                if (stored != null) {
                    val previous = formVm.selectedImageUri
                    formVm.selectedImageUri = stored
                    if (previous != null && previous != existing?.imageUri) {
                        imageStorage.deleteByUri(previous)
                    }
                } else {
                    imageError = "Couldn't save that photo. Try a smaller image or a different one."
                }
            } finally {
                imageStorage.deleteCaptureFile(captureFile)
                isProcessingImage = false
            }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) processPickedImage(uri)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val capture = pendingCapture
        pendingCapture = null
        if (success && capture != null) {
            processPickedImage(capture.uri, captureFile = capture.file)
        } else {
            // User backed out of the system camera — discard the empty temp file.
            imageStorage.deleteCaptureFile(capture?.file)
        }
    }

    fun launchCameraCapture() {
        val target = imageStorage.createCaptureFile()
        pendingCapture = target
        takePictureLauncher.launch(target.uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            imageError = "Camera permission denied. Pick from gallery instead."
        }
    }

    fun attemptCameraCapture() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) launchCameraCapture()
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun launchGalleryPick() {
        pickImageLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (existing == null) "Add item" else "Edit item",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (textRecognitionService != null && packagingClassifier != null) {
                    TextButton(onClick = { onScanRequested { showScanner = true } }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Scan")
                        if (scannerLocked) {
                            Spacer(Modifier.width(6.dp))
                            ProChip()
                        }
                    }
                }
            }

            ImageChooser(
                uri = formVm.selectedImageUri,
                isProcessing = isProcessingImage,
                errorMessage = imageError,
                onTakePhoto = { imageError = null; attemptCameraCapture() },
                onPickFromGallery = { imageError = null; launchGalleryPick() },
                onClear = {
                    val previous = formVm.selectedImageUri
                    formVm.selectedImageUri = null
                    imageError = null
                    if (previous != null && previous != existing?.imageUri) {
                        imageStorage.deleteByUri(previous)
                    }
                },
            )

            OutlinedTextField(
                value = formVm.itemName,
                onValueChange = { formVm.itemName = it },
                label = { Text("Item name") },
                singleLine = true,
                isError = duplicateError != null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            if (duplicateError != null) {
                Text(duplicateError!!, color = Color(0xFFFF6F71), style = MaterialTheme.typography.bodySmall)
            }

            CategoryPicker(
                selected = formVm.selectedCategoryName,
                customCategoryOptions = vaultService.state.value.categories
                    .map { it.name }
                    .filter { GroceryCategory.entries.none { g -> g.title == it } },
                onSelect = { formVm.selectedCategoryName = it },
            )

            OutlinedTextField(
                value = formVm.storeName,
                onValueChange = { formVm.storeName = it },
                label = { Text("Store") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = formVm.itemPrice,
                onValueChange = { raw ->
                    if (raw.isEmpty() || raw.matches(Regex("^\\d*[.,]?\\d{0,2}$"))) {
                        formVm.itemPrice = raw.replace(',', '.')
                    }
                },
                label = { Text("Price") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            UnitPicker(
                selected = formVm.unit,
                onSelect = { canonical ->
                    formVm.unit = canonical
                    formVm.resetPackageSize(soldAsUnit = canonical)
                },
            )

            if (!UnitSemantics.isContinuous(formVm.unit)) {
                PackageSizeFields(
                    soldAsUnit = formVm.unit,
                    value = formVm.packageSizeValue,
                    unit = formVm.packageSizeUnit,
                    onValueChange = { formVm.packageSizeValue = it },
                    onUnitChange = { formVm.packageSizeUnit = it },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) { Text("Cancel") }
                Button(
                    onClick = {
                        if (!formVm.attemptSubmission()) return@Button
                        // Freeze the Save button's current root-coord centre so
                        // the flight animation can start from there even after
                        // the sheet begins dismissing.
                        val originCenter = saveButtonCoords
                            ?.takeIf { it.isAttached }
                            ?.boundsInRoot()
                            ?.center
                        scope.launch {
                            isSaving = true
                            val payload = formVm.packageSizePayloadForPersistence
                            val price = formVm.itemPrice.toDoubleOrNull() ?: 0.0
                            val category = formVm.selectedCategoryName ?: return@launch
                            val insertedItem: ItemEntity? = if (existing == null) {
                                vaultService.addItem(
                                    name = formVm.itemName,
                                    toCategoryName = category,
                                    store = formVm.storeName,
                                    price = price,
                                    unit = formVm.unit,
                                    packageSizeValue = payload.value,
                                    packageSizeUnit = payload.unit,
                                    packagingMetadata = formVm.metadataForPersistence,
                                    imageUri = formVm.selectedImageUri,
                                )
                            } else null
                            val updated: Boolean = if (existing != null) {
                                val r = vaultService.updateItem(
                                    itemId = existing.id,
                                    newName = formVm.itemName,
                                    newCategoryName = category,
                                    newStore = formVm.storeName,
                                    newPrice = price,
                                    newUnit = formVm.unit,
                                    packageSizeValue = payload.value,
                                    packageSizeUnit = payload.unit,
                                    updatePackageSize = true,
                                    packagingMetadata = formVm.metadataForPersistence,
                                    updatePackagingMetadata = formVm.shouldUpdatePackagingMetadata,
                                    imageUri = formVm.selectedImageUri,
                                    updateImage = true,
                                )
                                if (r && existing.imageUri != null &&
                                    existing.imageUri != formVm.selectedImageUri
                                ) {
                                    imageStorage.deleteByUri(existing.imageUri)
                                }
                                r
                            } else false
                            val ok = insertedItem != null || updated
                            isSaving = false
                            if (ok) {
                                // Item actually persisted — one Confirm buzz
                                // for the successful save. Fires before the
                                // sheet dismisses so the user associates the
                                // haptic with the action they just took.
                                haptics.perform(AppHaptic.Confirm)
                                val hint = if (insertedItem != null && originCenter != null) {
                                    ItemFormFlightHint(insertedItem, originCenter)
                                } else null
                                onSaved(hint)
                            } else {
                                val recheck = vaultService.validateItemName(
                                    name = formVm.itemName,
                                    store = formVm.storeName,
                                    categoryName = formVm.selectedCategoryName,
                                    excludingItemId = existing?.id,
                                )
                                duplicateError = recheck.errorMessage ?: "Couldn't save item."
                            }
                        }
                    },
                    enabled = formVm.isFormValid && duplicateError == null && !isSaving && !isProcessingImage,
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned { saveButtonCoords = it },
                ) { Text(if (existing == null) "Save" else "Update") }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showScanner && textRecognitionService != null && packagingClassifier != null) {
        Dialog(
            onDismissRequest = { showScanner = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
            ),
        ) {
            ScannerScreen(
                textRecognitionService = textRecognitionService,
                packagingClassifier = packagingClassifier,
                onResult = { result ->
                    formVm.applyScanResult(result)
                    showScanner = false
                },
                onCancel = { showScanner = false },
            )
        }
    }
}

@Composable
private fun ImageChooser(
    uri: String?,
    isProcessing: Boolean,
    errorMessage: String?,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    onClear: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val openChooser = { if (!isProcessing) menuExpanded = true }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = !isProcessing, onClick = { openChooser() }),
                contentAlignment = Alignment.Center,
            ) {
                if (uri != null) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Add photo")
                }
                if (isProcessing) {
                    // Dim the thumbnail while IO work runs so users see we're
                    // working rather than interpreting the (now IO-threaded)
                    // delay as a frozen form.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x66000000)),
                    )
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Box {
                Column {
                    TextButton(onClick = { openChooser() }, enabled = !isProcessing) {
                        Text(if (uri == null) "Add photo" else "Replace photo")
                    }
                    if (uri != null) {
                        TextButton(onClick = onClear, enabled = !isProcessing) { Text("Remove photo") }
                    }
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                        text = { Text("Take photo") },
                        onClick = {
                            menuExpanded = false
                            onTakePhoto()
                        },
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                        text = { Text("Choose from gallery") },
                        onClick = {
                            menuExpanded = false
                            onPickFromGallery()
                        },
                    )
                }
            }
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color(0xFFFF6F71),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(
    selected: String?,
    customCategoryOptions: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            GroceryCategory.entries.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.title) },
                    leadingIcon = {
                        Icon(
                            imageVector = com.pocketsave.core.vault.icons.AppIcon.resolveIcon(cat.defaultIconKey),
                            contentDescription = null,
                        )
                    },
                    onClick = { onSelect(cat.title); expanded = false },
                )
            }
            customCategoryOptions.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = { onSelect(name); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitPicker(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = UnitMenuCatalog.continuousOptions + UnitMenuCatalog.discreteOptions
    val displayLabel = options.firstOrNull { it.abbr == selected }?.displayText ?: selected

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = displayLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Unit") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayText) },
                    onClick = { onSelect(UnitSemantics.canonicalUnit(option.abbr)); expanded = false },
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
            onValueChange = { raw ->
                if (raw.isEmpty() || raw.matches(Regex("^\\d*[.,]?\\d*$"))) {
                    onValueChange(raw.replace(',', '.'))
                }
            },
            label = { Text("Package size") },
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
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { onUnitChange(UnitSemantics.canonicalUnit(option)); expanded = false },
                    )
                }
            }
        }
    }
}
