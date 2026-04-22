package com.pocketsave.core.itemform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pocketsave.domain.semantics.PackagingScanMetadata
import com.pocketsave.domain.semantics.PackagingSemantics
import com.pocketsave.domain.semantics.PackagingSignal
import com.pocketsave.domain.semantics.UnitSemantics

/**
 * Port of `PocketSave/Core/ItemForm/ItemFormViewModel.swift`.
 *
 * Reused by the onboarding first-item flow today and by the full Item Form /
 * Shopping Add flows in later phases. Kept as a plain class (not an Android
 * `ViewModel`) so owning screens can compose it — matches the iOS pattern where
 * `OnboardingViewModel` holds an `ItemFormViewModel` directly.
 *
 * State uses Compose's `mutableStateOf` for clean two-way binding with
 * `TextField`s; no Android `ViewModel` lifecycle is needed because nothing here
 * is async. Async work (duplicate checks, submission) lives in
 * [com.pocketsave.core.onboarding.OnboardingViewModel].
 */
class ItemFormViewModel(
    val requiresPortion: Boolean = false,
    val requiresStore: Boolean = true,
    val context: EditContext = EditContext.VAULT,
) {

    enum class EditContext { VAULT, CART }

    // MARK: - Form data
    var itemName: String by mutableStateOf("")
    var storeName: String by mutableStateOf("")
    var itemPrice: String by mutableStateOf("")
    var unit: String by mutableStateOf("pc")
    var packageSizeValue: String by mutableStateOf("")
    var packageSizeUnit: String by mutableStateOf(UnitSemantics.defaultPackageSizeUnit("pc"))
    var packagingMetadata: PackagingScanMetadata by mutableStateOf(PackagingScanMetadata.EMPTY)
    var packagingSignals: List<PackagingSignal> by mutableStateOf(emptyList())
    var shouldUpdatePackagingMetadata: Boolean by mutableStateOf(false)
    var selectedCategoryName: String? by mutableStateOf(null)
    var portion: Double? by mutableStateOf(null)

    // MARK: - Image (transient; real bytes persisted separately in a later phase)
    var selectedImageUri: String? by mutableStateOf(null)
    var shouldRemoveImage: Boolean by mutableStateOf(false)

    // MARK: - Mode-aware properties
    var shouldUpdateVault: Boolean by mutableStateOf(true)
    var isTemporaryShoppingItem: Boolean by mutableStateOf(false)
    var allowedLockedStoreNames: List<String> by mutableStateOf(emptyList())
    var bypassPlanLocks: Boolean by mutableStateOf(false)

    // MARK: - Validation state
    var attemptedSubmission: Boolean by mutableStateOf(false)
        private set
    var firstMissingField: String? by mutableStateOf(null)
        private set
    var invalidSubmissionCount: Int by mutableStateOf(0)
        private set

    // MARK: - Computed

    val isValidStoreName: Boolean
        get() = storeName.trim().isNotEmpty()

    val isFormValid: Boolean
        get() {
            val priceValue = itemPrice.toDoubleOrNull()
            var valid = itemName.isNotEmpty() &&
                priceValue != null && priceValue > 0.0 &&
                unit.isNotEmpty() &&
                selectedCategoryName != null

            if (requiresStore) valid = valid && isValidStoreName
            if (requiresPortion) valid = valid && (portion != null && (portion ?: 0.0) > 0.0)
            return valid
        }

    // MARK: - Validation methods

    /** Port of `validateAndGetFirstMissingField()`. */
    fun validateAndGetFirstMissingField(): String? {
        if (itemName.isEmpty()) return "Item Name"
        if (selectedCategoryName == null) return "Category"
        if (requiresStore && !isValidStoreName) return "Store Name"
        if (requiresPortion && (portion == null || portion == 0.0)) return "Portion"
        if (unit.isEmpty()) return "Unit"
        val price = itemPrice.toDoubleOrNull()
        if (price == null || price == 0.0) return "Price"
        return null
    }

    /** Port of `attemptSubmission()`. */
    fun attemptSubmission(): Boolean {
        attemptedSubmission = true
        firstMissingField = validateAndGetFirstMissingField()
        invalidSubmissionCount = if (firstMissingField != null) invalidSubmissionCount + 1 else 0
        return firstMissingField == null
    }

    fun clearErrorForField(field: String) {
        if (firstMissingField == field) firstMissingField = null
    }

    fun resetValidation() {
        attemptedSubmission = false
        firstMissingField = null
        invalidSubmissionCount = 0
    }

    // MARK: - Lifecycle

    /** Port of `resetForm()`. */
    fun resetForm() {
        itemName = ""
        storeName = ""
        itemPrice = ""
        unit = "pc"
        resetPackageSize(soldAsUnit = unit)
        packagingMetadata = PackagingScanMetadata.EMPTY
        packagingSignals = emptyList()
        shouldUpdatePackagingMetadata = false
        selectedCategoryName = null
        portion = null
        shouldUpdateVault = true
        selectedImageUri = null
        shouldRemoveImage = false
        resetValidation()
    }

    /** Port of `resetPackageSize(for:)`. */
    fun resetPackageSize(soldAsUnit: String? = null) {
        val resolved = normalizedSoldAsUnit(soldAsUnit)
        packageSizeValue = ""
        packageSizeUnit = UnitSemantics.defaultPackageSizeUnit(resolved)
    }

    /**
     * Port of the iOS `applyScanResult(_:)` method (currently commented out —
     * see `ItemFormViewModel.swift::applyScanResult`). Fills whatever fields
     * the OCR/parser inferred without overwriting non-empty user edits.
     */
    fun applyScanResult(result: com.pocketsave.core.scanner.model.ProductScanResult) {
        if (result.name.isNotBlank() && itemName.isBlank()) {
            itemName = result.name
        }
        val price = result.price
        if (price != null && price > 0.0 && itemPrice.isBlank()) {
            itemPrice = if (price % 1.0 == 0.0) price.toInt().toString() else "%.2f".format(price)
        }
        val resolvedUnit = result.unit
        if (!resolvedUnit.isNullOrEmpty()) {
            val canonical = UnitSemantics.canonicalUnit(resolvedUnit)
            if (canonical.isNotEmpty()) {
                unit = canonical
                if (UnitSemantics.isContinuous(canonical)) {
                    // Continuous units imply no package size — clear any stale input.
                    packageSizeValue = ""
                    packageSizeUnit = UnitSemantics.defaultPackageSizeUnit(canonical)
                } else {
                    val sizeValue = result.packageSizeValue
                    val sizeUnit = result.packageSizeUnit
                    if (sizeValue != null && sizeValue > 0.0) {
                        packageSizeValue = if (sizeValue % 1.0 == 0.0) sizeValue.toInt().toString()
                        else "%.2f".format(sizeValue)
                    }
                    if (!sizeUnit.isNullOrEmpty()) {
                        packageSizeUnit = UnitSemantics.canonicalUnit(sizeUnit)
                    }
                }
            }
        }

        packagingMetadata = result.packagingMetadata
        packagingSignals = result.packagingSignals
        shouldUpdatePackagingMetadata = !result.packagingMetadata.isEmpty ||
            result.packagingSignals.isNotEmpty()
    }

    // MARK: - Persistence payloads

    /** Port of the `metadataForPersistence` computed property. */
    val metadataForPersistence: PackagingScanMetadata?
        get() {
            val normalizedOuter = PackagingSemantics.canonicalPackagingUnit(packagingMetadata.outerPackagingUnit)
            val normalizedConfidence = PackagingSemantics.clampedConfidence(packagingMetadata.outerPackagingConfidence)
            if (normalizedOuter == null && normalizedConfidence == null && packagingMetadata.source == null) {
                return null
            }
            return PackagingScanMetadata(
                soldAsConfidence = packagingMetadata.soldAsConfidence,
                outerPackagingUnit = normalizedOuter,
                outerPackagingConfidence = normalizedConfidence,
                source = packagingMetadata.source,
            )
        }

    /** Port of the `packageSizePayloadForPersistence` computed property. */
    data class PackageSizePayload(val value: Double?, val unit: String?) {
        companion object {
            val EMPTY = PackageSizePayload(null, null)
        }
    }

    val packageSizePayloadForPersistence: PackageSizePayload
        get() {
            val trimmed = packageSizeValue.trim()
            val parsed = trimmed.toDoubleOrNull()
            if (UnitSemantics.isContinuous(unit) || parsed == null || parsed <= 0.0) {
                return PackageSizePayload.EMPTY
            }
            val normalizedUnit = UnitSemantics.canonicalUnit(packageSizeUnit)
            if (normalizedUnit.trim().isEmpty()) return PackageSizePayload.EMPTY
            return PackageSizePayload(parsed, normalizedUnit)
        }

    // MARK: - Private helpers

    private fun normalizedSoldAsUnit(soldAsUnit: String?): String {
        val trimmed = soldAsUnit?.trim()
        val fallback = if (!trimmed.isNullOrEmpty()) trimmed else unit
        return UnitSemantics.canonicalUnit(fallback)
    }
}
