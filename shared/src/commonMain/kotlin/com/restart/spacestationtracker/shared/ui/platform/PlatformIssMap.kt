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

/**
 * The live map. [bottomInset] keeps the map's own logo, legal link and controls clear of anything drawn
 * over its bottom edge, such as the floating navigation bar; the map itself still fills [modifier].
 */
@Composable
expect fun PlatformIssMap(
    currentLocation: IssLocation?,
    orbit: List<IssMapPoint>,
    mapType: String,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp
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
