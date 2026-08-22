package com.restart.spacestationtracker.shared.settings

import com.restart.spacestationtracker.shared.preferences.PreferenceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SharedSettingsRepository(
    private val store: PreferenceStore
) {
    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun setShowOrbit(value: Boolean) = update(
        write = { store.putBoolean(AppSettingsKeys.SHOW_ORBIT, value) },
        transform = { it.copy(showOrbit = value) }
    )

    fun setMapType(value: String) = update(
        write = { store.putString(AppSettingsKeys.MAP_TYPE, value) },
        transform = { it.copy(mapType = value) }
    )

    fun setUnits(value: String) = update(
        write = { store.putString(AppSettingsKeys.UNITS, value) },
        transform = { it.copy(units = value) }
    )

    fun setTheme(value: String) = update(
        write = { store.putString(AppSettingsKeys.THEME, value) },
        transform = { it.copy(theme = value) }
    )

    fun setAutomaticPassAlertsEnabled(value: Boolean) = update(
        write = { store.putBoolean(AppSettingsKeys.AUTO_PASS_ALERTS_ENABLED, value) },
        transform = { it.copy(automaticPassAlertsEnabled = value) }
    )

    fun setAutomaticPassAlertMinVisibility(value: String) = update(
        write = { store.putString(AppSettingsKeys.AUTO_PASS_ALERT_MIN_VISIBILITY, value) },
        transform = { it.copy(automaticPassAlertMinVisibility = value) }
    )

    fun setAutomaticPassAlertNotificationTimes(value: Set<String>) {
        val normalized = value.ifEmpty { defaultAutomaticPassAlertNotificationTimes }
        update(
            write = {
                store.putStringSet(AppSettingsKeys.AUTO_PASS_ALERT_NOTIFICATION_TIMES, normalized)
            },
            transform = { it.copy(automaticPassAlertNotificationTimes = normalized) }
        )
    }

    fun setAutomaticPassAlertLocation(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        locationName: String
    ) = update(
        write = {
            store.putDouble(AppSettingsKeys.AUTO_PASS_ALERT_LATITUDE, latitude)
            store.putDouble(AppSettingsKeys.AUTO_PASS_ALERT_LONGITUDE, longitude)
            store.putDouble(AppSettingsKeys.AUTO_PASS_ALERT_ALTITUDE, altitude)
            store.putString(AppSettingsKeys.AUTO_PASS_ALERT_LOCATION_NAME, locationName)
        },
        transform = {
            it.copy(
                automaticPassAlertLatitude = latitude,
                automaticPassAlertLongitude = longitude,
                automaticPassAlertAltitude = altitude,
                automaticPassAlertLocationName = locationName
            )
        }
    )

    fun setScheduledIds(value: Set<String>) = update(
        write = { store.putStringSet(AppSettingsKeys.AUTO_PASS_ALERT_SCHEDULED_IDS, value) },
        transform = { it.copy(automaticPassAlertScheduledIds = value) }
    )

    fun setLifetimeAdRemoval(value: Boolean) = update(
        write = { store.putBoolean(AppSettingsKeys.LIFETIME_AD_REMOVAL, value) },
        transform = { it.copy(hasLifetimeAdRemoval = value) }
    )

    private fun update(write: () -> Unit, transform: (AppSettings) -> AppSettings) {
        write()
        _settings.update(transform)
    }

    private fun readSettings(): AppSettings {
        return AppSettings(
            minAltitude = store.getInt(AppSettingsKeys.MIN_ALTITUDE)
                ?: defaultAppSettings.minAltitude,
            minMagnitude = store.getInt(AppSettingsKeys.MIN_MAGNITUDE)
                ?: defaultAppSettings.minMagnitude,
            showEvents = store.getBoolean(AppSettingsKeys.SHOW_EVENTS)
                ?: defaultAppSettings.showEvents,
            showOrbit = store.getBoolean(AppSettingsKeys.SHOW_ORBIT)
                ?: defaultAppSettings.showOrbit,
            mapType = store.getString(AppSettingsKeys.MAP_TYPE)
                ?: defaultAppSettings.mapType,
            units = store.getString(AppSettingsKeys.UNITS)
                ?: defaultAppSettings.units,
            theme = store.getString(AppSettingsKeys.THEME)
                ?: defaultAppSettings.theme,
            hasLifetimeAdRemoval = store.getBoolean(AppSettingsKeys.LIFETIME_AD_REMOVAL)
                ?: defaultAppSettings.hasLifetimeAdRemoval,
            automaticPassAlertsEnabled =
                store.getBoolean(AppSettingsKeys.AUTO_PASS_ALERTS_ENABLED)
                    ?: defaultAppSettings.automaticPassAlertsEnabled,
            automaticPassAlertMinVisibility =
                store.getString(AppSettingsKeys.AUTO_PASS_ALERT_MIN_VISIBILITY)
                    ?: defaultAppSettings.automaticPassAlertMinVisibility,
            automaticPassAlertNotificationTimes =
                store.getStringSet(AppSettingsKeys.AUTO_PASS_ALERT_NOTIFICATION_TIMES)
                    ?: defaultAppSettings.automaticPassAlertNotificationTimes,
            automaticPassAlertLatitude =
                store.getDouble(AppSettingsKeys.AUTO_PASS_ALERT_LATITUDE),
            automaticPassAlertLongitude =
                store.getDouble(AppSettingsKeys.AUTO_PASS_ALERT_LONGITUDE),
            automaticPassAlertAltitude =
                store.getDouble(AppSettingsKeys.AUTO_PASS_ALERT_ALTITUDE),
            automaticPassAlertLocationName =
                store.getString(AppSettingsKeys.AUTO_PASS_ALERT_LOCATION_NAME),
            automaticPassAlertScheduledIds =
                store.getStringSet(AppSettingsKeys.AUTO_PASS_ALERT_SCHEDULED_IDS).orEmpty(),
            automaticPassAlertLastSyncTimeMillis =
                store.getLong(AppSettingsKeys.AUTO_PASS_ALERT_LAST_SYNC_TIME),
            automaticPassAlertLastSyncResult =
                store.getString(AppSettingsKeys.AUTO_PASS_ALERT_LAST_SYNC_RESULT),
            automaticPassAlertLastSyncMessage =
                store.getString(AppSettingsKeys.AUTO_PASS_ALERT_LAST_SYNC_MESSAGE)
        )
    }
}
