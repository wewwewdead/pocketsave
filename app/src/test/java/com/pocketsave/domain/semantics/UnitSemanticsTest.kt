package com.pocketsave.domain.semantics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression coverage for [UnitSemantics]. iOS parity is critical — a mis-
 * classified unit cascades into wrong planning totals + widget snapshots.
 */
class UnitSemanticsTest {

    @Test
    fun `canonicalUnit folds common spellings`() {
        assertEquals("g", UnitSemantics.canonicalUnit("gram"))
        assertEquals("g", UnitSemantics.canonicalUnit("Grams"))
        assertEquals("kg", UnitSemantics.canonicalUnit("kilograms"))
        assertEquals("L", UnitSemantics.canonicalUnit("liter"))
        assertEquals("mL", UnitSemantics.canonicalUnit("ml"))
        assertEquals("lb", UnitSemantics.canonicalUnit("Pound"))
        assertEquals("oz", UnitSemantics.canonicalUnit("ounces"))
    }

    @Test
    fun `canonicalUnit folds discrete aliases`() {
        assertEquals("pc", UnitSemantics.canonicalUnit("pcs"))
        assertEquals("pc", UnitSemantics.canonicalUnit("piece"))
        assertEquals("pack", UnitSemantics.canonicalUnit("pkt"))
        assertEquals("bottle", UnitSemantics.canonicalUnit("btls"))
        assertEquals("carton", UnitSemantics.canonicalUnit("ctns"))
    }

    @Test
    fun `canonicalUnit returns lowercase for unknown inputs`() {
        assertEquals("widget", UnitSemantics.canonicalUnit("WIDGET"))
        assertEquals("", UnitSemantics.canonicalUnit("   "))
    }

    @Test
    fun `isContinuous matches mass + volume units only`() {
        assertTrue(UnitSemantics.isContinuous("g"))
        assertTrue(UnitSemantics.isContinuous("kg"))
        assertTrue(UnitSemantics.isContinuous("L"))
        assertTrue(UnitSemantics.isContinuous("mL"))
        assertFalse(UnitSemantics.isContinuous("pc"))
        assertFalse(UnitSemantics.isContinuous("pack"))
        assertFalse(UnitSemantics.isContinuous("bag"))
    }

    @Test
    fun `packageSizeUnitOptions prefers count-first units for bag-style sales`() {
        val options = UnitSemantics.packageSizeUnitOptions("pack")
        // First slot should be a count unit (pc) because iOS's
        // `countFirstPackageSoldAsUnits` contains "pack".
        assertEquals("pc", options.first())
    }

    @Test
    fun `packageSizeUnitOptions prefers continuous units for piece-sold items`() {
        val options = UnitSemantics.packageSizeUnitOptions("pc")
        assertEquals("g", options.first())
    }

    @Test
    fun `baseQuantity converts g to kg`() {
        val base = UnitSemantics.baseQuantity(500.0, "g")!!
        assertEquals("kg", base.unitLabel)
        assertEquals(0.5, base.value, 1e-9)
    }

    @Test
    fun `baseQuantity converts lb to kg`() {
        val base = UnitSemantics.baseQuantity(2.0, "lb")!!
        assertEquals("kg", base.unitLabel)
        assertEquals(0.90718474, base.value, 1e-8)
    }

    @Test
    fun `baseQuantity returns null for zero or negative values`() {
        assertNull(UnitSemantics.baseQuantity(0.0, "g"))
        assertNull(UnitSemantics.baseQuantity(-1.0, "kg"))
    }

    @Test
    fun `baseQuantity returns null for unknown units`() {
        assertNull(UnitSemantics.baseQuantity(1.0, "xyzzy"))
    }
}
