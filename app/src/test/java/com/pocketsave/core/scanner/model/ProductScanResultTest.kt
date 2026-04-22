package com.pocketsave.core.scanner.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductScanResultTest {

    @Test
    fun `normalizeScannedItemName title-cases all caps input`() {
        assertEquals(
            "Peanut Butter",
            ProductScanResult.normalizeScannedItemName("PEANUT BUTTER"),
        )
    }

    @Test
    fun `normalizeScannedItemName leaves mixed-case names alone`() {
        assertEquals(
            "Peanut Butter",
            ProductScanResult.normalizeScannedItemName("Peanut Butter"),
        )
    }

    @Test
    fun `normalizeScannedItemName trims whitespace`() {
        assertEquals("Milk", ProductScanResult.normalizeScannedItemName("   Milk   "))
    }

    @Test
    fun `normalizeScannedItemName returns empty for blank input`() {
        assertEquals("", ProductScanResult.normalizeScannedItemName("   "))
    }

    @Test
    fun `replacingName normalises while keeping other fields`() {
        val sample = ProductScanResult.EMPTY.copy(price = 3.5, unit = "pc", rawLines = listOf("foo"))
        val renamed = sample.replacingName("CHOCOLATE BAR")
        assertEquals("Chocolate Bar", renamed.name)
        assertEquals(3.5, renamed.price!!, 1e-9)
        assertEquals("pc", renamed.unit)
        assertEquals(listOf("foo"), renamed.rawLines)
    }
}
