package com.pocketsave.core.scanner.model

import com.pocketsave.domain.semantics.PackagingScanMetadata
import com.pocketsave.domain.semantics.PackagingSignal

/**
 * Port of iOS `ProductScanResult` from
 * `PocketSave/Components/ProductScannerSheet.swift`.
 *
 * Returned by the parser and consumed by [com.pocketsave.core.itemform.ItemFormViewModel.applyScanResult]
 * to populate the add/edit form in one shot.
 */
data class ProductScanResult(
    val name: String,
    val price: Double?,
    val unit: String?,
    val packageSizeValue: Double?,
    val packageSizeUnit: String?,
    val packagingMetadata: PackagingScanMetadata,
    val packagingSignals: List<PackagingSignal>,
    val rawLines: List<String>,
) {
    /** Mirror of iOS `replacingName` — returns a copy with the name overridden. */
    fun replacingName(updatedName: String): ProductScanResult = copy(
        name = normalizeScannedItemName(updatedName),
    )

    companion object {
        val EMPTY = ProductScanResult(
            name = "",
            price = null,
            unit = null,
            packageSizeValue = null,
            packageSizeUnit = null,
            packagingMetadata = PackagingScanMetadata.EMPTY,
            packagingSignals = emptyList(),
            rawLines = emptyList(),
        )

        /**
         * Port of the private `normalizedScannedItemName(_:)` helper in iOS.
         * Keeps names readable when OCR returns an ALL-CAPS string: trims, then
         * lowercases + title-cases when no lowercase letters were present.
         */
        fun normalizeScannedItemName(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return ""
            val letters = trimmed.filter { it.isLetter() }
            if (letters.isEmpty()) return trimmed
            val isAllUppercase = letters.all { !it.isLowerCase() }
            return if (!isAllUppercase) trimmed else titleCase(trimmed.lowercase())
        }

        private fun titleCase(value: String): String =
            value.split(' ').joinToString(" ") { word ->
                if (word.isEmpty()) word
                else word.replaceFirstChar { c -> c.titlecase() }
            }
    }
}
