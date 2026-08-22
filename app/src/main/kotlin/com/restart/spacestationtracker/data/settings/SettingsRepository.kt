package com.restart.spacestationtracker.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.restart.spacestationtracker.shared.settings.AppSettingsKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext context: Context) {

    private val dataStore = context.dataStore
    private val _initialSettingsLoaded = MutableStateFlow(false)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var hasLifetimeAdRemovalCache = false

    private object Keys {
        val MIN_ALTITUDE = intPreferencesKey(AppSettingsKeys.MIN_ALTITUDE)
        val MIN_MAGNITUDE = intPreferencesKey(AppSettingsKeys.MIN_MAGNITUDE)
        val SHOW_EVENTS = booleanPreferencesKey(AppSettingsKeys.SHOW_EVENTS)
        val SHOW_ORBIT = booleanPreferencesKey(AppSettingsKeys.SHOW_ORBIT)
        val MAP_TYPE = stringPreferencesKey(AppSettingsKeys.MAP_TYPE)
        val UNITS = stringPreferencesKey(AppSettingsKeys.UNITS)
        val THEME = stringPreferencesKey(AppSettingsKeys.THEME)
        val LEGACY_AD_FREE_EXPIRY = longPreferencesKey("ad_free_expiry")
        val LIFETIME_AD_REMOVAL = booleanPreferencesKey(AppSettingsKeys.LIFETIME_AD_REMOVAL)
        val AUTO_PASS_ALERTS_ENABLED =
            booleanPreferencesKey(AppSettingsKeys.AUTO_PASS_ALERTS_ENABLED)
        val AUTO_PASS_ALERT_MIN_VISIBILITY =
            stringPreferencesKey(AppSettingsKeys.AUTO_PASS_ALERT_MIN_VISIBILITY)
        val AUTO_PASS_ALERT_NOTIFICATION_TIMES =
            stringSetPreferencesKey(AppSettingsKeys.AUTO_PASS_ALERT_NOTIFICATION_TIMES)
        val AUTO_PASS_ALERT_LATITUDE =
            doublePreferencesKey(AppSettingsKeys.AUTO_PASS_ALERT_LATITUDE)
        val AUTO_PASS_ALERT_LONGITUDE =
            doublePreferencesKey(AppSettingsKeys.AUTO_PASS_ALERT_LONGITUDE)
        val AUTO_PASS_ALERT_ALTITUDE =
            doublePreferencesKey(AppSettingsKeys.AUTO_PASS_ALERT_ALTITUDE)
        val AUTO_PASS_ALERT_LOCATION_NAME =
            stringPreferencesKey(AppSettingsKeys.AUTO_PASS_ALERT_LOCATION_NAME)
        val AUTO_PASS_ALERT_SCHEDULED_IDS =
            stringSetPreferencesKey(AppSettingsKeys.AUTO_PASS_ALERT_SCHEDULED_IDS)
        val AUTO_PASS_ALERT_LAST_SYNC_TIME =
            longPreferencesKey(AppSettingsKeys.AUTO_PASS_ALERT_LAST_SYNC_TIME)
        val AUTO_PASS_ALERT_LAST_SYNC_RESULT =
            stringPreferencesKey(AppSettingsKeys.AUTO_PASS_ALERT_LAST_SYNC_RESULT)
        val AUTO_PASS_ALERT_LAST_SYNC_MESSAGE =
            stringPreferencesKey(AppSettingsKeys.AUTO_PASS_ALERT_LAST_SYNC_MESSAGE)
    }

    init {
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences.remove(Keys.LEGACY_AD_FREE_EXPIRY)
            }
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) emit(emptyPreferences()) else throw exception
                }
                .map { preferences -> preferences[Keys.LIFETIME_AD_REMOVAL] ?: false }
                .distinctUntilChanged()
                .collect { enabled ->
                    hasLifetimeAdRemovalCache = enabled
                    _initialSettingsLoaded.value = true
                }
        }
    }

    val appSettingsFlow: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
        val minAltitude = preferences[Keys.MIN_ALTITUDE] ?: defaultAppSettings.minAltitude
        val minMagnitude = preferences[Keys.MIN_MAGNITUDE] ?: defaultAppSettings.minMagnitude
        val showEvents = preferences[Keys.SHOW_EVENTS] ?: defaultAppSettings.showEvents
        val showOrbit = preferences[Keys.SHOW_ORBIT] ?: defaultAppSettings.showOrbit
        val mapType = preferences[Keys.MAP_TYPE] ?: defaultAppSettings.mapType
        val units = preferences[Keys.UNITS] ?: defaultAppSettings.units
        val theme = preferences[Keys.THEME] ?: defaultAppSettings.theme
        val hasLifetimeAdRemoval =
            (preferences[Keys.LIFETIME_AD_REMOVAL] ?: false)
                .also { hasLifetimeAdRemovalCache = it }
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
        val automaticPassAlertLastSyncTimeMillis = preferences[Keys.AUTO_PASS_ALERT_LAST_SYNC_TIME]
        val automaticPassAlertLastSyncResult = preferences[Keys.AUTO_PASS_ALERT_LAST_SYNC_RESULT]
        val automaticPassAlertLastSyncMessage = preferences[Keys.AUTO_PASS_ALERT_LAST_SYNC_MESSAGE]
        
        AppSettings(
            minAltitude = minAltitude,
            minMagnitude = minMagnitude,
            showEvents = showEvents,
            showOrbit = showOrbit,
            mapType = mapType,
            units = units,
            theme = theme,
            hasLifetimeAdRemoval = hasLifetimeAdRemoval,
            automaticPassAlertsEnabled = automaticPassAlertsEnabled,
            automaticPassAlertMinVisibility = automaticPassAlertMinVisibility,
            automaticPassAlertNotificationTimes = automaticPassAlertNotificationTimes,
            automaticPassAlertLatitude = automaticPassAlertLatitude,
            automaticPassAlertLongitude = automaticPassAlertLongitude,
            automaticPassAlertAltitude = automaticPassAlertAltitude,
            automaticPassAlertLocationName = automaticPassAlertLocationName,
            automaticPassAlertScheduledIds = automaticPassAlertScheduledIds,
            automaticPassAlertLastSyncTimeMillis = automaticPassAlertLastSyncTimeMillis,
            automaticPassAlertLastSyncResult = automaticPassAlertLastSyncResult,
            automaticPassAlertLastSyncMessage = automaticPassAlertLastSyncMessage
        )
    }

    suspend fun setMapType(value: String) {
        dataStore.edit { preferences ->
            preferences[Keys.MAP_TYPE] = value
        }
    }

    suspend fun setShowOrbit(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.SHOW_ORBIT] = value
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

    suspend fun setAutomaticPassAlertSyncStatus(
        timestampMillis: Long,
        result: String,
        message: String
    ) {
        dataStore.edit { preferences ->
            preferences[Keys.AUTO_PASS_ALERT_LAST_SYNC_TIME] = timestampMillis
            preferences[Keys.AUTO_PASS_ALERT_LAST_SYNC_RESULT] = result
            preferences[Keys.AUTO_PASS_ALERT_LAST_SYNC_MESSAGE] = message
        }
    }

    suspend fun awaitInitialLoad() {
        _initialSettingsLoaded.first { it }
    }

    fun setLifetimeAdRemoval(enabled: Boolean) {
        hasLifetimeAdRemovalCache = enabled
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[Keys.LIFETIME_AD_REMOVAL] = enabled
            }
        }
    }

    fun hasLifetimeAdRemoval(): Boolean {
        return hasLifetimeAdRemovalCache
    }

    fun isAdFreeNow(): Boolean {
        return hasLifetimeAdRemovalCache
    }
}
