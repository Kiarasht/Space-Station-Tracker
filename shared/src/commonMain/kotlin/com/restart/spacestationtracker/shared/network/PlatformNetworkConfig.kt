package com.restart.spacestationtracker.shared.network

internal expect object PlatformNetworkConfig {
    val n2yoApiKey: String
    val platformName: String
}
