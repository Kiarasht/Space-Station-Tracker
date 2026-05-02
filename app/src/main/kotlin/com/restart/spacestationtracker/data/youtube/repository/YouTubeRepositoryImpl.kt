package com.restart.spacestationtracker.data.youtube.repository

import android.util.Log
import com.restart.spacestationtracker.BuildConfig
import com.restart.spacestationtracker.data.youtube.remote.YouTubeApiService
import com.restart.spacestationtracker.domain.youtube.model.LiveStream
import com.restart.spacestationtracker.domain.youtube.repository.YouTubeRepository
import retrofit2.HttpException
import javax.inject.Inject

const val NASA_CHANNEL_ID = "UCLA_DiR1FfKNvjuUpBHmylQ"

class YouTubeRepositoryImpl @Inject constructor(
    private val youTubeApiService: YouTubeApiService
) : YouTubeRepository {

    private var cachedLiveStreams: List<LiveStream>? = null
    private var cacheTimestampMillis: Long = 0L

    override suspend fun getNasaLiveStreams(): List<LiveStream> {
        val now = System.currentTimeMillis()
        cachedLiveStreams?.takeIf { now - cacheTimestampMillis < CACHE_DURATION_MILLIS }?.let {
            return it
        }

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
            }.also { liveStreams ->
                cachedLiveStreams = liveStreams
                cacheTimestampMillis = now
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(
                "YouTubeRepository",
                "Error fetching live streams: HTTP ${e.code()} ${errorBody.orEmpty()}",
                e
            )
            cachedLiveStreams = emptyList()
            cacheTimestampMillis = now
            emptyList()
        } catch (e: Exception) {
            Log.e("YouTubeRepository", "Error fetching live streams", e)
            cachedLiveStreams = emptyList()
            cacheTimestampMillis = now
            emptyList()
        }
    }

    private companion object {
        const val CACHE_DURATION_MILLIS = 30L * 60 * 1000
    }
}
