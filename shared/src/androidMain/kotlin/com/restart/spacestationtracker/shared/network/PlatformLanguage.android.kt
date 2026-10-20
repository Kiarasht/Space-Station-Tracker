package com.restart.spacestationtracker.shared.network

import java.util.Locale

internal actual fun appLanguageCode(): String = Locale.getDefault().language
