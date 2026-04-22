package com.pocketsave.domain.semantics

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray

/** Port of `PocketSave/Utils/UnitMenuCatalog.swift`. */
data class UnitMenuOption(val abbr: String, val full: String) {
    val id: String get() = "${abbr.lowercase()}|${full.lowercase()}"

    val displayText: String
        get() = when {
            abbr != full && full.isNotEmpty() -> "$abbr - $full"
            full.isEmpty() -> abbr
            else -> full
        }
}

object UnitMenuCatalog {

    val continuousOptions: List<UnitMenuOption> = listOf(
        UnitMenuOption("g", "grams"),
        UnitMenuOption("kg", "kilograms"),
        UnitMenuOption("lb", "pounds"),
        UnitMenuOption("oz", "ounces"),
        UnitMenuOption("L", "liters"),
        UnitMenuOption("mL", "milliliters"),
    )

    val discreteOptions: List<UnitMenuOption> = listOf(
        UnitMenuOption("pc", "piece"),
        UnitMenuOption("pack", "pack"),
        UnitMenuOption("sachet", "sachet"),
        UnitMenuOption("can", "can"),
        UnitMenuOption("roll", "roll"),
        UnitMenuOption("bar", "bar"),
        UnitMenuOption("stick", "stick"),
        UnitMenuOption("bottle", "bottle"),
        UnitMenuOption("jar", "jar"),
        UnitMenuOption("jug", "jug"),
        UnitMenuOption("carton", "carton"),
        UnitMenuOption("tub", "tub"),
        UnitMenuOption("pouch", "pouch"),
        UnitMenuOption("container", "container"),
        UnitMenuOption("box", "box"),
        UnitMenuOption("tray", "tray"),
        UnitMenuOption("bunch", "bunch"),
        UnitMenuOption("dozen", "dozen"),
        UnitMenuOption("bundle", "bundle"),
        UnitMenuOption("wrap", "wrap"),
        UnitMenuOption("bag", "bag"),
    )

    private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

    fun customOptions(json: String): List<UnitMenuOption> {
        if (json.isBlank()) return emptyList()
        val values: List<String> = runCatching {
            lenientJson.parseToJsonElement(json).jsonArray
                .mapNotNull { (it as? JsonPrimitive)?.content }
        }.getOrElse { return emptyList() }

        val seen = HashSet<String>()
        val result = ArrayList<UnitMenuOption>()
        for (raw in values) {
            val trimmed = raw.trim()
            val key = trimmed.lowercase()
            if (trimmed.isEmpty() || key in seen) continue
            seen += key
            result += UnitMenuOption(trimmed, trimmed)
        }
        return result
    }

    fun allOptions(customUnitsJson: String): List<UnitMenuOption> =
        continuousOptions + discreteOptions + customOptions(customUnitsJson)
}
