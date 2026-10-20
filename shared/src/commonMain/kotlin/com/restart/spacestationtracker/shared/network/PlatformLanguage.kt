package com.restart.spacestationtracker.shared.network

/** The two-letter language the app is showing, such as "de" or "zh"; used to pick a Wikipedia edition. */
internal expect fun appLanguageCode(): String

/** Wikipedia edition for an app language; Android still reports Indonesian with its old code "in". */
internal fun wikipediaLanguage(appLanguage: String): String = when (val code = appLanguage.lowercase()) {
    "in" -> "id"
    "iw" -> "he"
    else -> code
}
