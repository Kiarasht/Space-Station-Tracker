package com.restart.spacestationtracker.utils

import java.text.DateFormat
import java.util.*

object DateUtils {

    fun formatLaunchDate(timestamp: Long): String {
        val date = Date(timestamp * 1000)
        return DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(date)
    }
}
