package com.restart.spacestationtracker.shared.network

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

val spaceStationJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

expect fun createPlatformHttpClient(
    json: Json = spaceStationJson,
    userAgent: String = NetworkConfig().userAgent
): HttpClient
