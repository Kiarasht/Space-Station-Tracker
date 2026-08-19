package com.restart.spacestationtracker.shared.policy

import com.restart.spacestationtracker.shared.preferences.PreferenceStore

class AppOpenRepository(
    private val store: PreferenceStore
) {
    fun getAppOpenCount(): Int {
        return (store.getInt(KEY_FOREGROUND_OPEN_COUNT) ?: 0).coerceAtLeast(0)
    }

    fun recordAppOpen(): Int {
        val updatedCount = getAppOpenCount() + 1
        store.putInt(KEY_FOREGROUND_OPEN_COUNT, updatedCount)
        return updatedCount
    }

    companion object {
        const val PREFS_NAME = "app_open_ads"
        const val KEY_FOREGROUND_OPEN_COUNT = "foreground_open_count"
    }
}

object MonetizationPolicy {
    const val APP_OPEN_START_THRESHOLD = 5
    const val APP_OPEN_EXPIRATION_MILLIS = 4L * 60L * 60L * 1000L
}
