package com.pocketsave.common.util

import androidx.compose.ui.graphics.Color

/**
 * Port of `ColorOption` from `PocketSave/Core/Detail Cart/Views/ModeToggleView.swift`.
 *
 * Mirrors the 16-swatch palette exactly (hex + display name + ordering) so a
 * cart's background persists across platforms with the same visual identity.
 */
data class ColorOption(
    val hex: String,
    val name: String,
) {
    val color: Color get() = parseHex(hex)

    companion object {
        /** 16 swatches in the same order the iOS picker renders. */
        val options: List<ColorOption> = listOf(
            ColorOption("FFFFFF", "White"),
            ColorOption("F5E9D9", "Warm Beige"),
            ColorOption("FFE6E6", "Blush Pink"),
            ColorOption("FFE8CC", "Peach"),
            ColorOption("FFF5CC", "Butter Yellow"),
            ColorOption("FFEB99", "Lemon"),
            ColorOption("FFD8B8", "Apricot"),
            ColorOption("D6EDFF", "Sky Blue"),
            ColorOption("EDE6FF", "Lavender"),
            ColorOption("E3F7CD", "Mint Green"),
            ColorOption("E0F7FA", "Ice Blue"),
            ColorOption("F0E6FF", "Lilac"),
            ColorOption("D4F0C1", "Pear Green"),
            ColorOption("F5F0E6", "Oatmeal"),
            ColorOption("E8F4F8", "Morning Mist"),
            ColorOption("F0F0F0", "Cloud Gray"),
        )

        /** Matches iOS: `options[1]` when available, else `options[0]`. */
        val defaultColor: ColorOption get() = options.getOrNull(1) ?: options[0]

        fun byHex(hex: String): ColorOption? =
            options.firstOrNull { it.hex.equals(hex.removePrefix("#"), ignoreCase = true) }

        fun parseHex(hex: String): Color {
            val cleaned = hex.trim().removePrefix("#")
            if (cleaned.length != 6) return Color.White
            return runCatching {
                val value = cleaned.toLong(16)
                val r = ((value shr 16) and 0xFF) / 255f
                val g = ((value shr 8) and 0xFF) / 255f
                val b = (value and 0xFF) / 255f
                Color(red = r, green = g, blue = b, alpha = 1f)
            }.getOrElse { Color.White }
        }

        /**
         * Deterministic pastel for a given cart id — matches iOS
         * `ColorOption.getBackgroundColor(for:isRow:)`. Uses the cart id hash
         * to pick a swatch so the same cart always gets the same color.
         * Skips index 0 (White) so rows always have a visible tint.
         */
        fun getBackgroundColor(cartId: String, isRow: Boolean = true): Color {
            if (cartId.isEmpty()) return options[1].color
            val candidates = if (isRow) options.drop(1) else options
            val idx = (cartId.hashCode().rem(candidates.size).let { if (it < 0) it + candidates.size else it })
            return candidates[idx].color
        }
    }
}
