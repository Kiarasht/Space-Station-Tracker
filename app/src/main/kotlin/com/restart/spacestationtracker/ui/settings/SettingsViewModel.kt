package com.restart.spacestationtracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restart.spacestationtracker.data.settings.AppSettings
import com.restart.spacestationtracker.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settingsState: StateFlow<AppSettings> = settingsRepository.appSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings(9, 5, false, true, "Normal", "Metric", "Follow System")
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
}
