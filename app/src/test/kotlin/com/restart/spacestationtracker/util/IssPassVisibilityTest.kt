package com.restart.spacestationtracker.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IssPassVisibilityTest {

    @Test
    fun labelForMagnitude_mapsMagnitudeToVisibilityLabel() {
        assertEquals(IssPassVisibility.VERY_BRIGHT, IssPassVisibility.labelForMagnitude(-2.1))
        assertEquals(IssPassVisibility.BRIGHT, IssPassVisibility.labelForMagnitude(-1.6))
        assertEquals(IssPassVisibility.MODERATE, IssPassVisibility.labelForMagnitude(-1.1))
        assertEquals(IssPassVisibility.FAINT, IssPassVisibility.labelForMagnitude(-0.1))
        assertEquals("Very Faint", IssPassVisibility.labelForMagnitude(0.0))
    }

    @Test
    fun matchesMinimum_includesBrighterPasses() {
        assertTrue(IssPassVisibility.matchesMinimum(-2.1, IssPassVisibility.BRIGHT))
        assertTrue(IssPassVisibility.matchesMinimum(-1.6, IssPassVisibility.BRIGHT))
        assertFalse(IssPassVisibility.matchesMinimum(-1.4, IssPassVisibility.BRIGHT))
    }

    @Test
    fun matchesMinimum_usesStrictThresholds() {
        assertFalse(IssPassVisibility.matchesMinimum(-2.0, IssPassVisibility.VERY_BRIGHT))
        assertFalse(IssPassVisibility.matchesMinimum(-1.5, IssPassVisibility.BRIGHT))
        assertFalse(IssPassVisibility.matchesMinimum(-1.0, IssPassVisibility.MODERATE))
        assertFalse(IssPassVisibility.matchesMinimum(0.0, IssPassVisibility.FAINT))
    }
}
