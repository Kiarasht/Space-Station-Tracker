package com.restart.spacestationtracker.shared.ui

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.dateWithTimeIntervalSince1970

internal actual fun formatLocalizedDate(epochMillis: Long, skeleton: String): String {
    val formatter = NSDateFormatter()
    formatter.setLocalizedDateFormatFromTemplate(skeleton)
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0))
}
