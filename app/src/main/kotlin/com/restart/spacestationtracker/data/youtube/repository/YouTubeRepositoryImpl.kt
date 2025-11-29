package com.restart.spacestationtracker.data.youtube.repository

import android.util.Log
import com.restart.spacestationtracker.BuildConfig
import com.restart.spacestationtracker.data.youtube.remote.YouTubeApiService
import com.restart.spacestationtracker.domain.youtube.model.LiveStream
import com.restart.spacestationtracker.domain.youtube.repository.YouTubeRepository
import javax.inject.Inject

const val NASA_CHANNEL_ID = "UCLA_DiR1FfKNvjuUpBHmylQ"

class YouTubeRepositoryImpl @Inject constructor(
    private val youTubeApiService: YouTubeApiService
) : YouTubeRepository {
    override suspend fun getNasaLiveStreams(): List<LiveStream> {
        val apiKey = BuildConfig.YOUTUBE_API_KEY
        return try {
            val response = youTubeApiService.searchLiveStreams(
                channelId = NASA_CHANNEL_ID,
                apiKey = apiKey
            )
            response.items.map { video ->
                LiveStream(
                    videoId = video.id.videoId,
                    title = video.snippet.title
                )
            }
        } catch (e: Exception) {
            Log.e("YouTubeRepository", "Error fetching live streams", e)
            emptyList()
        }
    }
}
