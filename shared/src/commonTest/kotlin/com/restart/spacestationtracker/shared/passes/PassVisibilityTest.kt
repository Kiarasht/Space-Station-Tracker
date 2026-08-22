package com.restart.spacestationtracker.shared.passes

import kotlin.test.Test
import kotlin.test.assertEquals

class PassVisibilityTest {
    @Test
    fun classifiesMagnitudeUsingExistingAndroidThresholds() {
        assertEquals(PassVisibility.VERY_BRIGHT, PassVisibility.fromMagnitude(-2.01))
        assertEquals(PassVisibility.BRIGHT, PassVisibility.fromMagnitude(-2.0))
        assertEquals(PassVisibility.MODERATE, PassVisibility.fromMagnitude(-1.5))
        assertEquals(PassVisibility.FAINT, PassVisibility.fromMagnitude(-1.0))
        assertEquals(PassVisibility.VERY_FAINT, PassVisibility.fromMagnitude(0.0))
    }
}
