package com.restart.spacestationtracker.data.iss_live.remote

import com.google.gson.annotations.SerializedName
import com.restart.spacestationtracker.domain.iss_live.model.IssLocation

data class IssLocationDto(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val velocity: Double,
    val visibility: String,
    val footprint: Double,
    @SerializedName("solar_lat") val solarLat: Double,
    @SerializedName("solar_lon") val solarLon: Double,
    val timestamp: Long,
    val units: String
) {
    fun toIssLocation(): IssLocation {
        return IssLocation(
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            velocity = velocity,
            visibility = visibility,
            footprint = footprint,
            solarLat = solarLat,
            solarLon = solarLon,
            timestamp = timestamp
        )
    }
}
