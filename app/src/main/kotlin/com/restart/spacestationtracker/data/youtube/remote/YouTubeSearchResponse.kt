package com.restart.spacestationtracker.data.youtube.remote

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class YouTubeSearchResponse(
    @SerializedName("items")
    val items: List<YouTubeVideo>
)

@Keep
data class YouTubeVideo(
    @SerializedName("id")
    val id: VideoId,
    @SerializedName("snippet")
    val snippet: Snippet
)

@Keep
data class VideoId(
    @SerializedName("videoId")
    val videoId: String
)

@Keep
data class Snippet(
    @SerializedName("title")
    val title: String
)
