package com.pocketsave.domain.semantics

/**
 * Port of `PocketSave/Utils/UnitSemantics.swift`.
 *
 * Keeps canonical unit spellings and the kg/L base-quantity conversion identical
 * to iOS so price-per-unit math matches across platforms.
 */
object UnitSemantics {

    val continuousUnits: Set<String> = setOf("g", "kg", "lb", "oz", "L", "mL")

    val packageSizeContinuousUnits: List<String> = listOf("g", "kg", "lb", "oz", "mL", "L")

    val packageSizeCountUnits: List<String> = listOf(
        "pc", "pack", "sachet", "can", "roll", "bar", "stick",
        "bottle", "jar", "jug", "carton", "tub", "pouch",
        "container", "box", "tray", "bunch", "dozen",
        "bundle", "wrap", "bag",
    )

    private val countFirstPackageSoldAsUnits: Set<String> = setOf(
        "pack", "sachet", "carton", "box", "tray",
        "bundle", "wrap", "bag", "bunch", "dozen",
    )

    fun canonicalUnit(unit: String): String {
        val trimmed = unit.trim()
        if (trimmed.isEmpty()) return ""
        return when (trimmed.lowercase()) {
            "l", "liter", "liters", "litre", "litres" -> "L"
            "ml", "milliliter", "milliliters", "millilitre", "millilitres" -> "mL"
            "pc", "pcs", "piece", "pieces" -> "pc"
            "kg", "kilogram", "kilograms" -> "kg"
            "g", "gram", "grams" -> "g"
            "lb", "pound", "pounds" -> "lb"
            "oz", "ounce", "ounces" -> "oz"
            "pack", "packs", "pk", "pkt" -> "pack"
            "sachet", "sachets" -> "sachet"
            "can", "cans" -> "can"
            "roll", "rolls" -> "roll"
            "bar", "bars" -> "bar"
            "stick", "sticks" -> "stick"
            "bottle", "bottles", "btl", "btls" -> "bottle"
            "carton", "cartons", "ctn", "ctns" -> "carton"
            "box", "boxes" -> "box"
            "tray", "trays" -> "tray"
            "bunch", "bunches" -> "bunch"
            "dozen", "dozens" -> "dozen"
            "bundle", "bundles" -> "bundle"
            "wrap", "wraps" -> "wrap"
            "bag", "bags" -> "bag"
            "jar", "jars" -> "jar"
            "tub", "tubs" -> "tub"
            "pouch", "pouches" -> "pouch"
            "container", "containers" -> "container"
            "jug", "jugs" -> "jug"
            else -> trimmed.lowercase()
        }
    }

    fun isContinuous(unit: String): Boolean = canonicalUnit(unit) in continuousUnits

    fun packageSizeUnitOptions(soldAsUnit: String): List<String> {
        val soldAs = canonicalUnit(soldAsUnit)
        val preferCount = soldAs in countFirstPackageSoldAsUnits
        val primary = if (preferCount) packageSizeCountUnits else packageSizeContinuousUnits
        val secondary = if (preferCount) packageSizeContinuousUnits else packageSizeCountUnits
        return primary + secondary
    }

    fun defaultPackageSizeUnit(soldAsUnit: String): String {
        val soldAs = canonicalUnit(soldAsUnit)
        return if (soldAs in countFirstPackageSoldAsUnits) "pc" else "mL"
    }

    data class BaseQuantity(val value: Double, val unitLabel: String)

    fun baseQuantity(value: Double, unit: String): BaseQuantity? {
        if (value <= 0.0) return null
        return when (val canonical = canonicalUnit(unit)) {
            "g" -> BaseQuantity(value / 1000.0, "kg")
            "kg" -> BaseQuantity(value, "kg")
            "lb" -> BaseQuantity(value * 0.45359237, "kg")
            "oz" -> BaseQuantity(value * 0.028349523125, "kg")
            "mL" -> BaseQuantity(value / 1000.0, "L")
            "L" -> BaseQuantity(value, "L")
            in packageSizeCountUnits -> BaseQuantity(value, canonical)
            else -> null
        }
    }
}
