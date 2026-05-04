package com.restart.spacestationtracker.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restart.spacestationtracker.data.settings.AppSettings
import com.restart.spacestationtracker.data.settings.SettingsRepository
import com.restart.spacestationtracker.data.settings.defaultAppSettings
import com.restart.spacestationtracker.util.AutomaticPassAlertWorkScheduler
import com.restart.spacestationtracker.util.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val workScheduler: AutomaticPassAlertWorkScheduler,
    private val application: Application
) : ViewModel() {

    val settingsState: StateFlow<AppSettings> = settingsRepository.appSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = defaultAppSettings
        )

    fun onMapTypeChanged(mapType: String) {
        viewModelScope.launch {
            settingsRepository.setMapType(mapType)
        }
    }

    fun onUnitsChanged(units: String) {
        viewModelScope.launch {
            settingsRepository.setUnits(units)
        }
    }

    fun onThemeChanged(theme: String) {
        viewModelScope.launch {
            settingsRepository.setTheme(theme)
        }
    }

    fun enableAutomaticPassAlerts(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        locationName: String
    ) {
        viewModelScope.launch {
            settingsRepository.setAutomaticPassAlertLocation(
                latitude = latitude,
                longitude = longitude,
                altitude = altitude,
                locationName = locationName
            )
            settingsRepository.setAutomaticPassAlertsEnabled(true)
            workScheduler.scheduleDailySync()
            workScheduler.runImmediateSync()
        }
    }

    fun disableAutomaticPassAlerts() {
        viewModelScope.launch {
            cancelAutomaticAlertNotifications()
            settingsRepository.setAutomaticPassAlertsEnabled(false)
            settingsRepository.clearAutomaticPassAlertScheduledIds()
            workScheduler.cancelDailySync()
        }
    }

    fun updateAutomaticPassAlertLocation(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        locationName: String
    ) {
        viewModelScope.launch {
            cancelAutomaticAlertNotifications()
            settingsRepository.setAutomaticPassAlertLocation(
                latitude = latitude,
                longitude = longitude,
                altitude = altitude,
                locationName = locationName
            )
            settingsRepository.clearAutomaticPassAlertScheduledIds()
            if (settingsState.value.automaticPassAlertsEnabled) {
                workScheduler.scheduleDailySync()
                workScheduler.runImmediateSync()
            }
        }
    }

    fun onAutomaticPassAlertMinVisibilityChanged(value: String) {
        viewModelScope.launch {
            cancelAutomaticAlertNotifications()
            settingsRepository.setAutomaticPassAlertMinVisibility(value)
            settingsRepository.clearAutomaticPassAlertScheduledIds()
            if (settingsState.value.automaticPassAlertsEnabled) {
                workScheduler.runImmediateSync()
            }
        }
    }

    fun onAutomaticPassAlertNotificationTimesChanged(value: Set<String>) {
        viewModelScope.launch {
            cancelAutomaticAlertNotifications()
            settingsRepository.setAutomaticPassAlertNotificationTimes(value)
            settingsRepository.clearAutomaticPassAlertScheduledIds()
            if (settingsState.value.automaticPassAlertsEnabled) {
                workScheduler.runImmediateSync()
            }
        }
    }

    private fun cancelAutomaticAlertNotifications() {
        NotificationScheduler(application)
            .cancelAutomaticNotifications(settingsState.value.automaticPassAlertScheduledIds)
    }
}
