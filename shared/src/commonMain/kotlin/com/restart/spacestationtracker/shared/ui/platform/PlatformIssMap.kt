package com.restart.spacestationtracker.shared.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.restart.spacestationtracker.domain.iss_live.model.IssLocation

data class IssMapPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = 0L,
    val footprint: Double = 0.0,
    val solarLatitude: Double = 0.0,
    val solarLongitude: Double = 0.0
)

/** Another spacecraft on the map, such as Tiangong or Hubble; tapping it reports [id]. */
data class SatelliteMapMarker(
    val id: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    /** Marker color as 0xAARRGGBB. */
    val colorArgb: Long,
    /** Draws a station (solar panels) rather than a telescope. */
    val isStation: Boolean,
    val isSelected: Boolean
)

/**
 * Asks the map to glide to a spot at its current zoom. A new [serial] means a new request, so choosing the
 * same spacecraft twice still recenters on it.
 */
data class MapFocusRequest(
    val latitude: Double,
    val longitude: Double,
    val serial: Int
)

/**
 * A satellite's predicted path, with direction arrows and +30m to +120m labels along it. Hidden paths are
 * still passed so the map can fade them out (and back in) when [visible] changes.
 */
data class SatelliteMapTrack(
    val id: String,
    val points: List<IssMapPoint>,
    /** Line color as 0xAARRGGBB. */
    val colorArgb: Long,
    val visible: Boolean
)

/**
 * The live map. [bottomInset] keeps the map's own logo, legal link and controls clear of anything drawn
 * over its bottom edge, such as the floating navigation bar; the map itself still fills [modifier].
 * Every spacecraft, the ISS included, is a [satellites] marker. Tapping one calls [onSatelliteClick] with its id; tapping empty map
 * calls it with null. [tracks] are the other satellites' paths, each in its own color.
 */
@Composable
expect fun PlatformIssMap(
    currentLocation: IssLocation?,
    orbit: List<IssMapPoint>,
    mapType: String,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
    satellites: List<SatelliteMapMarker> = emptyList(),
    tracks: List<SatelliteMapTrack> = emptyList(),
    /** Whether the ISS's [orbit] line shows; it fades out while another spacecraft is selected. */
    orbitVisible: Boolean = true,
    /** Moves the map to a spacecraft chosen outside the map, such as from the switcher. */
    focus: MapFocusRequest? = null,
    onSatelliteClick: (String?) -> Unit = {}
)

@Composable
expect fun PlatformNativeAd(
    slotId: String,
    modifier: Modifier = Modifier
)

@Composable
expect fun PlatformBannerAd(
    modifier: Modifier = Modifier
)
