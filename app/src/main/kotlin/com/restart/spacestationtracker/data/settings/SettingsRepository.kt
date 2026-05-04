package com.restart.spacestationtracker.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.restart.spacestationtracker.util.IssPassVisibility
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val minAltitude: Int,
    val minMagnitude: Int,
    val showEvents: Boolean,
    val showOrbit: Boolean,
    val mapType: String,
    val units: String,
    val theme: String,
    val adFreeExpiry: Long,
    val automaticPassAlertsEnabled: Boolean,
    val automaticPassAlertMinVisibility: String,
    val automaticPassAlertNotificationTimes: Set<String>,
    val automaticPassAlertLatitude: Double?,
    val automaticPassAlertLongitude: Double?,
    val automaticPassAlertAltitude: Double?,
    val automaticPassAlertLocationName: String?,
    val automaticPassAlertScheduledIds: Set<String>
)

val defaultAutomaticPassAlertNotificationTimes = setOf("10 minutes before")

val defaultAppSettings = AppSettings(
    minAltitude = 10,
    minMagnitude = 4,
    showEvents = true,
    showOrbit = true,
    mapType = "Normal",
    units = "Metric",
    theme = "Follow System",
    adFreeExpiry = 0L,
    automaticPassAlertsEnabled = false,
    automaticPassAlertMinVisibility = IssPassVisibility.BRIGHT,
    automaticPassAlertNotificationTimes = defaultAutomaticPassAlertNotificationTimes,
    automaticPassAlertLatitude = null,
    automaticPassAlertLongitude = null,
    automaticPassAlertAltitude = null,
    automaticPassAlertLocationName = null,
    automaticPassAlertScheduledIds = emptySet()
)


