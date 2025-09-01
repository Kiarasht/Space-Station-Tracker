package com.restart.spacestationtracker.domain.iss_passes.model

import java.util.Date

data class IssPass(
    val startTime: Date,
    val durationInSeconds: Int,
    val magnitude: Double,
    val maxElevation: Double,
    val startAzimuthCompass: String,
    val endAzimuthCompass: String
)
