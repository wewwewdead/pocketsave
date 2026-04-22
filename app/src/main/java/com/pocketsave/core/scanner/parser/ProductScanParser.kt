package com.pocketsave.core.scanner.parser

import com.pocketsave.core.scanner.model.ProductScanResult
import com.pocketsave.core.scanner.model.ScannedTextBlock
import com.pocketsave.domain.semantics.PackagingScanMetadata
import com.pocketsave.domain.semantics.PackagingSemantics
import com.pocketsave.domain.semantics.PackagingSignal
import com.pocketsave.domain.semantics.PackagingSignalSource
import com.pocketsave.domain.semantics.UnitSemantics

/**
 * Port of the static helper `parse(blocks:packagingSignals:)` and its private
 * helpers from `PocketSave/Components/ProductScannerSheet.swift` (the
 * `ProductScanParser` scope, approximately lines 1462–2170).
 *
 * Regex patterns, scoring constants, and the discrete-unit alias table are
 * copied 1:1 so the Android parser produces the same candidate rankings iOS
 * does. Unlike iOS — which currently ships name-parsing disabled — this port
 * keeps a conservative name heuristic (`bestNameCandidate`) active, since the
 * Phase 9 brief explicitly asks for a name inference step.
 */
object ProductScanParser {

    /**
     * Port of iOS `ProductScanParser.parse(blocks:packagingSignals:)`. Accepts
     * OCR blocks + optional image-classifier signals and returns the normalised
     * scan result.
     */
    fun parse(
        blocks: List<ScannedTextBlock>,
        packagingSignals: List<PackagingSignal> = emptyList(),
    ): ProductScanResult {
        val candidates = normalizeLines(blocks)
        val priceMatch = bestPriceCandidate(candidates)
        val unitPriceUnit = priceMatch?.line?.let { extractUnitPriceUnit(it) }
        val packageSizeCandidate = if (unitPriceUnit == null) {
            bestPackageSizeCandidate(candidates, priceMatch?.line)
        } else null
        val bulkCandidate = bestBulkQuantityCandidate(candidates)

        val excludedLines = setOfNotNull(priceMatch?.line, packageSizeCandidate?.line)
        val name = bestNameCandidate(
            blocks = blocks,
            excludedLines = excludedLines,
        )

        val discreteUnit = bestDiscreteUnitCandidate(
            candidates = candidates,
            preferredLine = priceMatch?.line,
            packageSizeCandidate = packageSizeCandidate,
            preferredName = name,
            bulkCandidate = bulkCandidate,
            packagingSignals = packagingSignals,
        )
        val candidatePackagingSignals = packagingSignalsForResult(
            candidates = candidates,
            preferredLine = priceMatch?.line,
            preferredName = name,
            bulkCandidate = bulkCandidate,
            packagingSignals = packagingSignals,
        )
        val mergedPackagingSignals = PackagingSemantics.mergedSignals(candidatePackagingSignals)

        val unit: String? = when {
            unitPriceUnit != null -> unitPriceUnit
            discreteUnit != null -> discreteUnit
            else -> {
                val hasAnySignal = name.isNotEmpty() || priceMatch != null ||
                    packageSizeCandidate != null || mergedPackagingSignals.isNotEmpty()
                if (hasAnySignal) "pc" else null
            }
        }

        val resolvedPackagingMetadata = buildPackagingMetadata(
            selectedUnit = unit,
            bulkCandidate = bulkCandidate,
            signals = mergedPackagingSignals,
        )

        return ProductScanResult(
            name = ProductScanResult.normalizeScannedItemName(name),
            price = priceMatch?.price,
            unit = unit,
            packageSizeValue = packageSizeCandidate?.value,
            packageSizeUnit = packageSizeCandidate?.unit,
            packagingMetadata = resolvedPackagingMetadata,
            packagingSignals = mergedPackagingSignals,
            rawLines = candidates.map { it.line },
        )
    }

    // MARK: - Alias table (iOS `discreteUnitAliases`)

