package com.restart.spacestationtracker.ui.iss_live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.maps.android.compose.MapType
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
                val isAdFree = System.currentTimeMillis() < settings.adFreeExpiry
                _uiState.value = _uiState.value.copy(
                    mapType = when (settings.mapType) {
                        "Satellite" -> MapType.SATELLITE
                        "Hybrid" -> MapType.HYBRID
                        "Terrain" -> MapType.TERRAIN
                        else -> MapType.NORMAL
                    },
                    isAdFree = isAdFree
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
                val shouldFetchFuture = lineCount < 15
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
                    error = throwable.localizedMessage ?: "An unknown error occurred"
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

    fun grantAdFreeAccess() {
        // 6 hours = 6 * 60 * 60 * 1000 milliseconds
        val durationInMillis = 6L * 60 * 60 * 1000
        val expiryTime = System.currentTimeMillis() + durationInMillis
        settingsRepository.setAdFreeExpiry(expiryTime)
    }


    override fun onCleared() {
        super.onCleared()
        issTrackingJob?.cancel()
    }
}
