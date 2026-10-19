package com.restart.spacestationtracker.shared.ui

import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal actual fun formatLocalizedDate(epochMillis: Long, skeleton: String): String {
    val locale = Locale.getDefault()
    return SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
        .format(Date(epochMillis))
}