    private val discreteUnitAliases: Map<String, String> = mapOf(
        "pc" to "pc", "pcs" to "pc", "piece" to "pc", "pieces" to "pc", "ea" to "pc", "each" to "pc",
        "pack" to "pack", "packs" to "pack", "pk" to "pack", "pkt" to "pack",
        "sachet" to "sachet", "sachets" to "sachet",
        "can" to "can", "cans" to "can",
        "roll" to "roll", "rolls" to "roll",
        "bar" to "bar", "bars" to "bar",
        "stick" to "stick", "sticks" to "stick",
        "bottle" to "bottle", "bottles" to "bottle", "btl" to "bottle", "btls" to "bottle",
        "jar" to "jar", "jars" to "jar",
        "jug" to "jug", "jugs" to "jug",
        "carton" to "carton", "cartons" to "carton", "ctn" to "carton", "ctns" to "carton",
        "tub" to "tub", "tubs" to "tub",
        "pouch" to "pouch", "pouches" to "pouch",
        "container" to "container", "containers" to "container",
        "box" to "box", "boxes" to "box",
        "tray" to "tray", "trays" to "tray",
        "bunch" to "bunch", "bunches" to "bunch",
        "dozen" to "dozen", "dozens" to "dozen",
        "bundle" to "bundle", "bundles" to "bundle",
        "wrap" to "wrap", "wraps" to "wrap",
        "bag" to "bag", "bags" to "bag",
    )

    // MARK: - Candidate normalisation

    private data class ScannedCandidate(val line: String, val bounds: ScannedTextBlock.Bounds)

    private fun normalizeLines(blocks: List<ScannedTextBlock>): List<ScannedCandidate> {
        val seen = HashSet<String>()
        val result = ArrayList<ScannedCandidate>()
        for (block in blocks) {
            for (raw in block.transcript.split('\n', '\r')) {
                val line = normalizeText(raw)
                val key = line.lowercase()
                if (line.isEmpty() || key in seen) continue
                seen += key
                result += ScannedCandidate(line = line, bounds = block.bounds)
            }
        }
        return result
    }

    private fun normalizeText(value: String): String = value
        .replace('\t', ' ')
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .joinToString(" ")
        .trim()

    // MARK: - Price extraction

    private data class PriceCandidate(val price: Double, val line: String, val score: Double)

    private fun bestPriceCandidate(candidates: List<ScannedCandidate>): PriceCandidate? {
        var best: PriceCandidate? = null
        for (candidate in candidates) {
            val line = candidate.line
            val extracted = extractPrice(line) ?: continue

            var score = 0.0
            if (containsCurrencySymbol(line)) score += 10
            if (line.contains('.') || line.contains(',')) score += 4
            if (line.length <= 10) score += 2
            if (line.lowercase().contains("save")) score -= 2

            score += minOf(candidate.bounds.height, 120f) / 6.0
            score += minOf(candidate.bounds.width, 300f) / 60.0

            val next = PriceCandidate(extracted, line, score)
            val current = best
            best = when {
                current == null -> next
                next.score > current.score -> next
                next.score == current.score && next.price > current.price -> next
                else -> current
            }
        }
        return best
    }

    private fun containsCurrencySymbol(line: String): Boolean {
        val lower = line.lowercase()
        return line.contains('₱') || line.contains('$') || lower.contains("php")
    }

    private val priceRegex = Regex(
        "(?i)(?:₱|\\\$|php|p)?\\s*(\\d{1,3}(?:[,\\s]\\d{3})*|\\d+)([.,]\\d{2})",
    )

    private fun extractPrice(line: String): Double? {
        val match = priceRegex.find(line) ?: return null
        val integerPart = match.groupValues[1]
        val decimalPart = match.groupValues[2]
        var numberString = (integerPart + decimalPart).replace(" ", "")

        if (numberString.contains(',') && numberString.contains('.')) {
            numberString = numberString.replace(",", "")
        } else if (numberString.contains(',') && !numberString.contains('.')) {
            val parts = numberString.split(",", limit = 2)
            if (parts.size == 2 && parts[1].length == 2) {
                numberString = parts[0] + "." + parts[1]
            }
        }

        val value = numberString.toDoubleOrNull() ?: return null
        return if (value > 0.0) value else null
    }

    private fun parseNumber(raw: String): Double? {
        var numberString = raw.replace(" ", "").trim()
        if (numberString.contains(',') && numberString.contains('.')) {
            numberString = numberString.replace(",", "")
        } else if (numberString.contains(',') && !numberString.contains('.')) {
            val parts = numberString.split(",", limit = 2)
            numberString = if (parts.size == 2 && parts[1].length <= 2) {
                parts[0] + "." + parts[1]
            } else numberString.replace(",", "")
        }
        val value = numberString.toDoubleOrNull() ?: return null
        return if (value > 0.0) value else null
    }

