package com.restart.spacestationtracker.data.youtube.remote

import androidx.annotation.Keep
import retrofit2.http.GET
import retrofit2.http.Query

@Keep
interface YouTubeApiService {
    @GET("youtube/v3/search")
    suspend fun searchLiveStreams(
        @Query("part") part: String = "snippet",
        @Query("channelId") channelId: String,
        @Query("eventType") eventType: String = "live",
        @Query("type") type: String = "video",
        @Query("key") apiKey: String
    ): YouTubeSearchResponse
}
