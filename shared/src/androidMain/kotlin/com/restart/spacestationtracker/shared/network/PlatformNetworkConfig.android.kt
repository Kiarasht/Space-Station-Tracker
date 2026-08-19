package com.restart.spacestationtracker.shared.network

internal actual object PlatformNetworkConfig {
    actual val n2yoApiKey: String
        get() = appBuildConfigString("N2YO_API_KEY")
            .ifBlank { System.getenv("N2YO_API_KEY").orEmpty() }

    actual val platformName: String = "Android"
}

private fun appBuildConfigString(fieldName: String): String {
    return runCatching {
        Class.forName("com.restart.spacestationtracker.BuildConfig")
            .getField(fieldName)
            .get(null) as? String
    }.getOrNull().orEmpty()
}
