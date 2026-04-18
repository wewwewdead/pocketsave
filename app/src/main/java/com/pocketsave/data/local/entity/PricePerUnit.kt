package com.pocketsave.data.local.entity

/**
 * Mirrors the iOS `PricePerUnit` value type. Embedded into [PriceOptionEntity] —
 * iOS models it as a separate `@Model`, but Room represents 1:1 value types more
 * naturally with `@Embedded`.
 */
data class PricePerUnit(
    val priceValue: Double,
    val unit: String,
    val packageSizeValue: Double? = null,
    val packageSizeUnit: String? = null,
    val outerPackagingUnit: String? = null,
    val outerPackagingConfidence: Double? = null,
    val outerPackagingSource: String? = null,
)
