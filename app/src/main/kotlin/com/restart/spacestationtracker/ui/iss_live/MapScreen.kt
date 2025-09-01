package com.restart.spacestationtracker.ui.iss_live

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.restart.spacestationtracker.R

@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    contentPadding: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    val screenPadding = PaddingValues(
        start = contentPadding.calculateStartPadding(LayoutDirection.Ltr) + 16.dp,
        end = contentPadding.calculateEndPadding(LayoutDirection.Ltr) + 16.dp,
        top = contentPadding.calculateTopPadding() + 16.dp,
        bottom = contentPadding.calculateBottomPadding()
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            if (uiState.isLoading && uiState.issLocation == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Text(
                    text = "Error: ${uiState.error}",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }

            uiState.issLocation?.let { location ->
                val issLatLng = LatLng(location.latitude, location.longitude)
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(issLatLng, 2f)
                }
                val markerState = rememberUpdatedMarkerState(position = issLatLng)

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(mapType = uiState.mapType),
                    contentPadding = PaddingValues(bottom = 50.dp),
                    uiSettings = MapUiSettings(zoomControlsEnabled = false)
                ) {
                    Marker(
                        state = markerState,
                        title = "ISS",
                        snippet = "International Space Station",
                        icon = BitmapDescriptorFactory.fromResource(R.drawable.iss_2011),
                        anchor = Offset(0.5f, 0.5f)
                    )

                    if (uiState.futureIssLocations.isNotEmpty()) {
                        val points = mutableListOf(issLatLng)
                        points.addAll(uiState.futureIssLocations.map {
                            LatLng(
                                it.latitude,
                                it.longitude
                            )
                        })
                        Polyline(
                            points = points,
                            color = Color.Red,
                            width = 5f
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(screenPadding)
                ) {
                    IssDataCard(location = location)
                }
            }
        }
    }
}
