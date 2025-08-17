package com.restart.spacestationtracker.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SightSee(
    val startUTC: Long,
    val endUTC: Long,
    val durationSeconds: Int,
    val magnitude: Double,
    val maxElevation: Double,
    val startAzimuthCompass: String,
    val endAzimuthCompass: String,
    val location: String,
) {
    val riseTimeDate: Date by lazy { Date(startUTC * 1000L) }
    val setTimeDate: Date by lazy { Date(endUTC * 1000L) }

    val formattedRiseTime: String by lazy {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(riseTimeDate)
    }

    val formattedDuration: String by lazy {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        "$minutes min $seconds sec"
    }

    val formattedDateHeader: String by lazy {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(riseTimeDate).uppercase(Locale.getDefault())
    }

    companion object {
        var location: String = ""
    }
}