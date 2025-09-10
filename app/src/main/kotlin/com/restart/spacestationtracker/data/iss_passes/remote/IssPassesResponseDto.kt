package com.restart.spacestationtracker.data.iss_passes.remote

import com.restart.spacestationtracker.domain.iss_passes.model.IssPass
import java.util.Date

data class IssPassesResponseDto(
    val info: InfoDto,
    val passes: List<PassDto>
)

data class InfoDto(
    val satid: Int,
    val satname: String,
    val transactionscount: Int,
    val passescount: Int
)

data class PassDto(
    val startAz: Double,
    val startAzCompass: String,
    val startEl: Double,
    val startUTC: Long,
    val maxAz: Double,
    val maxAzCompass: String,
    val maxEl: Double,
    val maxUTC: Long,
    val endAz: Double,
    val endAzCompass: String,
    val endEl: Double,
    val endUTC: Long,
    val mag: Double,
    val duration: Int
) {
    fun toIssPass(): IssPass {
        return IssPass(
            startTime = Date(startUTC * 1000),
            durationInSeconds = duration,
            magnitude = mag,
            maxElevation = maxEl,
            startAzimuthCompass = startAzCompass,
            endAzimuthCompass = endAzCompass
        )
    }
}
