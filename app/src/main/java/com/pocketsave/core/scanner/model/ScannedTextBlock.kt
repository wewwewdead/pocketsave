package com.pocketsave.core.scanner.model

/**
 * Port of the iOS `ScannedTextBlock` struct referenced from
 * `PocketSave/Components/ProductScannerSheet.swift`.
 *
 * A single block returned from the text recogniser — the raw transcript (may
 * contain embedded newlines) plus the normalised bounding box in image space
 * (0..1 coordinates, top-left origin).
 *
 * The bounds are modelled as a pure-Kotlin value type (not `android.graphics.RectF`)
 * so parser/regression tests run on the JVM without needing Robolectric.
 */
data class ScannedTextBlock(
    val transcript: String,
    /**
     * Bounding box in normalised coordinates (origin top-left, values in 0..1).
     * Width / height multiplied by 100 map into the iOS scoring heuristics that
     * boost "big" text as a price / title candidate.
     */
    val bounds: Bounds,
) {
    data class Bounds(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    ) {
        val width: Float get() = (right - left).coerceAtLeast(0f)
        val height: Float get() = (bottom - top).coerceAtLeast(0f)

        companion object {
            val FULL = Bounds(0f, 0f, 1f, 1f)
        }
    }
}
