package com.restart.spacestationtracker.shared.presentation

import com.restart.spacestationtracker.domain.iss_live.model.IssLocation
import com.restart.spacestationtracker.domain.iss_passes.model.IssPass
import com.restart.spacestationtracker.domain.people_in_space.model.Astronaut
import com.restart.spacestationtracker.domain.people_in_space.model.Expedition
import com.restart.spacestationtracker.domain.youtube.model.LiveStream
import com.restart.spacestationtracker.shared.network.KtorSpaceStationRepository
import com.restart.spacestationtracker.shared.settings.AppSettings
import com.restart.spacestationtracker.shared.settings.SharedSettingsRepository
import com.restart.spacestationtracker.shared.settings.defaultAppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SharedAppState(
    val currentIssLocation: IssLocation? = null,
    val futureIssLocations: List<IssLocation> = emptyList(),
    val liveStreams: List<LiveStream> = emptyList(),
    val expedition: Expedition? = null,
    val astronauts: List<Astronaut> = emptyList(),
    val passes: List<IssPass> = emptyList(),
    val passLocationName: String? = null,
    val settings: AppSettings = defaultAppSettings,
    val isMapLoading: Boolean = true,
    val isCrewLoading: Boolean = true,
    val isPassesLoading: Boolean = false,
    val mapError: String? = null,
    val crewError: String? = null,
    val passesError: String? = null
)