    // MARK: - Unit extraction

    private val unitPriceRegex = Regex("(?i)(?:/|per)\\s*(kg|g|lb|oz|ml|l)\\b")

    private fun extractUnitPriceUnit(line: String): String? {
        val normalized = line.lowercase()
        val match = unitPriceRegex.find(normalized) ?: return null
        val raw = match.groupValues[1]
        val unit = UnitSemantics.canonicalUnit(raw)
        return if (UnitSemantics.isContinuous(unit)) unit else null
    }

    private val discreteUnitQuantityRegex = Regex(
        "(?i)\\b\\d+(?:[.,]\\d+)?\\s*(pc|pcs|piece|pieces|ea|each|pack|packs|pk|pkt|sachet|sachets|can|cans|roll|rolls|bar|bars|stick|sticks|bottle|bottles|btl|btls|jar|jars|jug|jugs|carton|cartons|ctn|ctns|tub|tubs|pouch|pouches|container|containers|box|boxes|tray|trays|bunch|bunches|dozen|dozens|bundle|bundles|wrap|wraps|bag|bags)\\b",
    )

    private fun extractDiscreteUnit(line: String): String? {
        val normalized = line.lowercase()
        extractDiscreteUnitFromQuantity(normalized)?.let { return it }
        return firstDiscreteAliasMatch(normalized)
    }

    private fun extractDiscreteUnitFromQuantity(normalized: String): String? {
        val match = discreteUnitQuantityRegex.find(normalized) ?: return null
        val raw = match.groupValues[1].lowercase()
        return discreteUnitAliases[raw] ?: UnitSemantics.canonicalUnit(raw)
    }

    private fun firstDiscreteAliasMatch(normalized: String): String? {
        for (token in normalized.split(Regex("[^A-Za-z0-9]+"))) {
            if (token.isEmpty()) continue
            discreteUnitAliases[token]?.let { return it }
        }
        return null
    }

    // MARK: - Package size extraction

    private data class PackageSizeCandidate(
        val value: Double,
        val unit: String,
        val line: String,
        val score: Double,
    )

    private fun bestPackageSizeCandidate(
        candidates: List<ScannedCandidate>,
        preferredLine: String?,
    ): PackageSizeCandidate? {
        var best: PackageSizeCandidate? = null
        if (preferredLine != null) {
            extractPackageSize(preferredLine)?.let { extracted ->
                best = PackageSizeCandidate(
                    value = extracted.first,
                    unit = extracted.second,
                    line = preferredLine,
                    score = 18.0,
                )
            }
        }
        for (candidate in candidates) {
            val extracted = extractPackageSize(candidate.line) ?: continue
            var score = 0.0
            if (candidate.line.contains('(')) score += 2
            if (containsCurrencySymbol(candidate.line)) score -= 6
            score += minOf(candidate.bounds.height, 120f) / 6.0
            score += minOf(candidate.bounds.width, 300f) / 60.0

            val next = PackageSizeCandidate(
                value = extracted.first,
                unit = extracted.second,
                line = candidate.line,
                score = score,
            )
            val current = best
            best = when {
                current == null -> next
                next.score > current.score -> next
                next.score == current.score && next.value > current.value -> next
                else -> current
            }
        }
        return best
    }

    private val packageSizeRegex = Regex(
        "(?i)\\b(\\d+(?:[.,]\\d+)?)\\s*(kg|g|lb|oz|ml|l|gal|gallon|gallons|fl\\s*oz|floz)\\b",
    )

    private val multiPackSizeRegex = Regex(
        "(?i)\\b(\\d+)\\s*[x×]\\s*(\\d+(?:[.,]\\d+)?)\\s*(kg|g|lb|oz|ml|l|gal|gallon|gallons|fl\\s*oz|floz)\\b",
    )

