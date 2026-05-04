package com.restart.spacestationtracker.data.youtube.remote

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Url

@Keep
interface NasaLiveStreamsApiService {
    @GET
    suspend fun getNasaLiveStreams(@Url url: String): NasaLiveStreamsResponse
}

@Keep
data class NasaLiveStreamsResponse(
    @SerializedName("streams")
    val streams: List<NasaLiveStreamDto>
)

@Keep
data class NasaLiveStreamDto(
    @SerializedName("videoId")
    val videoId: String,
    @SerializedName("title")
    val title: String
)
