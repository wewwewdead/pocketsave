package com.pocketsave.core.scanner.parser

import com.pocketsave.core.scanner.model.ScannedTextBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the OCR → [com.pocketsave.core.scanner.model.ProductScanResult] parser.
 *
 * Cases are constructed as hand-built `ScannedTextBlock` lists so the tests
 * stay pure-JVM (no ML Kit dependency). Each fixture is a plausible shelf
 * label layout; assertions cover price / unit / package-size / bulk-unit
 * behaviour iOS ships today.
 */
class ProductScanParserTest {

    private fun block(text: String, top: Float = 0f, left: Float = 0f, width: Float = 1f, height: Float = 0.1f): ScannedTextBlock =
        ScannedTextBlock(
            transcript = text,
            bounds = ScannedTextBlock.Bounds(left, top, left + width, top + height),
        )

    @Test
    fun `extracts dollar price`() {
        val blocks = listOf(
            block("Peanut Butter", top = 0.10f),
            block("$12.49", top = 0.40f, height = 0.15f),
            block("454 g", top = 0.60f),
        )
        val result = ProductScanParser.parse(blocks)
        assertEquals(12.49, result.price!!, 1e-6)
        assertEquals("g", result.packageSizeUnit)
        assertEquals(454.0, result.packageSizeValue!!, 1e-6)
    }

    @Test
    fun `extracts php price`() {
        val blocks = listOf(
            block("Rice 5 kg", top = 0.10f),
            block("₱285.00", top = 0.40f, height = 0.18f),
        )
        val result = ProductScanParser.parse(blocks)
        assertEquals(285.00, result.price!!, 1e-6)
        assertEquals("kg", result.packageSizeUnit)
        assertEquals(5.0, result.packageSizeValue!!, 1e-6)
    }

    @Test
    fun `detects unit-price per kg sets continuous unit`() {
        val blocks = listOf(
            block("Apples", top = 0.10f),
            block("3.99 / kg", top = 0.40f, height = 0.15f),
        )
        val result = ProductScanParser.parse(blocks)
        assertEquals(3.99, result.price!!, 1e-6)
        assertEquals("kg", result.unit)
        assertNull("unit-price implies no package size", result.packageSizeValue)
    }

    @Test
    fun `maps fluid ounces to milliliters`() {
        val blocks = listOf(
            block("Juice", top = 0.1f),
            block("1.00 fl oz", top = 0.4f),
        )
        val result = ProductScanParser.parse(blocks)
        assertEquals("mL", result.packageSizeUnit)
        // 1 fl oz ≈ 29.5735 mL
        assertEquals(29.5735, result.packageSizeValue!!, 0.01)
    }

    @Test
    fun `multi-pack size multiplies count`() {
        val blocks = listOf(
            block("Water bottles", top = 0.1f),
            block("6 x 500 ml", top = 0.4f),
        )
        val result = ProductScanParser.parse(blocks)
        assertEquals("mL", result.packageSizeUnit)
        assertEquals(3000.0, result.packageSizeValue!!, 1e-6)
    }

    @Test
    fun `bulk quantity picks discrete outer unit`() {
        val blocks = listOf(
            block("Sachets", top = 0.1f),
            block("12 sachets", top = 0.4f),
        )
        val result = ProductScanParser.parse(blocks)
        // 12 sachets → bulk candidate; the inner unit "sachet" isn't in the
        // outer-priority list so the parser falls back to "pack".
        assertNotNull(result.unit)
        assertTrue(result.unit == "pack" || result.unit == "sachet")
    }

    @Test
    fun `discrete unit is picked even without a price`() {
        val blocks = listOf(
            block("Crackers", top = 0.1f),
            block("1 box", top = 0.4f),
        )
        val result = ProductScanParser.parse(blocks)
        assertEquals("box", result.unit)
    }

    @Test
    fun `name candidate picks first readable non-price line`() {
        val blocks = listOf(
            block("Whole Milk", top = 0.05f, height = 0.10f),
            block("$4.29", top = 0.35f, height = 0.15f),
            block("1 L", top = 0.55f),
        )
        val result = ProductScanParser.parse(blocks)
        assertEquals("Whole Milk", result.name)
        assertEquals(4.29, result.price!!, 1e-6)
        assertEquals("L", result.packageSizeUnit)
    }

    @Test
    fun `empty input returns empty result`() {
        val result = ProductScanParser.parse(emptyList())
        assertNull(result.price)
        assertNull(result.unit)
        assertEquals("", result.name)
        assertTrue(result.rawLines.isEmpty())
    }
}