    private fun extractPackageSize(line: String): Pair<Double, String>? {
        val normalized = line.lowercase()
        extractMultiPackSize(normalized)?.let { return it }

        val matches = packageSizeRegex.findAll(normalized).toList()
        if (matches.isEmpty()) return null

        var best: Triple<Double, String, Int>? = null
        for (match in matches) {
            val rawValue = match.groupValues[1]
            val rawUnit = match.groupValues[2].trim()
            val value = parseNumber(rawValue) ?: continue
            val normalizedRawUnit = rawUnit.replace(" ", "")
            val mapped = mapSizeUnit(value, normalizedRawUnit)

            var score = 0
            if (mapped.second == "L" || mapped.second == "mL" || mapped.second == "kg" || mapped.second == "g") score += 2
            if (rawUnit.contains("gal") || rawUnit.contains("fl")) score -= 1

            val current = best
            best = when {
                current == null -> Triple(mapped.first, mapped.second, score)
                score > current.third -> Triple(mapped.first, mapped.second, score)
                score == current.third && mapped.first > current.first -> Triple(mapped.first, mapped.second, score)
                else -> current
            }
        }
        val b = best ?: return null
        return b.first to b.second
    }

    private fun extractMultiPackSize(normalized: String): Pair<Double, String>? {
        val match = multiPackSizeRegex.find(normalized) ?: return null
        val rawCount = match.groupValues[1]
        val rawValue = match.groupValues[2]
        val rawUnit = match.groupValues[3].trim()

        val count = rawCount.toIntOrNull() ?: return null
        val value = parseNumber(rawValue) ?: return null
        val normalizedRawUnit = rawUnit.replace(" ", "")
        return mapSizeUnit(value * count.toDouble(), normalizedRawUnit)
    }

    private fun mapSizeUnit(value: Double, rawUnit: String): Pair<Double, String> = when (rawUnit) {
        "l" -> value to "L"
        "ml" -> value to "mL"
        "gal", "gallon", "gallons" -> (value * 3.785411784) to "L"
        "floz", "fl.oz", "fl-oz" -> (value * 29.5735295625) to "mL"
        else -> value to rawUnit
    }

    // MARK: - Bulk quantity extraction

    private data class BulkQuantityCandidate(
        val count: Int,
        val innerUnit: String,
        val line: String,
        val score: Double,
    )

    private val bulkQuantityRegexes = listOf(
        Regex(
            "(?i)\\b(\\d+)\\s*(pc|pcs|piece|pieces|ea|each|pack|packs|pk|pkt|sachet|sachets|can|cans|roll|rolls|bar|bars|stick|sticks|bottle|bottles|btl|btls|jar|jars|jug|jugs|carton|cartons|ctn|ctns|tub|tubs|pouch|pouches|container|containers|box|boxes|tray|trays|bunch|bunches|dozen|dozens|bundle|bundles|wrap|wraps|bag|bags)\\b",
        ),
        Regex(
            "(?i)\\b(\\d+)\\s*[x×]\\s*(?:\\d+(?:[.,]\\d+)?)?\\s*(kg|kilogram|kilograms|g|gram|grams|lb|pound|pounds|oz|ounce|ounces|ml|milliliter|milliliters|millilitre|millilitres|l|liter|liters|litre|litres|pc|pcs|piece|pieces|ea|each|pack|packs|pk|pkt|sachet|sachets|can|cans|roll|rolls|bar|bars|stick|sticks|bottle|bottles|btl|btls|jar|jars|jug|jugs|carton|cartons|ctn|ctns|tub|tubs|pouch|pouches|container|containers|box|boxes|tray|trays|bunch|bunches|dozen|dozens|bundle|bundles|wrap|wraps|bag|bags)\\b",
        ),
    )

    private fun bestBulkQuantityCandidate(candidates: List<ScannedCandidate>): BulkQuantityCandidate? {
        var best: BulkQuantityCandidate? = null
        for (candidate in candidates) {
            val extracted = extractBulkQuantity(candidate.line) ?: continue
            var score = extracted.third
            score += minOf(candidate.bounds.height, 120f) / 10.0
            score += minOf(candidate.bounds.width, 300f) / 90.0

            val next = BulkQuantityCandidate(
                count = extracted.first,
                innerUnit = extracted.second,
                line = candidate.line,
                score = score,
            )
            val current = best
            best = when {
                current == null -> next
                next.score > current.score -> next
                next.score == current.score && next.count > current.count -> next
                else -> current
            }
        }
        return best
    }

