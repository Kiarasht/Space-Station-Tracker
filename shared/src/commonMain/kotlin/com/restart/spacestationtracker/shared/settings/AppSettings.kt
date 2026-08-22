package com.restart.spacestationtracker.shared.settings

data class AppSettings(
    val minAltitude: Int = 10,
    val minMagnitude: Int = 4,
    val showEvents: Boolean = true,
    val showOrbit: Boolean = true,
    val mapType: String = MAP_TYPE_NORMAL,
    val units: String = UNITS_METRIC,
    val theme: String = THEME_FOLLOW_SYSTEM,
    val hasLifetimeAdRemoval: Boolean = false,
    val automaticPassAlertsEnabled: Boolean = false,
    val automaticPassAlertMinVisibility: String = VISIBILITY_BRIGHT,
    val automaticPassAlertNotificationTimes: Set<String> = DEFAULT_NOTIFICATION_TIMES,
    val automaticPassAlertLatitude: Double? = null,
    val automaticPassAlertLongitude: Double? = null,
    val automaticPassAlertAltitude: Double? = null,
    val automaticPassAlertLocationName: String? = null,
    val automaticPassAlertScheduledIds: Set<String> = emptySet(),
    val automaticPassAlertLastSyncTimeMillis: Long? = null,
    val automaticPassAlertLastSyncResult: String? = null,
    val automaticPassAlertLastSyncMessage: String? = null
) {
    companion object {
        const val MAP_TYPE_NORMAL = "Normal"
        const val UNITS_METRIC = "Metric"
        const val THEME_FOLLOW_SYSTEM = "Follow System"
        const val VISIBILITY_BRIGHT = "Bright"
        val DEFAULT_NOTIFICATION_TIMES = setOf("10 minutes before")
    }
}

val defaultAppSettings = AppSettings()
val defaultAutomaticPassAlertNotificationTimes = AppSettings.DEFAULT_NOTIFICATION_TIMES

object AppSettingsKeys {
    const val MIN_ALTITUDE = "min_altitude"
    const val MIN_MAGNITUDE = "min_magnitude"
    const val SHOW_EVENTS = "show_events"
    const val SHOW_ORBIT = "show_orbit"
    const val MAP_TYPE = "map_type"
    const val UNITS = "units"
    const val THEME = "theme"
    const val LIFETIME_AD_REMOVAL = "ad_removal_lifetime_enabled"
    const val AUTO_PASS_ALERTS_ENABLED = "auto_pass_alerts_enabled"
    const val AUTO_PASS_ALERT_MIN_VISIBILITY = "auto_pass_alert_min_visibility"
    const val AUTO_PASS_ALERT_NOTIFICATION_TIMES = "auto_pass_alert_notification_times"
    const val AUTO_PASS_ALERT_LATITUDE = "auto_pass_alert_latitude"
    const val AUTO_PASS_ALERT_LONGITUDE = "auto_pass_alert_longitude"
    const val AUTO_PASS_ALERT_ALTITUDE = "auto_pass_alert_altitude"
    const val AUTO_PASS_ALERT_LOCATION_NAME = "auto_pass_alert_location_name"
    const val AUTO_PASS_ALERT_SCHEDULED_IDS = "auto_pass_alert_scheduled_ids"
    const val AUTO_PASS_ALERT_LAST_SYNC_TIME = "auto_pass_alert_last_sync_time"
    const val AUTO_PASS_ALERT_LAST_SYNC_RESULT = "auto_pass_alert_last_sync_result"
    const val AUTO_PASS_ALERT_LAST_SYNC_MESSAGE = "auto_pass_alert_last_sync_message"
}
