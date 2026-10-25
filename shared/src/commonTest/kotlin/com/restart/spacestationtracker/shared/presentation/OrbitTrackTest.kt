package com.restart.spacestationtracker.shared.presentation

import com.restart.spacestationtracker.domain.iss_live.model.IssLocation
import com.restart.spacestationtracker.shared.satellites.SatelliteSnapshot
import com.restart.spacestationtracker.shared.satellites.TrackedSatellite
import com.restart.spacestationtracker.shared.ui.platform.IssMapPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrbitTrackTest {
    @Test
    fun startsWindowAfterNowWhenEmpty() {
        val timestamps = OrbitTrack.nextTimestamps(emptyList(), nowSeconds = 1_000)

        assertEquals(OrbitTrack.BATCH_SIZE, timestamps.size)
        assertEquals(1_060L, timestamps.first())
        assertEquals(1_600L, timestamps.last())
    }

    @Test
    fun continuesFromLastPointWithoutDuplicates() {
        val future = listOf(location(1_060), location(1_120))

        val timestamps = OrbitTrack.nextTimestamps(future, nowSeconds = 1_000)

        assertEquals(1_180L, timestamps.first())
    }

    @Test
    fun requestsNothingWhenWindowIsFull() {
        val full = listOf(location(1_000 + OrbitTrack.WINDOW_SECONDS))

        assertTrue(OrbitTrack.nextTimestamps(full, nowSeconds = 1_000).isEmpty())
    }

    @Test
    fun mergeDropsPastAndDuplicatePointsAndSorts() {
        val merged = OrbitTrack.merge(
            existing = listOf(location(900), location(1_060), location(1_120)),
            incoming = listOf(location(1_180), location(1_120)),
            nowSeconds = 1_000
        )

        assertEquals(listOf(1_060L, 1_120L, 1_180L), merged.map(IssLocation::timestamp))
    }

    @Test
    fun predictionFillsTheWholeLineAtOnce() {
        val prediction = issSnapshot(timestamps = listOf(1_000L, 1_060L, 1_120L, 1_180L))

        val ahead = OrbitTrack.ahead(
            prediction = prediction,
            existing = listOf(location(1_060)),
            incoming = emptyList(),
            nowSeconds = 1_000
        )

        assertEquals(listOf(1_060L, 1_120L, 1_180L), ahead.map(IssLocation::timestamp))
    }

    @Test
    fun withoutPredictionTheApiPointsAreMerged() {
        val merged = OrbitTrack.ahead(
            prediction = null,
            existing = listOf(location(1_060)),
            incoming = listOf(location(1_120)),
            nowSeconds = 1_000
        )
        assertEquals(listOf(1_060L, 1_120L), merged.map(IssLocation::timestamp))
    }

    @Test
    fun predictionIsTheIssSnapshot() {
        val hubble = issSnapshot(listOf(1_060L)).copy(satellite = TrackedSatellite.HUBBLE)
        assertEquals(null, OrbitTrack.prediction(listOf(hubble)))
        assertEquals(TrackedSatellite.ISS, OrbitTrack.prediction(listOf(hubble, issSnapshot(listOf(1_060L))))?.satellite)
    }

    private fun issSnapshot(timestamps: List<Long>) = SatelliteSnapshot(
        satellite = TrackedSatellite.ISS,
        latitude = 0.0,
        longitude = 0.0,
        altitudeKm = 420.0,
        speedKmh = 27_600.0,
        periodMinutes = 92,
        timestampSeconds = timestamps.first(),
        isInEarthShadow = false,
        track = timestamps.map { IssMapPoint(latitude = 0.0, longitude = 0.0, timestamp = it) }
    )

    private fun location(timestamp: Long) = IssLocation(
        latitude = 0.0,
        longitude = 0.0,
        altitude = 420.0,
        velocity = 27_600.0,
        visibility = "daylight",
        footprint = 4_500.0,
        solarLat = 0.0,
        solarLon = 0.0,
        timestamp = timestamp
    )
}
