package com.restart.spacestationtracker.shared.network

data class NetworkConfig(
    val userAgent: String = "ISS-Tracker (restartapplication@gmail.com)",
    val youtubeLiveStreamsUrl: String =
        "https://raw.githubusercontent.com/Kiarasht/Space-Station-Tracker/live-stream-cache/docs/nasa-live-streams.json",
    val youtubeApiKey: String = "",
    val youtubeRequestHeaders: Map<String, String> = emptyMap(),
    val n2yoApiKey: String = PlatformNetworkConfig.n2yoApiKey
)
