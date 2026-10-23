package com.restart.spacestationtracker.shared.ui

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.restart.spacestationtracker.shared.network.createPlatformHttpClient

/**
 * Coil's loader with the app's own HTTP client, so image hosts see the app's User-Agent. Wikimedia, which
 * hosts the crew and expedition photos, answers OkHttp's default User-Agent with 403 Forbidden.
 */
fun spaceImageLoader(context: PlatformContext): ImageLoader =
    ImageLoader.Builder(context)
        .components {
            add(KtorNetworkFetcherFactory(httpClient = { createPlatformHttpClient() }))
        }
        .build()

/** Makes [spaceImageLoader] Coil's singleton, for hosts that don't render [SharedAppRoot] (the Android app). */
fun installSpaceImageLoader() {
    SingletonImageLoader.setSafe { context -> spaceImageLoader(context) }
}