    private fun extractBulkQuantity(line: String): Triple<Int, String, Double>? {
        val normalized = line.lowercase()
        for ((index, regex) in bulkQuantityRegexes.withIndex()) {
            val match = regex.find(normalized) ?: continue
            val rawCount = match.groupValues[1]
            val rawUnit = match.groupValues[2].lowercase()
            val count = rawCount.toIntOrNull() ?: continue
            if (count <= 1) continue
            val innerUnit = discreteUnitAliases[rawUnit] ?: UnitSemantics.canonicalUnit(rawUnit)
            val score = if (index == 0) 14.0 else 12.0
            return Triple(count, innerUnit, score)
        }
        return null
    }

    // MARK: - Discrete unit resolution

    private fun bestDiscreteUnitCandidate(
        candidates: List<ScannedCandidate>,
        preferredLine: String?,
        packageSizeCandidate: PackageSizeCandidate?,
        preferredName: String,
        bulkCandidate: BulkQuantityCandidate?,
        packagingSignals: List<PackagingSignal>,
    ): String? {
        if (bulkCandidate != null) {
            return inferBulkPackagingUnit(
                bulkCandidate = bulkCandidate,
                candidates = candidates,
                preferredName = preferredName,
                packagingSignals = packagingSignals,
            )
        }
        preferredLine?.let { extractDiscreteUnit(it)?.let { unit -> return unit } }
        for (candidate in candidates) {
            extractDiscreteUnit(candidate.line)?.let { return it }
        }
        inferPackagingUnit(candidates, preferredName, packagingSignals)?.let { return it }
        if (packageSizeCandidate != null) return null
        return null
    }

    // MARK: - Packaging signals

    private fun packagingSignalsForResult(
        candidates: List<ScannedCandidate>,
        preferredLine: String?,
        preferredName: String,
        bulkCandidate: BulkQuantityCandidate?,
        packagingSignals: List<PackagingSignal>,
    ): List<PackagingSignal> {
        val merged = ArrayList(packagingSignals)

        if (preferredLine != null) {
            extractDiscreteUnit(preferredLine)?.let { explicit ->
                PackagingSemantics.canonicalPackagingUnit(explicit)?.let { unit ->
                    merged += PackagingSignal.normalized(unit, 0.82, PackagingSignalSource.OCR_EXPLICIT)
                }
            }
        }

        for (candidate in candidates) {
            val explicit = extractDiscreteUnit(candidate.line) ?: continue
            val unit = PackagingSemantics.canonicalPackagingUnit(explicit) ?: continue
            val heightBoost = minOf(candidate.bounds.height.toDouble(), 120.0) / 320.0
            merged += PackagingSignal.normalized(
                unit,
                minOf(0.78, 0.58 + heightBoost),
                PackagingSignalSource.OCR_EXPLICIT,
            )
        }

        inferPackagingUnit(candidates, preferredName, packagingSignals)?.let { inferred ->
            PackagingSemantics.canonicalPackagingUnit(inferred)?.let { unit ->
                merged += PackagingSignal.normalized(unit, 0.56, PackagingSignalSource.HEURISTIC)
            }
        }

        if (bulkCandidate != null) {
            val outerUnit = inferBulkPackagingUnit(
                bulkCandidate = bulkCandidate,
                candidates = candidates,
                preferredName = preferredName,
                packagingSignals = packagingSignals,
            )
            PackagingSemantics.canonicalPackagingUnit(outerUnit)?.let { unit ->
                merged += PackagingSignal.normalized(unit, 0.72, PackagingSignalSource.BULK_HEURISTIC)
            }
        }

        return PackagingSemantics.deduplicatedSignals(merged)
    }

    private fun buildPackagingMetadata(
        selectedUnit: String?,
        bulkCandidate: BulkQuantityCandidate?,
        signals: List<PackagingSignal>,
    ): PackagingScanMetadata {
        val soldAsSignal = PackagingSemantics.strongestSignal(selectedUnit, signals)
        val outerSignal = PackagingSemantics.strongestSignal(
            preferredUnits = PackagingSemantics.outerPackagingPriority,
            signals = signals,
        ) ?: soldAsSignal

        return PackagingScanMetadata(
            soldAsConfidence = soldAsSignal?.confidence,
            outerPackagingUnit = outerSignal?.unit,
            outerPackagingConfidence = outerSignal?.confidence,
            source = outerSignal?.source ?: soldAsSignal?.source,
        )
    }

    // MARK: - Packaging heuristics

