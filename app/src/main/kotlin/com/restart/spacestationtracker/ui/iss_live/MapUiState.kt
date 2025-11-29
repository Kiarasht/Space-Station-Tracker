package com.restart.spacestationtracker.ui.iss_live

import com.google.maps.android.compose.MapType
import com.restart.spacestationtracker.domain.iss_live.model.IssLocation
import com.restart.spacestationtracker.domain.youtube.model.LiveStream

data class MapUiState(
    val issLocation: IssLocation? = null,
    val futureIssLocations: List<IssLocation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val mapType: MapType = MapType.NORMAL,
    val liveStreams: List<LiveStream>? = null,
    val isAdFree: Boolean = false
)
