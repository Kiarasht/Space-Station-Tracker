package com.restart.spacestationtracker.domain.youtube.repository

import com.restart.spacestationtracker.domain.youtube.model.LiveStream
import com.restart.spacestationtracker.domain.youtube.model.ScheduledLiveEvent

interface YouTubeRepository {
    suspend fun getNasaLiveStreams(): List<LiveStream>

    /** NASA's scheduled broadcasts, soonest first. Fails when the published list can't be read. */
    suspend fun getUpcomingLiveEvents(): Result<List<ScheduledLiveEvent>>
}
