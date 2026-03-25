package com.restart.spacestationtracker.domain.youtube.repository

import com.restart.spacestationtracker.domain.youtube.model.LiveStream

interface YouTubeRepository {
    suspend fun getNasaLiveStreams(): List<LiveStream>
}
