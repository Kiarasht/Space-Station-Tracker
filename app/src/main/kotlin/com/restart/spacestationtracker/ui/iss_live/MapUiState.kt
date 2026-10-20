package com.restart.spacestationtracker.ui.iss_live

import com.restart.spacestationtracker.domain.iss_live.model.IssLocation
import com.restart.spacestationtracker.domain.youtube.model.LiveStream
import com.restart.spacestationtracker.shared.satellites.SatelliteSnapshot

data class MapUiState(
    val issLocation: IssLocation? = null,
    val futureIssLocations: List<IssLocation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val mapType: String = "Normal",
    val units: String = "Metric",
    val liveStreams: List<LiveStream> = emptyList(),
    val isAdFree: Boolean = false,
    val showOrbit: Boolean = true,
    val satellites: List<SatelliteSnapshot> = emptyList()
)
