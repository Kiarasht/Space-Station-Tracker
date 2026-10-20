package com.restart.spacestationtracker.shared.presentation

import com.restart.spacestationtracker.domain.iss_live.model.IssLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrbitTrackTest {
    @Test
    fun startsWindowAfterNowWhenEmpty() {
        val timestamps = OrbitTrack.nextTimestamps(emptyList(), nowSeconds = 1_000, showOrbit = true)

        assertEquals(OrbitTrack.BATCH_SIZE, timestamps.size)
        assertEquals(1_060L, timestamps.first())
        assertEquals(1_600L, timestamps.last())
    }

    @Test
    fun continuesFromLastPointWithoutDuplicates() {
        val future = listOf(location(1_060), location(1_120))

        val timestamps = OrbitTrack.nextTimestamps(future, nowSeconds = 1_000, showOrbit = true)

        assertEquals(1_180L, timestamps.first())
    }

    @Test
    fun requestsNothingWhenWindowIsFullOrOrbitHidden() {
        val full = listOf(location(1_000 + OrbitTrack.WINDOW_SECONDS))

        assertTrue(OrbitTrack.nextTimestamps(full, nowSeconds = 1_000, showOrbit = true).isEmpty())
        assertTrue(OrbitTrack.nextTimestamps(emptyList(), nowSeconds = 1_000, showOrbit = false).isEmpty())
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
