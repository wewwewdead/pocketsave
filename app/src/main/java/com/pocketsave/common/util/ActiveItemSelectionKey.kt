package com.pocketsave.common.util

/**
 * Port of `PocketSave/Extensions/ActiveItemSelectionKey.swift`.
 *
 * Encodes the (itemId, store) tuple used by both the vault selection state and
 * cart item tracking into a single string key. Keeping the exact same format
 * (`itemId||store`) means any future shared-data interop with the iOS app stays
 * compatible bit-for-bit.
 */
object ActiveItemSelectionKey {
    private const val SEPARATOR = "||"

    fun make(itemId: String, store: String?): String {
        val trimmed = store?.trim().orEmpty()
        return if (trimmed.isEmpty()) itemId else "$itemId$SEPARATOR$trimmed"
    }

    data class Parsed(val itemId: String, val store: String?)

    fun parse(key: String): Parsed {
        val idx = key.indexOf(SEPARATOR)
        if (idx < 0) return Parsed(key, null)
        val itemId = key.substring(0, idx)
        val raw = key.substring(idx + SEPARATOR.length).trim()
        return Parsed(itemId, raw.ifEmpty { null })
    }

    fun itemId(fromKey: String): String = parse(fromKey).itemId
}
