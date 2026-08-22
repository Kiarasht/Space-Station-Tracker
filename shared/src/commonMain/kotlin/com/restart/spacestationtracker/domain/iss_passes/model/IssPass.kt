package com.restart.spacestationtracker.domain.iss_passes.model

data class IssPass(
    val startTimeMillis: Long,
    val durationInSeconds: Int,
    val magnitude: Double,
    val maxElevation: Double,
    val startAzimuthCompass: String,
    val endAzimuthCompass: String
)