    private fun inferPackagingUnit(
        candidates: List<ScannedCandidate>,
        preferredName: String,
        packagingSignals: List<PackagingSignal>,
    ): String? {
        preferredPackagingUnit(packagingSignals)?.let { return it }

        val joined = (listOf(preferredName) + candidates.map { it.line })
            .joinToString(" ")
            .lowercase()
        val padded = " $joined "
        val heuristics = listOf(
            listOf(" jar ", " jars ", " jam ", " jelly ", " salsa ", " pickle ", " mayonnaise ", " mayo ", " peanut butter ") to "jar",
            listOf(" tub ", " tubs ", " yogurt ", " yoghurt ", " ice cream ", " margarine ", " spread ", " dip ", " cream cheese ") to "tub",
            listOf(" pouch ", " pouches ", " refill ") to "pouch",
            listOf(" jug ", " jugs ", " gallon ") to "jug",
            listOf(" carton ", " cartons ", " ctn ", " milk ", " broth ", " stock ", " cream ") to "carton",
            listOf(" bottle ", " bottles ", " btl ", " water ", " soda ", " juice ", " shampoo ", " conditioner ", " vinegar ", " oil ") to "bottle",
            listOf(" bag ", " bags ", " rice ", " flour ", " sugar ", " chips ", " coffee beans ") to "bag",
        )
        for ((keywords, unit) in heuristics) {
            if (keywords.any { padded.contains(it) }) return unit
        }
        return null
    }

    private fun inferBulkPackagingUnit(
        bulkCandidate: BulkQuantityCandidate,
        candidates: List<ScannedCandidate>,
        preferredName: String,
        packagingSignals: List<PackagingSignal>,
    ): String {
        val preferredOuterUnits = listOf("box", "carton", "tray", "bag", "container", "bundle", "wrap")
        PackagingSemantics.strongestSignal(
            preferredUnits = preferredOuterUnits,
            signals = packagingSignals,
        )?.let { return it.unit }

        val joined = (listOf(preferredName, bulkCandidate.line) + candidates.map { it.line })
            .joinToString(" ")
            .lowercase()
        for (unit in preferredOuterUnits) {
            if (joined.contains(" $unit ") || joined.contains(" ${unit}s ")) return unit
        }
        if (bulkCandidate.innerUnit in listOf("box", "carton", "tray", "bag", "container", "bundle", "wrap", "pack")) {
            return bulkCandidate.innerUnit
        }
        return "pack"
    }

    private fun preferredPackagingUnit(signals: List<PackagingSignal>): String? =
        PackagingSemantics.strongestSignal(
            preferredUnits = PackagingSemantics.outerPackagingPriority,
            signals = signals,
        )?.unit

    // MARK: - Name inference

    /**
     * Conservative name picker. iOS ships `name = ""` today (see the TODO in
     * `ProductScanParser.parse`), but the Phase 9 brief asks for a name
     * inference step. Strategy:
     *   1) walk over blocks in top-down order (iOS draws origin at bottom-left
     *      for Vision; our [ScannedTextBlock] uses top-left, so smaller `y`
     *      wins);
     *   2) skip blocks that look like prices / units / package-size numbers;
     *   3) return the first surviving block with at least two letters.
     */
    private fun bestNameCandidate(blocks: List<ScannedTextBlock>, excludedLines: Set<String>): String {
        val ranked = blocks.asSequence()
            .flatMap { block ->
                block.transcript.split('\n', '\r')
                    .asSequence()
                    .map { normalizeText(it) to block.bounds }
                    .filter { it.first.isNotEmpty() }
            }
            .filter { it.first.lowercase() !in excludedLines.map(String::lowercase) }
            .filter { looksLikeNameCandidate(it.first) }
            .sortedWith(
                compareBy({ it.second.top }, { -it.second.height }),
            )
            .toList()

        return ranked.firstOrNull()?.first ?: ""
    }

    private fun looksLikeNameCandidate(line: String): Boolean {
        val letters = line.count { it.isLetter() }
        if (letters < 2) return false
        if (extractPrice(line) != null) return false
        if (unitPriceRegex.containsMatchIn(line)) return false
        if (packageSizeRegex.containsMatchIn(line.lowercase())) return false
        if (discreteUnitQuantityRegex.containsMatchIn(line.lowercase())) return false
        return true
    }
}
