package com.restart.spacestationtracker.shared.ui.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
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
    modifier: Modifier
) {
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
