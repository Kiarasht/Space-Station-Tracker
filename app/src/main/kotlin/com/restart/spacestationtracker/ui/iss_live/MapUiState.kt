package com.restart.spacestationtracker.ui.iss_live

import com.google.maps.android.compose.MapType
import com.restart.spacestationtracker.domain.iss_live.model.IssLocation

data class MapUiState(
    val issLocation: IssLocation? = null,
    val futureIssLocations: List<IssLocation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val mapType: MapType = MapType.NORMAL
)
