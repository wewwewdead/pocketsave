package com.pocketsave.domain.semantics

/**
 * Port of the domain types from `PocketSave/Utils/PackagingSemantics.swift`.
 *
 * The iOS file also hosts merging/deduplication helpers for Vision/OCR signals; those
 * live here too so the scanner port (later phase) can plug into the same semantics.
 * The ML classifier itself is deferred — see `PackagingModelClassifier.swift`.
 */

enum class PackagingSignalSource(val raw: String) {
    VISION_BUILTIN("vision_builtin"),
    OCR_EXPLICIT("ocr_explicit"),
    HEURISTIC("heuristic"),
    BULK_HEURISTIC("bulk_heuristic"),
    CUSTOM_MODEL("custom_model");

    companion object {
        fun fromRaw(raw: String?): PackagingSignalSource? =
            raw?.let { entries.firstOrNull { s -> s.raw == it } }
    }
}

data class PackagingSignal(
    val unit: String,
    val confidence: Double,
    val source: PackagingSignalSource,
) {
    companion object {
        fun normalized(unit: String, confidence: Double, source: PackagingSignalSource): PackagingSignal {
            val canonical = PackagingSemantics.canonicalPackagingUnit(unit) ?: unit
            val clamped = PackagingSemantics.clampedConfidence(confidence) ?: 0.0
            return PackagingSignal(canonical, clamped, source)
        }
    }
}

data class PackagingScanMetadata(
    val soldAsConfidence: Double? = null,
    val outerPackagingUnit: String? = null,
    val outerPackagingConfidence: Double? = null,
    val source: PackagingSignalSource? = null,
) {
    val isEmpty: Boolean
        get() = soldAsConfidence == null &&
            outerPackagingUnit == null &&
            outerPackagingConfidence == null &&
            source == null

    companion object {
        val EMPTY = PackagingScanMetadata()
    }
}

object PackagingSemantics {

    val supportedUnits: Set<String> = setOf(
        "pack", "sachet", "can", "roll", "bar", "stick",
        "bottle", "jar", "jug", "carton", "tub", "pouch",
        "container", "box", "tray", "bundle", "wrap", "bag",
    )

    val outerPackagingPriority: List<String> = listOf(
        "box", "carton", "tray", "bag", "container", "bundle",
        "wrap", "bottle", "jar", "jug", "tub", "pouch", "can",
    )

    fun canonicalPackagingUnit(rawUnit: String?): String? {
        if (rawUnit == null) return null
        val canonical = UnitSemantics.canonicalUnit(rawUnit)
        return if (canonical in supportedUnits) canonical else null
    }

    fun clampedConfidence(value: Double?): Double? {
        if (value == null || value.isNaN() || value.isInfinite()) return null
        return value.coerceIn(0.0, 1.0)
    }

    fun deduplicatedSignals(signals: List<PackagingSignal>): List<PackagingSignal> {
        val bestByKey = HashMap<String, PackagingSignal>()
        for (signal in signals) {
            val canonical = canonicalPackagingUnit(signal.unit) ?: continue
            val normalized = PackagingSignal.normalized(canonical, signal.confidence, signal.source)
            val key = "$canonical|${signal.source.raw}"
            val existing = bestByKey[key]
            if (existing == null || normalized.confidence > existing.confidence) {
                bestByKey[key] = normalized
            }
        }
        return bestByKey.values.sortedWith(
            compareByDescending<PackagingSignal> { it.confidence }.thenBy { it.unit },
        )
    }

    fun mergedSignals(signals: List<PackagingSignal>): List<PackagingSignal> {
        val deduplicated = deduplicatedSignals(signals)
        val grouped = deduplicated.groupBy { it.unit }
        val merged = grouped.mapNotNull { (unit, unitSignals) ->
            val dominant = unitSignals.maxByOrNull { weightedEvidence(it) } ?: return@mapNotNull null
            val combinedConfidence = 1.0 - unitSignals.fold(1.0) { acc, s ->
                val evidence = kotlin.math.min(weightedEvidence(s), 0.98)
                acc * (1.0 - evidence)
            }
            PackagingSignal.normalized(
                unit = unit,
                confidence = clampedConfidence(combinedConfidence) ?: dominant.confidence,
                source = dominant.source,
            )
        }
        return merged.sortedWith(
            compareByDescending<PackagingSignal> { it.confidence }.thenBy { it.unit },
        )
    }

    fun strongestSignal(unit: String?, signals: List<PackagingSignal>): PackagingSignal? {
        val target = canonicalPackagingUnit(unit) ?: return null
        return mergedSignals(signals)
            .filter { it.unit == target }
            .maxWithOrNull(
                compareBy<PackagingSignal> { it.confidence }.thenBy { it.source.raw },
            )
    }

    fun strongestSignal(preferredUnits: List<String>, signals: List<PackagingSignal>): PackagingSignal? {
        val merged = mergedSignals(signals)
        for (unit in preferredUnits) {
            merged.firstOrNull { it.unit == unit }?.let { return it }
        }
        return null
    }

    private fun weightedEvidence(signal: PackagingSignal): Double {
        val weight = when (signal.source) {
            PackagingSignalSource.CUSTOM_MODEL -> 1.0
            PackagingSignalSource.OCR_EXPLICIT -> 0.95
            PackagingSignalSource.BULK_HEURISTIC -> 0.88
            PackagingSignalSource.VISION_BUILTIN -> 0.74
            PackagingSignalSource.HEURISTIC -> 0.64
        }
        return signal.confidence * weight
    }
}
