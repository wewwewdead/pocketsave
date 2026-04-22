package com.pocketsave.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveItemSelectionKeyTest {

    @Test
    fun `make appends store with the double-pipe separator`() {
        val key = ActiveItemSelectionKey.make("item-1", "Trader Joe's")
        assertEquals("item-1||Trader Joe's", key)
    }

    @Test
    fun `make falls back to plain itemId when store is null or blank`() {
        assertEquals("item-1", ActiveItemSelectionKey.make("item-1", null))
        assertEquals("item-1", ActiveItemSelectionKey.make("item-1", "  "))
    }

    @Test
    fun `parse recovers the pair`() {
        val parsed = ActiveItemSelectionKey.parse("item-1||Whole Foods")
        assertEquals("item-1", parsed.itemId)
        assertEquals("Whole Foods", parsed.store)
    }

    @Test
    fun `parse falls back when the separator is missing`() {
        val parsed = ActiveItemSelectionKey.parse("legacy-id")
        assertEquals("legacy-id", parsed.itemId)
        assertNull(parsed.store)
    }

    @Test
    fun `itemId extracts cleanly for both formats`() {
        assertEquals("id", ActiveItemSelectionKey.itemId(fromKey = "id"))
        assertEquals("id", ActiveItemSelectionKey.itemId(fromKey = "id||store"))
    }
}
