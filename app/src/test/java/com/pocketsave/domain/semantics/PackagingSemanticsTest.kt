package com.pocketsave.domain.semantics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackagingSemanticsTest {

    @Test
    fun `canonicalPackagingUnit accepts supported units only`() {
        assertEquals("jar", PackagingSemantics.canonicalPackagingUnit("Jar"))
        assertEquals("bag", PackagingSemantics.canonicalPackagingUnit("bags"))
        assertNull(PackagingSemantics.canonicalPackagingUnit("kg"))
        assertNull(PackagingSemantics.canonicalPackagingUnit(null))
    }

    @Test
    fun `clampedConfidence clamps to 0_1`() {
        assertEquals(0.0, PackagingSemantics.clampedConfidence(-0.5)!!, 1e-9)
        assertEquals(1.0, PackagingSemantics.clampedConfidence(1.5)!!, 1e-9)
        assertEquals(0.75, PackagingSemantics.clampedConfidence(0.75)!!, 1e-9)
        assertNull(PackagingSemantics.clampedConfidence(Double.NaN))
        assertNull(PackagingSemantics.clampedConfidence(Double.POSITIVE_INFINITY))
        assertNull(PackagingSemantics.clampedConfidence(null))
    }

    @Test
    fun `deduplicatedSignals keeps the highest confidence per unit_source`() {
        val signals = listOf(
            PackagingSignal.normalized("jar", 0.3, PackagingSignalSource.OCR_EXPLICIT),
            PackagingSignal.normalized("jar", 0.6, PackagingSignalSource.OCR_EXPLICIT),
            PackagingSignal.normalized("jar", 0.4, PackagingSignalSource.HEURISTIC),
        )
        val dedup = PackagingSemantics.deduplicatedSignals(signals)
        assertEquals(2, dedup.size)
        val ocrSignal = dedup.first { it.source == PackagingSignalSource.OCR_EXPLICIT }
        assertEquals(0.6, ocrSignal.confidence, 1e-9)
    }

    @Test
    fun `mergedSignals combines evidence across sources`() {
        val signals = listOf(
            PackagingSignal.normalized("jar", 0.6, PackagingSignalSource.OCR_EXPLICIT),
            PackagingSignal.normalized("jar", 0.4, PackagingSignalSource.HEURISTIC),
        )
        val merged = PackagingSemantics.mergedSignals(signals)
        assertEquals(1, merged.size)
        assertTrue("expected combined > single source", merged.first().confidence > 0.6)
    }

    @Test
    fun `strongestSignal by unit respects deduplication`() {
        val signals = listOf(
            PackagingSignal.normalized("bottle", 0.4, PackagingSignalSource.OCR_EXPLICIT),
            PackagingSignal.normalized("bottle", 0.8, PackagingSignalSource.OCR_EXPLICIT),
        )
        val best = PackagingSemantics.strongestSignal("bottle", signals)
        assertNotNull(best)
        assertTrue(best!!.confidence >= 0.8 - 1e-9)
    }

    @Test
    fun `strongestSignal with preferredUnits honours order`() {
        val signals = listOf(
            PackagingSignal.normalized("bag", 0.9, PackagingSignalSource.OCR_EXPLICIT),
            PackagingSignal.normalized("box", 0.5, PackagingSignalSource.OCR_EXPLICIT),
        )
        val preferred = PackagingSemantics.strongestSignal(listOf("box", "bag"), signals)
        assertNotNull(preferred)
        assertEquals("box", preferred!!.unit)
    }

    @Test
    fun `scan metadata isEmpty reflects content`() {
        assertTrue(PackagingScanMetadata.EMPTY.isEmpty)
        assertFalse(
            PackagingScanMetadata(
                soldAsConfidence = 0.5,
                outerPackagingUnit = "jar",
                outerPackagingConfidence = 0.5,
                source = PackagingSignalSource.OCR_EXPLICIT,
            ).isEmpty,
        )
    }
}
