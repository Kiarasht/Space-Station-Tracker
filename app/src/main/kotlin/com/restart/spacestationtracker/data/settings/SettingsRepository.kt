package com.restart.spacestationtracker.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
)

val defaultAppSettings = AppSettings(
    minAltitude = 10,
    minMagnitude = 4,
    showEvents = true,
    showOrbit = true,
    mapType = "Normal",
    units = "Metric",
    theme = "Follow System"
)


@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext context: Context) {

    private val dataStore = context.dataStore

    private object Keys {
        val MIN_ALTITUDE = intPreferencesKey("min_altitude")
        val MIN_MAGNITUDE = intPreferencesKey("min_magnitude")
        val SHOW_EVENTS = booleanPreferencesKey("show_events")
        val SHOW_ORBIT = booleanPreferencesKey("show_orbit")
        val MAP_TYPE = stringPreferencesKey("map_type")
        val UNITS = stringPreferencesKey("units")
        val THEME = stringPreferencesKey("theme")
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
            AppSettings(minAltitude, minMagnitude, showEvents, showOrbit, mapType, units, theme)
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
}
