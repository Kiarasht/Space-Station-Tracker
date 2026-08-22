package com.restart.spacestationtracker.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Privacy-safe product analytics. Values accepted here are fixed app vocabulary;
 * never pass coordinates, location names, URLs, person names, transaction data,
 * user-entered text, or raw exception messages.
 */
object AppAnalytics {
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var currentScreen = "unknown"

    fun initialize(context: Context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext).also {
            it.setAnalyticsCollectionEnabled(true)
            it.setUserProperty("platform", "android")
        }
    }

    fun trackScreen(route: String?) {
        val screenName = screenNameForRoute(route)
        currentScreen = screenName
        firebaseAnalytics?.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
            }
        )
    }

    fun trackInteraction(action: String, screen: String = currentScreen) {
        firebaseAnalytics?.logEvent(
            "feature_interaction",
            Bundle().apply {
                putString("screen_name", sanitize(screen))
                putString("action", sanitize(action))
            }
        )
    }

    fun trackSetting(setting: String, value: String) {
        firebaseAnalytics?.logEvent(
            "setting_changed",
            Bundle().apply {
                putString("setting_name", sanitize(setting))
                putString("setting_value", sanitize(value))
            }
        )
    }

    fun trackPurchaseFlow(stage: String) {
        firebaseAnalytics?.logEvent(
            "ad_removal_flow",
            Bundle().apply { putString("stage", sanitize(stage)) }
        )
    }

    fun updateAdFreeState(isAdFree: Boolean) {
        firebaseAnalytics?.setUserProperty("ad_free", isAdFree.toString())
    }

    private fun screenNameForRoute(route: String?): String = when {
        route == "Map" -> "map"
        route == "Sky Path" -> "sky_path"
        route == "On Duty" -> "on_duty"
        route == "Settings" -> "settings"
        route == "About" -> "about"
        route?.startsWith("legal/") == true -> "legal"
        else -> "unknown"
    }

    private fun sanitize(value: String): String = value
        .lowercase()
        .replace(Regex("[^a-z0-9_]+"), "_")
        .trim('_')
        .take(40)
        .ifEmpty { "unknown" }
}
