package com.pocketsave.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ColorOptionTest {

    @Test
    fun `default colour is the second swatch`() {
        // iOS `ColorOption.defaultColor` returns index 1 when > 1 options exist.
        assertEquals(ColorOption.options[1], ColorOption.defaultColor)
    }

    @Test
    fun `palette contains 16 swatches matching iOS`() {
        assertEquals(16, ColorOption.options.size)
        assertEquals("FFFFFF", ColorOption.options.first().hex)
    }

    @Test
    fun `byHex tolerates leading hash and case`() {
        assertNotNull(ColorOption.byHex("ffffff"))
        assertNotNull(ColorOption.byHex("#FFFFFF"))
        assertNull(ColorOption.byHex("not-a-color"))
    }

    @Test
    fun `unique hexes in palette`() {
        val hexes = ColorOption.options.map { it.hex }
        assertEquals(hexes.distinct(), hexes)
    }
}
