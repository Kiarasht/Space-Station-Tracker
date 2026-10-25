package com.restart.spacestationtracker.shared.ui.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import com.restart.spacestationtracker.domain.iss_live.model.IssLocation
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIView

object IosIssMapRegistry {
    private var factory: (() -> UIView)? = null
    private var updater: ((UIView, IssMapPoint?, List<IssMapPoint>, String) -> Unit)? = null

    fun register(
        factory: () -> UIView,
        updater: (UIView, IssMapPoint?, List<IssMapPoint>, String) -> Unit
    ) {
        this.factory = factory
        this.updater = updater
    }

    fun create(): UIView = factory?.invoke() ?: UIView()

    fun update(view: UIView, current: IssMapPoint?, orbit: List<IssMapPoint>, mapType: String) {
        updater?.invoke(view, current, orbit, mapType)
    }

    private var insetUpdater: ((UIView, Double) -> Unit)? = null

    /** Receives the bottom inset in points so MapKit keeps its logo and legal link visible. */
    fun registerInsetUpdater(updater: (UIView, Double) -> Unit) {
        insetUpdater = updater
    }

    fun updateInset(view: UIView, bottomInsetPoints: Double) {
        insetUpdater?.invoke(view, bottomInsetPoints)
    }

    private var satelliteUpdater: ((UIView, List<SatelliteMapMarker>, List<SatelliteMapTrack>, Boolean, (String?) -> Unit) -> Unit)? = null

    /** Receives the spacecraft markers, their paths, whether the ISS orbit shows, and the tap callback. */
    fun registerSatelliteUpdater(
        updater: (UIView, List<SatelliteMapMarker>, List<SatelliteMapTrack>, Boolean, (String?) -> Unit) -> Unit
    ) {
        satelliteUpdater = updater
    }

    private var focusUpdater: ((UIView, MapFocusRequest) -> Unit)? = null

    /** Receives requests to move the map to a spacecraft; each new serial is a new request. */
    fun registerFocusUpdater(updater: (UIView, MapFocusRequest) -> Unit) {
        focusUpdater = updater
    }

    fun updateFocus(view: UIView, focus: MapFocusRequest) {
        focusUpdater?.invoke(view, focus)
    }

    fun updateSatellites(
        view: UIView,
        satellites: List<SatelliteMapMarker>,
        tracks: List<SatelliteMapTrack>,
        orbitVisible: Boolean,
        onSatelliteClick: (String?) -> Unit
    ) {
        satelliteUpdater?.invoke(view, satellites, tracks, orbitVisible, onSatelliteClick)
    }
}

object IosNativeAdRegistry {
    private var factory: ((String) -> UIView)? = null

    fun register(factory: (String) -> UIView) {
        this.factory = factory
    }

    fun create(slotId: String): UIView = factory?.invoke(slotId) ?: UIView()
}

object IosBannerAdRegistry {
    private var factory: (() -> UIView)? = null

    fun register(factory: () -> UIView) {
        this.factory = factory
    }

    fun create(): UIView = factory?.invoke() ?: UIView()
}

@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun PlatformIssMap(
    currentLocation: IssLocation?,
    orbit: List<IssMapPoint>,
    mapType: String,
    modifier: Modifier,
    bottomInset: Dp,
    satellites: List<SatelliteMapMarker>,
    tracks: List<SatelliteMapTrack>,
    orbitVisible: Boolean,
    focus: MapFocusRequest?,
    onSatelliteClick: (String?) -> Unit
) {
    val currentOnSatelliteClick by rememberUpdatedState(onSatelliteClick)
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        UIKitView(
            factory = IosIssMapRegistry::create,
            update = { view ->
                IosIssMapRegistry.update(
                    view = view,
                    current = currentLocation?.let {
                        IssMapPoint(
                            latitude = it.latitude,
                            longitude = it.longitude,
                            timestamp = it.timestamp,
                            footprint = it.footprint,
                            solarLatitude = it.solarLat,
                            solarLongitude = it.solarLon
                        )
                    },
                    orbit = orbit,
                    mapType = mapType
                )
                IosIssMapRegistry.updateInset(view, bottomInset.value.toDouble())
                IosIssMapRegistry.updateSatellites(
                    view = view,
                    satellites = satellites,
                    tracks = tracks,
                    orbitVisible = orbitVisible,
                    onSatelliteClick = { id -> currentOnSatelliteClick(id) }
                )
                focus?.let { IosIssMapRegistry.updateFocus(view, it) }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun PlatformNativeAd(
    slotId: String,
    modifier: Modifier
) {
    UIKitView(
        factory = { IosNativeAdRegistry.create(slotId) },
        modifier = modifier.clip(RoundedCornerShape(12.dp))
    )
}

@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun PlatformBannerAd(
    modifier: Modifier
) {
    UIKitView(
        factory = IosBannerAdRegistry::create,
        modifier = modifier
    )
}
