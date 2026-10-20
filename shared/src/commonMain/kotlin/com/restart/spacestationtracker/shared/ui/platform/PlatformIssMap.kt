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

/** Map id reported when the ISS marker itself is tapped. */
const val ISS_MARKER_ID = "iss"

/**
 * The live map. [bottomInset] keeps the map's own logo, legal link and controls clear of anything drawn
 * over its bottom edge, such as the floating navigation bar; the map itself still fills [modifier].
 * Tapping the ISS or a [satellites] marker calls [onSatelliteClick] with its id, and tapping empty map
 * calls it with null. [selectedTrack] is the selected satellite's next orbit, drawn in [selectedTrackColorArgb].
 */
@Composable
expect fun PlatformIssMap(
    currentLocation: IssLocation?,
    orbit: List<IssMapPoint>,
    mapType: String,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
    satellites: List<SatelliteMapMarker> = emptyList(),
    selectedTrack: List<IssMapPoint> = emptyList(),
    selectedTrackColorArgb: Long = 0xFFFFFFFF,
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
