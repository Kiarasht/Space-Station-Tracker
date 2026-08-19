package com.restart.spacestationtracker.shared.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.restart.spacestationtracker.domain.iss_live.model.IssLocation

data class IssMapPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = 0L,
    val footprint: Double = 0.0,
    val solarLatitude: Double = 0.0,
    val solarLongitude: Double = 0.0
)

@Composable
expect fun PlatformIssMap(
    currentLocation: IssLocation?,
    orbit: List<IssMapPoint>,
    mapType: String,
    modifier: Modifier = Modifier
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
