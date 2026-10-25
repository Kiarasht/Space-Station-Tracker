package com.restart.spacestationtracker.shared.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

/** The switcher's short name for each spacecraft, the same as its map label. */
@Composable
private fun TrackedSatellite.shortName(): String = stringResource(
    when (this) {
        TrackedSatellite.ISS -> Res.string.satellite_iss_short
        TrackedSatellite.TIANGONG -> Res.string.satellite_tiangong_short
        TrackedSatellite.HUBBLE -> Res.string.satellite_hubble_short
    }
)

/**
 * A stack of pills for jumping between the spacecraft on the map, each with a dot in its path's color. The
 * selected one is filled with that color, matching its marker, card and line.
 */
@Composable
internal fun SatelliteSwitcher(
    available: List<TrackedSatellite>,
    selected: TrackedSatellite,
    onSelect: (TrackedSatellite) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.spaceColors
    Column(
        modifier = modifier.width(IntrinsicSize.Max),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        available.forEach { satellite ->
            val isSelected = satellite == selected
            val accent = Color(satellite.markerArgb)
            Surface(
                onClick = { onSelect(satellite) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { this.selected = isSelected },
                shape = RoundedCornerShape(percent = 50),
                color = if (isSelected) accent else colors.navBarContainer,
                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                border = if (isSelected) null else BorderStroke(1.dp, colors.navBarBorder),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else accent)
                    )
                    Text(
                        satellite.shortName(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
