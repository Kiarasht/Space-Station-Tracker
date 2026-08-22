package com.restart.spacestationtracker.ui.iss_live

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.data.settings.SettingsRepository
import com.restart.spacestationtracker.domain.iss_live.use_case.GetFutureIssLocationsUseCase
import com.restart.spacestationtracker.domain.youtube.use_case.GetNasaLiveStreamStatusUseCase
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
    private var futureTimeOffset = 0
    private var lineCount = 0

    init {
        startIssTracking()
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
                    showOrbit = settings.showOrbit
                )
            }.launchIn(viewModelScope)
    }

    private fun startIssTracking() {
        issTrackingJob?.cancel()
        issTrackingJob = viewModelScope.launch {
            if (_uiState.value.issLocation == null) {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
            while (isActive) {
                // Fetch current location always.
                // Fetch future locations only for the first ~2 hours (15 batches * 9 mins = 135 mins)
                // to prevent infinite line growth.
                val shouldFetchFuture = lineCount < 15 &&
                    (_uiState.value.showOrbit || _uiState.value.futureIssLocations.isNotEmpty())
                val success = fetchIssLocations(fetchFuture = shouldFetchFuture)
                if (success && shouldFetchFuture) {
                    ++lineCount
                }
                delay(5000)
            }
        }
    }

    private suspend fun fetchIssLocations(fetchFuture: Boolean): Boolean {
        val currentTime = System.currentTimeMillis() / 1000
        val timestamps = if (fetchFuture) {
            (0..9).map { minute ->
                currentTime + (futureTimeOffset + minute) * 60
            }
        } else {
            emptyList()
        }
        
        var isSuccess = false
        getFutureIssLocationsUseCase(listOf(currentTime) + timestamps)
            .onSuccess { newLocations ->
                val currentLocation = newLocations.first()
                val futureLocations = newLocations.drop(1)

                _uiState.value = _uiState.value.copy(
                    issLocation = currentLocation,
                    futureIssLocations = _uiState.value.futureIssLocations + futureLocations,
                    isLoading = false,
                    error = null
                )
                if (fetchFuture) {
                    futureTimeOffset += 9
                }
                isSuccess = true
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    error = throwable.localizedMessage ?: application.getString(R.string.unknown_error)
                )
                isSuccess = false
            }
        return isSuccess
    }

    private fun checkNasaLiveStatus() {
        viewModelScope.launch {
            val streams = getNasaLiveStreamStatusUseCase()
            _uiState.value = _uiState.value.copy(liveStreams = streams)
        }
    }

    override fun onCleared() {
        super.onCleared()
        issTrackingJob?.cancel()
    }
}
