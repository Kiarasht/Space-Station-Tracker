package com.restart.spacestationtracker.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.restart.spacestationtracker.domain.iss_live.model.IssLocation
import com.restart.spacestationtracker.shared.resources.Res
import com.restart.spacestationtracker.shared.resources.satellite_iss_short
import com.restart.spacestationtracker.shared.resources.satellite_hubble_short
import com.restart.spacestationtracker.shared.resources.satellite_tiangong_short
import com.restart.spacestationtracker.shared.satellites.SatelliteSnapshot
import com.restart.spacestationtracker.shared.satellites.TrackedSatellite
import com.restart.spacestationtracker.shared.ui.platform.SatelliteMapMarker
import org.jetbrains.compose.resources.stringResource

// Each path gets a color far from the others: the ISS's red line, Tiangong's teal, Hubble's violet.
private const val ISS_TRACK_ARGB = 0xFFFF3B30
private const val TIANGONG_ARGB = 0xFF14B8A6
private const val HUBBLE_ARGB = 0xFF8B5CF6

private val TrackedSatellite.accentArgb: Long
    get() = when (this) {
        TrackedSatellite.ISS -> 0xFFFFC83D
        TrackedSatellite.TIANGONG -> TIANGONG_ARGB
        TrackedSatellite.HUBBLE -> HUBBLE_ARGB
    }

/** Marker and card-border color for each satellite. */
internal val TrackedSatellite.accent: Color get() = Color(accentArgb)

internal fun TrackedSatellite.trackColorArgb(): Long = if (this == TrackedSatellite.ISS) ISS_TRACK_ARGB else accentArgb

/** Each marker matches its path's color, the ISS's red included. */
private val TrackedSatellite.markerArgb: Long get() = trackColorArgb()

/**
 * One marker per spacecraft, all drawn the same way: a badge in the color of its path with its name below.
 * The ISS sits at its live position; the others at their computed ones.
 */
@Composable
internal fun satelliteMarkers(
    snapshots: List<SatelliteSnapshot>,
    issLocation: IssLocation?,
    selected: TrackedSatellite?
): List<SatelliteMapMarker> {
    val iss = issLocation?.let {
        SatelliteMapMarker(
            id = TrackedSatellite.ISS.id,
            label = stringResource(Res.string.satellite_iss_short),
            latitude = it.latitude,
            longitude = it.longitude,
            colorArgb = TrackedSatellite.ISS.markerArgb,
            isStation = true,
            isSelected = selected == TrackedSatellite.ISS
        )
    }
    val tiangong = stringResource(Res.string.satellite_tiangong_short)
    val hubble = stringResource(Res.string.satellite_hubble_short)
    val others = snapshots.filter { it.satellite != TrackedSatellite.ISS }.map { snapshot ->
        SatelliteMapMarker(
            id = snapshot.satellite.id,
            label = if (snapshot.satellite == TrackedSatellite.TIANGONG) tiangong else hubble,
            latitude = snapshot.latitude,
            longitude = snapshot.longitude,
            colorArgb = snapshot.satellite.markerArgb,
            // Hubble is a telescope; the others are stations.
            isStation = snapshot.satellite != TrackedSatellite.HUBBLE,
            isSelected = snapshot.satellite == selected
        )
    }
    return listOfNotNull(iss) + others
}
