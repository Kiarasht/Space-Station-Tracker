package com.restart.spacestationtracker.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    fun formatLaunchDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        val date = Date(timestamp * 1000)
        val formattedDate = sdf.format(date)

        val day = SimpleDateFormat("d", Locale.getDefault()).format(date).toInt()
        return formattedDate.replaceFirst(day.toString(), getDayWithSuffix(day))
    }

    private fun getDayWithSuffix(day: Int): String {
        if (day in 11..13) {
            return "${day}th"
        }
        return when (day % 10) {
            1 -> "${day}st"
            2 -> "${day}nd"
            3 -> "${day}rd"
            else -> "${day}th"
        }
    }
}
