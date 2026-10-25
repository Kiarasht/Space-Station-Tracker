package com.restart.spacestationtracker.shared.presentation

import com.restart.spacestationtracker.domain.iss_live.model.IssLocation
import com.restart.spacestationtracker.shared.satellites.SatelliteSnapshot
import com.restart.spacestationtracker.shared.satellites.TrackedSatellite

/**
 * Keeps the predicted orbit as a rolling window of one-minute points ahead of "now".
 *
 * The line normally comes whole from the on-device prediction (see [ahead]). Until the ISS's orbital
 * elements have loaded, it falls back to the position API, which returns at most 10 points per request:
 * past points are dropped on every merge and the window is topped up in small batches.
 */
object OrbitTrack {
    const val WINDOW_SECONDS = 135L * 60L
    const val STEP_SECONDS = 60L
    const val BATCH_SIZE = 10

    /** Timestamps (epoch seconds) to request next, or an empty list when the window is full. */
    fun nextTimestamps(
        future: List<IssLocation>,
        nowSeconds: Long
    ): List<Long> {
        val windowEnd = nowSeconds + WINDOW_SECONDS
        val lastTimestamp = future.lastOrNull()?.timestamp?.takeIf { it > nowSeconds } ?: nowSeconds
        if (lastTimestamp >= windowEnd) return emptyList()
        return (1..BATCH_SIZE)
            .map { step -> lastTimestamp + step * STEP_SECONDS }
            .filter { it <= windowEnd + STEP_SECONDS }
    }

    /**
     * The ISS's predicted path from the on-device satellite layer, or null until its elements have loaded.
     * When present, the API is asked only for the current position.
     */
    fun prediction(satellites: List<SatelliteSnapshot>): SatelliteSnapshot? =
        satellites.firstOrNull { it.satellite == TrackedSatellite.ISS }

    /**
     * The orbit line ahead of now: the whole on-device prediction at once when there is one (so it appears
     * with the map, like the other spacecraft), otherwise the API points fetched so far merged in.
     */
    fun ahead(
        prediction: SatelliteSnapshot?,
        existing: List<IssLocation>,
        incoming: List<IssLocation>,
        nowSeconds: Long
    ): List<IssLocation> = when {
        prediction != null -> prediction.track
            .filter { it.timestamp > nowSeconds }
            // Only position and time are drawn.
            .map { point ->
                IssLocation(
                    latitude = point.latitude,
                    longitude = point.longitude,
                    altitude = prediction.altitudeKm,
                    velocity = prediction.speedKmh,
                    visibility = "",
                    footprint = 0.0,
                    solarLat = 0.0,
                    solarLon = 0.0,
                    timestamp = point.timestamp
                )
            }
        else -> merge(existing, incoming, nowSeconds)
    }

    /** Merges newly fetched points into the window, dropping past and duplicate timestamps. */
    fun merge(
        existing: List<IssLocation>,
        incoming: List<IssLocation>,
        nowSeconds: Long
    ): List<IssLocation> {
        return (existing + incoming)
            .filter { it.timestamp > nowSeconds }
            .distinctBy(IssLocation::timestamp)
            .sortedBy(IssLocation::timestamp)
    }
}
