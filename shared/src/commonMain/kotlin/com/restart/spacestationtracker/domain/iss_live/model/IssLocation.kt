package com.restart.spacestationtracker.domain.iss_live.model

data class IssLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val velocity: Double,
    val visibility: String,
    val footprint: Double,
    val solarLat: Double,
    val solarLon: Double,
    val timestamp: Long
)