@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext context: Context) {

    private val dataStore = context.dataStore
    private val _adFreeExpiryFlow = MutableStateFlow(0L)

    private object Keys {
        val MIN_ALTITUDE = intPreferencesKey("min_altitude")
        val MIN_MAGNITUDE = intPreferencesKey("min_magnitude")
        val SHOW_EVENTS = booleanPreferencesKey("show_events")
        val SHOW_ORBIT = booleanPreferencesKey("show_orbit")
        val MAP_TYPE = stringPreferencesKey("map_type")
        val UNITS = stringPreferencesKey("units")
        val THEME = stringPreferencesKey("theme")
        val AUTO_PASS_ALERTS_ENABLED = booleanPreferencesKey("auto_pass_alerts_enabled")
        val AUTO_PASS_ALERT_MIN_VISIBILITY = stringPreferencesKey("auto_pass_alert_min_visibility")
        val AUTO_PASS_ALERT_NOTIFICATION_TIMES = stringSetPreferencesKey("auto_pass_alert_notification_times")
        val AUTO_PASS_ALERT_LATITUDE = doublePreferencesKey("auto_pass_alert_latitude")
        val AUTO_PASS_ALERT_LONGITUDE = doublePreferencesKey("auto_pass_alert_longitude")
        val AUTO_PASS_ALERT_ALTITUDE = doublePreferencesKey("auto_pass_alert_altitude")
        val AUTO_PASS_ALERT_LOCATION_NAME = stringPreferencesKey("auto_pass_alert_location_name")
        val AUTO_PASS_ALERT_SCHEDULED_IDS = stringSetPreferencesKey("auto_pass_alert_scheduled_ids")
    }

    val appSettingsFlow: Flow<AppSettings> = combine(
        dataStore.data.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        },
        _adFreeExpiryFlow
    ) { preferences, adFreeExpiry ->
        val minAltitude = preferences[Keys.MIN_ALTITUDE] ?: defaultAppSettings.minAltitude
        val minMagnitude = preferences[Keys.MIN_MAGNITUDE] ?: defaultAppSettings.minMagnitude
        val showEvents = preferences[Keys.SHOW_EVENTS] ?: defaultAppSettings.showEvents
        val showOrbit = preferences[Keys.SHOW_ORBIT] ?: defaultAppSettings.showOrbit
        val mapType = preferences[Keys.MAP_TYPE] ?: defaultAppSettings.mapType
        val units = preferences[Keys.UNITS] ?: defaultAppSettings.units
        val theme = preferences[Keys.THEME] ?: defaultAppSettings.theme
        val automaticPassAlertsEnabled =
            preferences[Keys.AUTO_PASS_ALERTS_ENABLED] ?: defaultAppSettings.automaticPassAlertsEnabled
        val automaticPassAlertMinVisibility =
            preferences[Keys.AUTO_PASS_ALERT_MIN_VISIBILITY] ?: defaultAppSettings.automaticPassAlertMinVisibility
        val automaticPassAlertNotificationTimes =
            preferences[Keys.AUTO_PASS_ALERT_NOTIFICATION_TIMES] ?: defaultAppSettings.automaticPassAlertNotificationTimes
        val automaticPassAlertLatitude = preferences[Keys.AUTO_PASS_ALERT_LATITUDE]
        val automaticPassAlertLongitude = preferences[Keys.AUTO_PASS_ALERT_LONGITUDE]
        val automaticPassAlertAltitude = preferences[Keys.AUTO_PASS_ALERT_ALTITUDE]
        val automaticPassAlertLocationName = preferences[Keys.AUTO_PASS_ALERT_LOCATION_NAME]
        val automaticPassAlertScheduledIds =
            preferences[Keys.AUTO_PASS_ALERT_SCHEDULED_IDS] ?: defaultAppSettings.automaticPassAlertScheduledIds
        
        AppSettings(
            minAltitude = minAltitude,
            minMagnitude = minMagnitude,
            showEvents = showEvents,
            showOrbit = showOrbit,
            mapType = mapType,
            units = units,
            theme = theme,
            adFreeExpiry = adFreeExpiry,
            automaticPassAlertsEnabled = automaticPassAlertsEnabled,
            automaticPassAlertMinVisibility = automaticPassAlertMinVisibility,
            automaticPassAlertNotificationTimes = automaticPassAlertNotificationTimes,
            automaticPassAlertLatitude = automaticPassAlertLatitude,
            automaticPassAlertLongitude = automaticPassAlertLongitude,
            automaticPassAlertAltitude = automaticPassAlertAltitude,
            automaticPassAlertLocationName = automaticPassAlertLocationName,
            automaticPassAlertScheduledIds = automaticPassAlertScheduledIds
        )
    }

    suspend fun setMapType(value: String) {
        dataStore.edit { preferences ->
            preferences[Keys.MAP_TYPE] = value
        }
    }

    suspend fun setUnits(value: String) {
        dataStore.edit { preferences ->
            preferences[Keys.UNITS] = value
        }
    }

    suspend fun setTheme(value: String) {
        dataStore.edit { preferences ->
            preferences[Keys.THEME] = value
        }
    }

    suspend fun setAutomaticPassAlertsEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.AUTO_PASS_ALERTS_ENABLED] = value
        }
    }

    suspend fun setAutomaticPassAlertMinVisibility(value: String) {
        dataStore.edit { preferences ->
            preferences[Keys.AUTO_PASS_ALERT_MIN_VISIBILITY] = value
        }
    }

    suspend fun setAutomaticPassAlertNotificationTimes(value: Set<String>) {
        dataStore.edit { preferences ->
            preferences[Keys.AUTO_PASS_ALERT_NOTIFICATION_TIMES] =
                value.ifEmpty { defaultAutomaticPassAlertNotificationTimes }
        }
    }

    suspend fun setAutomaticPassAlertLocation(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        locationName: String
    ) {
        dataStore.edit { preferences ->
            preferences[Keys.AUTO_PASS_ALERT_LATITUDE] = latitude
            preferences[Keys.AUTO_PASS_ALERT_LONGITUDE] = longitude
            preferences[Keys.AUTO_PASS_ALERT_ALTITUDE] = altitude
            preferences[Keys.AUTO_PASS_ALERT_LOCATION_NAME] = locationName
        }
    }

    suspend fun setAutomaticPassAlertScheduledIds(value: Set<String>) {
        dataStore.edit { preferences ->
            preferences[Keys.AUTO_PASS_ALERT_SCHEDULED_IDS] = value
        }
    }

    suspend fun clearAutomaticPassAlertScheduledIds() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.AUTO_PASS_ALERT_SCHEDULED_IDS)
        }
    }

    fun setAdFreeExpiry(timestamp: Long) {
        _adFreeExpiryFlow.value = timestamp
    }

    fun isAdFreeNow(): Boolean {
        return System.currentTimeMillis() < _adFreeExpiryFlow.value
    }
}
