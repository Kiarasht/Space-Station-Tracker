package com.restart.spacestationtracker.shared.network

import platform.Foundation.NSBundle
import platform.Foundation.NSProcessInfo

internal actual object PlatformNetworkConfig {
    actual val n2yoApiKey: String
        get() = listOf(
            NSBundle.mainBundle.objectForInfoDictionaryKey("ISSTrackerN2yoApiKey") as? String,
            NSProcessInfo.processInfo.environment["N2YO_API_KEY"] as? String
        ).firstOrNull { value ->
            !value.isNullOrBlank() && !value.contains("\$(")
        }.orEmpty()

    actual val platformName: String = "iOS"
}
