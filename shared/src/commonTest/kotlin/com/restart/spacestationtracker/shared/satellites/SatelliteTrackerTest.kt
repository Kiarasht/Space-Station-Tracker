package com.restart.spacestationtracker.shared.satellites

import com.restart.spacestationtracker.shared.passes.Tle
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SatelliteTrackerTest {
    // Where the ISS at? published this element set and computed the ISS at 44.3755°, 125.3432°, 427.6 km
    // for 2026-10-19 07:20:00 UTC.
    private val iss = Tle(
        "1 25544U 98067A   26291.78980200  .00007771  00000+0  14938-3 0  9993",
        "2 25544  51.6325  48.4339 0006764 280.4294  79.5932 15.48929442590914"
    )
    private val hubble = Tle(
        "1 20580U 90037B   26292.48537891  .00006862  00000+0  20823-3 0  9999",
        "2 20580  28.4737 310.2311 0002146  99.9540 260.1298 15.32006005807643"
    )
    private val referenceMillis = 1_792_440_000_000L

    @Test
    fun matchesAnIndependentIssPosition() {
        val snapshot = SatelliteTracker.snapshot(TrackedSatellite.ISS, iss, referenceMillis)
        assertClose(44.3755, snapshot.latitude, 0.2, "latitude")
        assertClose(125.3432, snapshot.longitude, 0.2, "longitude")
        assertClose(427.6, snapshot.altitudeKm, 3.0, "altitude")
        assertClose(27_571.0, snapshot.speedKmh, 60.0, "speed")
        assertEquals(93, snapshot.periodMinutes)
        // Where the ISS at? reported it "eclipsed" at that moment.
        assertTrue(snapshot.isInEarthShadow)
    }

    @Test
    fun tracksTwoHoursAheadFromTheCurrentPosition() {
        val snapshot = SatelliteTracker.snapshot(TrackedSatellite.HUBBLE, hubble, referenceMillis)
        assertEquals(TRACK_MINUTES + 1, snapshot.track.size)
        assertEquals(snapshot.latitude, snapshot.track.first().latitude)
        assertEquals(referenceMillis / 1_000L + 60L, snapshot.track[1].timestamp)
        // Hubble's 28.5° inclination keeps it between those latitudes.
        assertTrue(snapshot.track.all { abs(it.latitude) < 29.0 })
        assertTrue(snapshot.altitudeKm in 450.0..560.0, "altitude ${snapshot.altitudeKm}")
    }

    @Test
    fun layerKeepsElementsAndRetriesMissingOnes() = runTest {
        var calls = 0
        val source = object : SatelliteElementsSource {
            override suspend fun satelliteElements(satellites: List<TrackedSatellite>): Map<TrackedSatellite, Tle> {
                calls++
                return if (calls == 1) mapOf(TrackedSatellite.ISS to iss) else mapOf(TrackedSatellite.HUBBLE to hubble)
            }
        }
        val layer = SatelliteLayer(source, listOf(TrackedSatellite.ISS, TrackedSatellite.HUBBLE))
        assertEquals(listOf(TrackedSatellite.ISS), layer.snapshots(referenceMillis).map { it.satellite })
        // Within the retry window nothing is fetched again.
        layer.snapshots(referenceMillis + 5_000L)
        assertEquals(1, calls)
        // After it, the missing satellite is retried and the earlier one is kept.
        val later = layer.snapshots(referenceMillis + 61_000L).map { it.satellite }
        assertEquals(2, calls)
        assertEquals(listOf(TrackedSatellite.ISS, TrackedSatellite.HUBBLE), later)
    }

    private fun assertClose(expected: Double, actual: Double, tolerance: Double, label: String) {
        assertTrue(abs(expected - actual) <= tolerance, "$label: expected $expected ± $tolerance, was $actual")
    }
}
