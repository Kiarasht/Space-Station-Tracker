package com.restart.spacestationtracker.shared.presentation

import com.restart.spacestationtracker.domain.iss_live.model.IssLocation

/**
 * Keeps the predicted orbit as a rolling window of one-minute points ahead of "now".
 *
 * Past points are dropped on every merge and the window is topped up in small batches, so the
 * orbit line always starts at the station and keeps extending no matter how long the map is open.
 */
object OrbitTrack {
    const val WINDOW_SECONDS = 135L * 60L
    const val STEP_SECONDS = 60L
    const val BATCH_SIZE = 10

    /** Timestamps (epoch seconds) to request next, or an empty list when the window is full. */
    fun nextTimestamps(
        future: List<IssLocation>,
        nowSeconds: Long,
        showOrbit: Boolean
    ): List<Long> {
        if (!showOrbit) return emptyList()
        val windowEnd = nowSeconds + WINDOW_SECONDS
        val lastTimestamp = future.lastOrNull()?.timestamp?.takeIf { it > nowSeconds } ?: nowSeconds
        if (lastTimestamp >= windowEnd) return emptyList()
        return (1..BATCH_SIZE)
            .map { step -> lastTimestamp + step * STEP_SECONDS }
            .filter { it <= windowEnd + STEP_SECONDS }
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
