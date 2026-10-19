package com.restart.spacestationtracker.ui.iss_live

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.data.settings.SettingsRepository
import com.restart.spacestationtracker.domain.iss_live.use_case.GetFutureIssLocationsUseCase
import com.restart.spacestationtracker.domain.youtube.use_case.GetNasaLiveStreamStatusUseCase
import com.restart.spacestationtracker.shared.presentation.OrbitTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val getFutureIssLocationsUseCase: GetFutureIssLocationsUseCase,
    private val settingsRepository: SettingsRepository,
    private val getNasaLiveStreamStatusUseCase: GetNasaLiveStreamStatusUseCase,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var issTrackingJob: Job? = null

    init {
        observeSettings()
        checkNasaLiveStatus()
    }

    private fun observeSettings() {
        settingsRepository.appSettingsFlow
            .onEach { settings ->
                val isAdFree = settings.hasLifetimeAdRemoval
                _uiState.value = _uiState.value.copy(
                    mapType = settings.mapType,
                    units = settings.units,
                    isAdFree = isAdFree,
                    showOrbit = settings.showOrbit,
                    futureIssLocations = if (settings.showOrbit) {
                        _uiState.value.futureIssLocations
                    } else {
                        emptyList()
                    }
                )
            }.launchIn(viewModelScope)
    }

    /** Called when the map becomes visible; polling stops again in [stopIssTracking]. */
    fun startIssTracking() {
        if (issTrackingJob?.isActive == true) return
        issTrackingJob = viewModelScope.launch {
            if (_uiState.value.issLocation == null) {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
            while (isActive) {
                fetchIssLocations()
                delay(5000)
            }
        }
    }

    fun stopIssTracking() {
        issTrackingJob?.cancel()
        issTrackingJob = null
    }

    private suspend fun fetchIssLocations() {
        val currentTime = System.currentTimeMillis() / 1000
        val timestamps = OrbitTrack.nextTimestamps(
            future = _uiState.value.futureIssLocations,
            nowSeconds = currentTime,
            showOrbit = _uiState.value.showOrbit
        )

        getFutureIssLocationsUseCase(listOf(currentTime) + timestamps)
            .onSuccess { newLocations ->
                val currentLocation = newLocations.firstOrNull()
                if (currentLocation == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = application.getString(R.string.unknown_error)
                    )
                    return@onSuccess
                }

                _uiState.value = _uiState.value.copy(
                    issLocation = currentLocation,
                    futureIssLocations = if (_uiState.value.showOrbit) {
                        OrbitTrack.merge(
                            existing = _uiState.value.futureIssLocations,
                            incoming = newLocations.drop(1),
                            nowSeconds = currentTime
                        )
                    } else {
                        emptyList()
                    },
                    isLoading = false,
                    error = null
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = application.getString(R.string.unknown_error)
                )
            }
    }

    private fun checkNasaLiveStatus() {
        viewModelScope.launch {
            val streams = getNasaLiveStreamStatusUseCase()
            _uiState.value = _uiState.value.copy(liveStreams = streams)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopIssTracking()
    }
}
