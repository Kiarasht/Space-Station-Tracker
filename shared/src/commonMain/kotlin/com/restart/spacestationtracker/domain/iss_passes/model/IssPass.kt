package com.restart.spacestationtracker.domain.iss_passes.model

data class IssPass(
    val startTimeMillis: Long,
    val durationInSeconds: Int,
    val magnitude: Double,
    val maxElevation: Double,
    val startAzimuthCompass: String,
    val endAzimuthCompass: String,
    /** Elevation in degrees, sampled evenly from the start to the end of the visible window. */
    val elevationTrack: List<Double> = emptyList()
)
