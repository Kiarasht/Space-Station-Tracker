package com.restart.spacestationtracker.shared.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.restart.spacestationtracker.shared.network.appLanguageCode
import com.restart.spacestationtracker.shared.network.wikipediaLanguage
import com.restart.spacestationtracker.shared.resources.Res
import com.restart.spacestationtracker.shared.resources.close
import com.restart.spacestationtracker.shared.resources.iss_name
import com.restart.spacestationtracker.shared.resources.launched_format
import com.restart.spacestationtracker.shared.resources.learn_more
import com.restart.spacestationtracker.shared.resources.satellite_hubble_description
import com.restart.spacestationtracker.shared.resources.satellite_hubble_name
import com.restart.spacestationtracker.shared.resources.satellite_hubble_short
import com.restart.spacestationtracker.shared.resources.satellite_iss_description
import com.restart.spacestationtracker.shared.resources.satellite_kind_station
import com.restart.spacestationtracker.shared.resources.satellite_kind_telescope
import com.restart.spacestationtracker.shared.resources.satellite_orbit_format
import com.restart.spacestationtracker.shared.resources.satellite_tiangong_description
import com.restart.spacestationtracker.shared.resources.satellite_tiangong_name
import com.restart.spacestationtracker.shared.resources.satellite_tiangong_short
import com.restart.spacestationtracker.shared.satellites.SatelliteSnapshot
import com.restart.spacestationtracker.shared.satellites.TrackedSatellite
import com.restart.spacestationtracker.shared.ui.platform.SatelliteMapMarker
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** How each satellite looks and reads in the app. */
internal val TrackedSatellite.accent: Color
    get() = when (this) {
        TrackedSatellite.ISS -> Color(0xFFFFC83D)
        TrackedSatellite.TIANGONG -> Color(TIANGONG_ARGB)
        TrackedSatellite.HUBBLE -> Color(HUBBLE_ARGB)
    }

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

private val TrackedSatellite.isStation: Boolean get() = this != TrackedSatellite.HUBBLE

private val TrackedSatellite.nameRes: StringResource
    get() = when (this) {
        TrackedSatellite.ISS -> Res.string.iss_name
        TrackedSatellite.TIANGONG -> Res.string.satellite_tiangong_name
        TrackedSatellite.HUBBLE -> Res.string.satellite_hubble_name
    }

private val TrackedSatellite.descriptionRes: StringResource
    get() = when (this) {
        TrackedSatellite.ISS -> Res.string.satellite_iss_description
        TrackedSatellite.TIANGONG -> Res.string.satellite_tiangong_description
        TrackedSatellite.HUBBLE -> Res.string.satellite_hubble_description
    }

/** Agencies are proper names, so they read the same in every language. */
private val TrackedSatellite.operators: String
    get() = when (this) {
        TrackedSatellite.ISS -> "NASA · Roscosmos · ESA · JAXA · CSA"
        TrackedSatellite.TIANGONG -> "CMSA"
        TrackedSatellite.HUBBLE -> "NASA · ESA"
    }

/** Map markers for everything except the ISS, which keeps its own marker. */
@Composable
internal fun satelliteMarkers(snapshots: List<SatelliteSnapshot>, selected: TrackedSatellite?): List<SatelliteMapMarker> {
    val tiangong = stringResource(Res.string.satellite_tiangong_short)
    val hubble = stringResource(Res.string.satellite_hubble_short)
    return snapshots.filter { it.satellite != TrackedSatellite.ISS }.map { snapshot ->
        SatelliteMapMarker(
            id = snapshot.satellite.id,
            label = if (snapshot.satellite == TrackedSatellite.TIANGONG) tiangong else hubble,
            latitude = snapshot.latitude,
            longitude = snapshot.longitude,
            colorArgb = snapshot.satellite.accentArgb,
            isStation = snapshot.satellite.isStation,
            isSelected = snapshot.satellite == selected
        )
    }
}

internal fun TrackedSatellite.trackColorArgb(): Long = if (this == TrackedSatellite.ISS) ISS_TRACK_ARGB else accentArgb

/**
 * About the selected satellite: kind, orbit time, a short description, and a link to Wikipedia in the
 * reader's language. Its live position, altitude and speed are in the map's top-left card.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SatelliteDetailCard(
    satellite: TrackedSatellite,
    snapshot: SatelliteSnapshot?,
    onClose: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = satellite.accent
    val pillColor = satellite.pillColor()
    Surface(
        modifier = modifier.widthIn(max = 560.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SpaceIconTile(
                    icon = if (satellite.isStation) Icons.Default.SatelliteAlt else Icons.Default.PhotoCamera,
                    tint = accent,
                    size = 40.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(satellite.nameRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stringResource(if (satellite.isStation) Res.string.satellite_kind_station else Res.string.satellite_kind_telescope),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.close))
                }
            }
            FlowRow(
                modifier = Modifier.padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                snapshot?.let {
                    SpacePill(
                        stringResource(Res.string.satellite_orbit_format, it.periodMinutes),
                        color = pillColor,
                        icon = Icons.Default.Timer
                    )
                }
            }
            Text(
                stringResource(satellite.descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${stringResource(Res.string.launched_format, satellite.launchYear.toString())} · ${satellite.operators}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { onOpenUrl(satellite.learnMoreUrl(wikipediaLanguage(appLanguageCode()))) }) {
                    Text(stringResource(Res.string.learn_more))
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** Each satellite's accent, deepened on light cards so pill text stays readable. */
@Composable
private fun TrackedSatellite.pillColor(): Color {
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    return when (this) {
        TrackedSatellite.ISS -> MaterialTheme.spaceColors.visibilityVeryBright
        TrackedSatellite.TIANGONG -> if (isLight) Color(0xFF0F766E) else Color(0xFF2DD4BF)
        TrackedSatellite.HUBBLE -> if (isLight) Color(0xFF6D28D9) else Color(0xFFA78BFA)
    }
}
