package com.restart.spacestationtracker.shared.network

import platform.Foundation.NSBundle

/** The localization iOS picked for this app, such as "zh-Hans" or "pt-BR", reduced to its language. */
internal actual fun appLanguageCode(): String =
    (NSBundle.mainBundle.preferredLocalizations.firstOrNull() as? String)
        ?.substringBefore('-')
        ?: "en"