class SharedAppController(
    private val repository: KtorSpaceStationRepository = KtorSpaceStationRepository(),
    private val settingsRepository: SharedSettingsRepository? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(SharedAppState())
    val state: StateFlow<SharedAppState> = _state.asStateFlow()

    private var trackingJob: Job? = null
    private var hasStarted = false

    fun start() {
        if (hasStarted) return
        hasStarted = true
        settingsRepository?.let { repository ->
            scope.launch {
                repository.settings.collect { settings ->
                    _state.update { it.copy(settings = settings) }
                }
            }
        }
        startIssTracking()
        scope.launch { loadCrew() }
        scope.launch {
            _state.update { it.copy(liveStreams = repository.getNasaLiveStreams()) }
        }
    }

    fun setShowOrbit(showOrbit: Boolean) {
        settingsRepository?.setShowOrbit(showOrbit)
        _state.update {
            it.copy(settings = it.settings.copy(showOrbit = showOrbit))
        }
        if (showOrbit) startIssTracking()
    }

    fun setMapType(mapType: String) {
        settingsRepository?.setMapType(mapType)
        _state.update { it.copy(settings = it.settings.copy(mapType = mapType)) }
    }

    fun setUnits(units: String) {
        settingsRepository?.setUnits(units)
        _state.update { it.copy(settings = it.settings.copy(units = units)) }
    }

    fun setTheme(theme: String) {
        settingsRepository?.setTheme(theme)
        _state.update { it.copy(settings = it.settings.copy(theme = theme)) }
    }

    fun setAutomaticPassAlertsEnabled(enabled: Boolean) {
        settingsRepository?.setAutomaticPassAlertsEnabled(enabled)
        _state.update {
            it.copy(settings = it.settings.copy(automaticPassAlertsEnabled = enabled))
        }
    }

    fun setAutomaticPassAlertMinVisibility(value: String) {
        settingsRepository?.setAutomaticPassAlertMinVisibility(value)
        _state.update {
            it.copy(settings = it.settings.copy(automaticPassAlertMinVisibility = value))
        }
    }

    fun setAutomaticPassAlertNotificationTimes(value: Set<String>) {
        settingsRepository?.setAutomaticPassAlertNotificationTimes(value)
        _state.update {
            it.copy(settings = it.settings.copy(automaticPassAlertNotificationTimes = value))
        }
    }

    fun setAutomaticPassAlertLocation(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        locationName: String
    ) {
        settingsRepository?.setAutomaticPassAlertLocation(
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            locationName = locationName
        )
        _state.update {
            it.copy(
                settings = it.settings.copy(
                    automaticPassAlertLatitude = latitude,
                    automaticPassAlertLongitude = longitude,
                    automaticPassAlertAltitude = altitude,
                    automaticPassAlertLocationName = locationName
                )
            )
        }
    }

    fun retryCrew() {
        scope.launch { loadCrew() }
    }

    fun loadPasses(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        locationName: String
    ) {
        scope.launch {
            _state.update {
                it.copy(
                    isPassesLoading = true,
                    passesError = null,
                    passLocationName = locationName
                )
            }
            repository.getIssPasses(latitude, longitude, altitude)
                .onSuccess { passes ->
                    _state.update {
                        it.copy(
                            passes = passes,
                            isPassesLoading = false,
                            passesError = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isPassesLoading = false,
                            passesError = error.message ?: "Unable to load visible passes."
                        )
                    }
                }
        }
    }

    fun clearPassesError() {
        _state.update { it.copy(passesError = null) }
    }

    fun setPassesError(message: String) {
        _state.update {
            it.copy(
                isPassesLoading = false,
                passesError = message
            )
        }
    }

    fun setPassesLocationError(message: String) {
        _state.update {
            it.copy(
                passes = emptyList(),
                passLocationName = null,
                isPassesLoading = false,
                passesError = message
            )
        }
    }

    fun close() {
        trackingJob?.cancel()
        scope.cancel()
        repository.close()
    }

    private fun startIssTracking() {
        if (trackingJob?.isActive == true) return
        trackingJob = scope.launch {
            var futureMinuteOffset = 0
            var futureBatchCount = 0
            while (isActive) {
                val shouldLoadOrbit = futureBatchCount < 15 &&
                    (_state.value.settings.showOrbit || _state.value.futureIssLocations.isNotEmpty())
                val nowSeconds = kotlin.time.Clock.System.now().epochSeconds
                val futureTimestamps = if (shouldLoadOrbit) {
                    (0..9).map { minute -> nowSeconds + (futureMinuteOffset + minute) * 60L }
                } else {
                    emptyList()
                }

                repository.getIssFutureLocations(listOf(nowSeconds) + futureTimestamps)
                    .onSuccess { locations ->
                        val current = locations.firstOrNull()
                        val future = locations.drop(1)
                        _state.update { old ->
                            old.copy(
                                currentIssLocation = current ?: old.currentIssLocation,
                                futureIssLocations = old.futureIssLocations + future,
                                isMapLoading = false,
                                mapError = null
                            )
                        }
                        if (shouldLoadOrbit) {
                            futureMinuteOffset += 9
                            futureBatchCount += 1
                        }
                    }
                    .onFailure { error ->
                        _state.update {
                            it.copy(
                                isMapLoading = false,
                                mapError = error.message ?: "Unable to load the ISS position."
                            )
                        }
                    }
                delay(5_000)
            }
        }
    }

    private suspend fun loadCrew() {
        _state.update { it.copy(isCrewLoading = true, crewError = null) }
        repository.getPeopleInSpace()
            .onSuccess { (expedition, astronauts) ->
                val enrichedExpedition = scope.async {
                    val pageTitle = expedition.url.substringAfterLast("/")
                    expedition.copy(
                        bio = repository.getAstronautBio(pageTitle)
                            .getOrDefault("Biography not available.")
                    )
                }
                val enrichedAstronauts = astronauts.map { astronaut ->
                    scope.async {
                        val pageTitle = astronaut.bioUrl.substringAfterLast("/")
                        astronaut.copy(
                            bio = repository.getAstronautBio(pageTitle)
                                .getOrDefault("Biography not available.")
                        )
                    }
                }.awaitAll()
                _state.update {
                    it.copy(
                        expedition = enrichedExpedition.await(),
                        astronauts = enrichedAstronauts,
                        isCrewLoading = false,
                        crewError = null
                    )
                }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(
                        isCrewLoading = false,
                        crewError = error.message ?: "Unable to load the current crew."
                    )
                }
            }
    }
}
