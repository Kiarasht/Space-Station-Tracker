package com.restart.spacestationtracker.ui.iss_live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.maps.android.compose.MapType
import com.restart.spacestationtracker.data.settings.SettingsRepository
import com.restart.spacestationtracker.domain.iss_live.use_case.GetFutureIssLocationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val getFutureIssLocationsUseCase: GetFutureIssLocationsUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var issTrackingJob: Job? = null
    private var futureTimeOffset = 0
    private var lineCount = 0

    init {
        startIssTracking()
        observeSettings()
    }

    private fun observeSettings() {
        settingsRepository.appSettingsFlow
            .onEach { settings ->
                _uiState.value = _uiState.value.copy(
                    mapType = when (settings.mapType) {
                        "Satellite" -> MapType.SATELLITE
                        "Hybrid" -> MapType.HYBRID
                        "Terrain" -> MapType.TERRAIN
                        else -> MapType.NORMAL
                    }
                )
            }.launchIn(viewModelScope)
    }

    private fun startIssTracking() {
        issTrackingJob?.cancel()
        issTrackingJob = viewModelScope.launch {
            while (lineCount < 19) {
                ++lineCount
                getFutureIssLocations()
                delay(5000)
            }
        }
    }

    private fun getFutureIssLocations() {
        viewModelScope.launch {
            val timestamps = (0..9).map { minute ->
                System.currentTimeMillis() / 1000 + (futureTimeOffset + minute) * 60
            }
            getFutureIssLocationsUseCase(listOf(System.currentTimeMillis() / 1000) + timestamps).onSuccess { newLocations ->
                val currentLocation = newLocations.first()
                val futureLocations = newLocations.drop(1)

                _uiState.value = _uiState.value.copy(
                    issLocation = currentLocation,
                    futureIssLocations = _uiState.value.futureIssLocations + futureLocations,
                    isLoading = false,
                    error = null
                )
                futureTimeOffset += 9
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    error = throwable.localizedMessage ?: "An unknown error occurred"
                )
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        issTrackingJob?.cancel()
    }
}
