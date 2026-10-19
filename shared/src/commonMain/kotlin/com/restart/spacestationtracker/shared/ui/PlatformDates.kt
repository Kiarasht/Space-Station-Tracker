package com.restart.spacestationtracker.shared.ui

/**
 * Formats [epochMillis] in the device's time zone and language from a date-format skeleton such as
 * "EEEMMMd" (Sun, Oct 19) or "jmm" (6:48 AM, or 06:48 where 24-hour time is the norm).
 */
internal expect fun formatLocalizedDate(epochMillis: Long, skeleton: String): String
