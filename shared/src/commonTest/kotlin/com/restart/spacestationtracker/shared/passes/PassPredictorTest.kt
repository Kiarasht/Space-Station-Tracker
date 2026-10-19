package com.restart.spacestationtracker.shared.passes

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Sgp4Test {
    // Vallado et al. (2006) verification case for satellite 00005 (tcppver.out), WGS-72.
    private val tle = Tle(
        "1 00005U 58002B   00179.78495062  .00000023  00000-0  28098-4 0  4753",
        "2 00005  34.2682 348.7242 1859667 331.7664  19.3264 10.82419157413667"
    )

    @Test
    fun matchesReferenceStateAtEpoch() {
        val state = Sgp4(tle).propagateMinutes(0.0)
        assertClose(7022.46529266, state.x, 1.0e-4)
        assertClose(-1400.08296755, state.y, 1.0e-4)
        assertClose(0.03995155, state.z, 1.0e-4)
        assertClose(1.893841015, state.vx, 1.0e-7)
        assertClose(6.405893759, state.vy, 1.0e-7)
        assertClose(4.534807250, state.vz, 1.0e-7)
    }

    @Test
    fun matchesReferenceStateSixHoursLater() {
        val state = Sgp4(tle).propagateMinutes(360.0)
        assertClose(-7154.03120202, state.x, 1.0e-4)
        assertClose(-3783.17682504, state.y, 1.0e-4)
        assertClose(-3536.19412294, state.z, 1.0e-4)
        assertClose(4.741887409, state.vx, 1.0e-7)
        assertClose(-4.151817765, state.vy, 1.0e-7)
        assertClose(-2.093935425, state.vz, 1.0e-7)
    }

    @Test
    fun parsesEpochAndDragTerm() {
        // Day 179.78495062 of 2000 = 2000-06-27 18:50:19.733 UTC.
        assertClose(962_131_819_733.0, tle.epochMillis.toDouble(), 1.0)
        assertClose(0.28098e-4, tle.bstar, 1.0e-12)
    }

    private fun assertClose(expected: Double, actual: Double, tolerance: Double) {
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected but was $actual (±$tolerance)")
    }
}

class PassPredictorTest {
    // N2YO's ISS element set and its visual-pass predictions for San Francisco (37.7749, -122.4194, 0 m),
    // both captured on 2026-10-17 with the same 10-day window and 300-second minimum visibility.
    private val tle = Tle(
        "1 25544U 98067A   26289.78971908  .00007803  00000-0  15012-3 0  9996",
        "2 25544  51.6326  58.3278 0006748 271.9059  88.1156 15.48899044590607"
    )
    private val observer = Observer(latitude = 37.7749, longitude = -122.4194, altitudeMeters = 0.0)
    private val fetchedAtMillis = 1_792_224_254_000L

    // N2YO's startUTC is an earlier reference point, but end - duration is when the ISS becomes visible,
    // which is what the app reports as the pass start.
    private class ReferencePass(
        val endSeconds: Long,
        val visibleSeconds: Int,
        val maxElevation: Double,
        val endCompass: String,
        val magnitude: Double
    ) {
        val visibleStartSeconds = endSeconds - visibleSeconds
    }

    private val reference = listOf(
        ReferencePass(1_792_245_210, 465, 21.2, "NE", -0.5),
        ReferencePass(1_792_328_770, 330, 30.6, "NE", -1.1),
        ReferencePass(1_792_418_130, 420, 12.1, "NE", 0.2),
        // N2YO keeps this pass visible until the Sun is 5.6° below the horizon although it applies 6° to
        // the others, so it ends about two minutes later than here.
        ReferencePass(1_792_936_985, 490, 10.3, "ENE", 0.4)
    )

    @Test
    fun findsTheSamePassesAsReference() {
        val passes = PassPredictor.visiblePasses(tle, observer, fetchedAtMillis)

        assertEquals(reference.size, passes.size, "pass count")
        passes.zip(reference).forEachIndexed { index, (pass, expected) ->
            val label = "pass ${index + 1}"
            val startSeconds = pass.startTimeMillis / 1_000
            assertTrue(abs(pass.maxElevation - expected.maxElevation) <= 0.5, "$label max elevation ${pass.maxElevation}")
            assertTrue(abs(pass.magnitude - expected.magnitude) <= 0.5, "$label magnitude ${pass.magnitude}")
            if (index < 3) {
                assertTrue(abs(startSeconds - expected.visibleStartSeconds) <= 15, "$label start $startSeconds")
                assertTrue(abs(startSeconds + pass.durationInSeconds - expected.endSeconds) <= 15, "$label end")
                assertTrue(abs(pass.durationInSeconds - expected.visibleSeconds) <= 15, "$label duration ${pass.durationInSeconds}")
                assertEquals(expected.endCompass, pass.endAzimuthCompass, "$label end direction")
            } else {
                assertTrue(abs(startSeconds - expected.visibleStartSeconds) <= 60, "$label start $startSeconds")
            }
        }
    }

    @Test
    fun requiresADarkSky() {
        // The ISS still crosses the sky, but no moment satisfies an impossible darkness limit.
        val none = PassPredictor.visiblePasses(
            tle,
            observer,
            fetchedAtMillis,
            PassPredictionSettings(days = 10, maximumSunElevation = -90.0)
        )
        assertTrue(none.isEmpty())
    }

    @Test
    fun compassPointsCoverAllSixteenDirections() {
        assertEquals("N", compassPoint(0.0))
        assertEquals("N", compassPoint(359.0))
        assertEquals("NNE", compassPoint(22.5))
        assertEquals("E", compassPoint(90.0))
        assertEquals("SSW", compassPoint(202.5))
        assertEquals("WNW", compassPoint(292.5))
    }
}
