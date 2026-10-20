package com.restart.spacestationtracker.shared.ui

import com.restart.spacestationtracker.shared.passes.Observer
import com.restart.spacestationtracker.shared.passes.PassPredictor
import com.restart.spacestationtracker.shared.passes.Tle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SkyPathChartTest {
    private val tle = Tle(
        "1 25544U 98067A   26289.78971908  .00007803  00000-0  15012-3 0  9996",
        "2 25544  51.6326  58.3278 0006748 271.9059  88.1156 15.48899044590607"
    )
    private val observers = listOf(
        Observer(latitude = 37.7749, longitude = -122.4194, altitudeMeters = 0.0), // San Francisco
        Observer(latitude = 47.6062, longitude = -122.3321, altitudeMeters = 0.0) // Seattle
    )
    private val startMillis = 1_792_224_254_000L

    @Test
    fun realPassesDrawAsSmoothArcsInsideTheDome() {
        val passes = observers.flatMap { PassPredictor.visiblePasses(tle, it, startMillis) }
        assertTrue(passes.isNotEmpty())
        for (pass in passes) {
            assertEquals(pass.elevationTrack.size, pass.azimuthTrack.size)
            val points = skyChartPoints(pass.elevationTrack, pass.azimuthTrack)
            val label = "pass at ${pass.startTimeMillis} (peak ${pass.maxElevation}°)"
            // Starts on the left (its start direction) and ends on the right.
            assertTrue(points.first().x < 0.0, "$label starts at x=${points.first().x}")
            assertTrue(points.last().x > 0.0, "$label ends at x=${points.last().x}")
            for ((index, point) in points.withIndex()) {
                assertTrue(point.x * point.x + point.height * point.height <= 1.0 + 1e-9, "$label leaves the dome at $index")
                // Never doubles back: the old time-based layout drew a hook after an early peak.
                if (index > 0) assertTrue(point.x >= points[index - 1].x - 0.01, "$label turns back at $index")
            }
        }
    }

    @Test
    fun withoutDirectionsItSpacesSamplesInTime() {
        val points = skyChartPoints(listOf(0.0, 45.0, 0.0), emptyList())
        assertEquals(-1.0, points.first().x, 1e-9)
        assertEquals(0.0, points[1].x, 1e-9)
        assertEquals(1.0, points.last().x, 1e-9)
    }
}
