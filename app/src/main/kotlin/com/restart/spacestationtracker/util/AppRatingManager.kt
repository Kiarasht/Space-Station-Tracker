package com.restart.spacestationtracker.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.edit
import com.restart.spacestationtracker.BuildConfig
import java.util.concurrent.TimeUnit

class AppRatingManager(private val context: Context) {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordAppLaunch() {
        val now = System.currentTimeMillis()
        preferences.edit {
            if (!preferences.contains(KEY_FIRST_LAUNCH_TIME)) {
                putLong(KEY_FIRST_LAUNCH_TIME, now)
            }
            putInt(KEY_LAUNCH_COUNT, preferences.getInt(KEY_LAUNCH_COUNT, 0) + 1)
        }
    }

    fun recordScreenVisit(route: String?) {
        if (route == null || route.startsWith("legal")) return

        preferences.edit {
            putInt(KEY_SCREEN_VISIT_COUNT, preferences.getInt(KEY_SCREEN_VISIT_COUNT, 0) + 1)
        }
    }

    fun shouldShowPrompt(): Boolean {
        if (preferences.getBoolean(KEY_HAS_RATED, false)) return false

        val now = System.currentTimeMillis()
        val firstLaunchTime = preferences.getLong(KEY_FIRST_LAUNCH_TIME, now)
        val snoozedUntil = preferences.getLong(KEY_SNOOZED_UNTIL, 0L)
        val launchCount = preferences.getInt(KEY_LAUNCH_COUNT, 0)
        val screenVisitCount = preferences.getInt(KEY_SCREEN_VISIT_COUNT, 0)

        return now >= snoozedUntil &&
            now - firstLaunchTime >= MIN_USAGE_AGE_MILLIS &&
            launchCount >= MIN_LAUNCH_COUNT &&
            screenVisitCount >= MIN_SCREEN_VISITS
    }

    fun snoozePrompt() {
        preferences.edit {
            putLong(KEY_SNOOZED_UNTIL, System.currentTimeMillis() + SNOOZE_DURATION_MILLIS)
        }
    }

    fun markRatedAndOpenStore() {
        preferences.edit {
            putBoolean(KEY_HAS_RATED, true)
        }
        openStoreListing()
    }

    private fun openStoreListing() {
        val installerPackageName = getInstallerPackageName()
        val intent = when (installerPackageName) {
            GALAXY_STORE_PACKAGE -> Intent(
                Intent.ACTION_VIEW,
                Uri.parse("samsungapps://ProductDetail/${BuildConfig.APPLICATION_ID}")
            )
            else -> Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=${BuildConfig.APPLICATION_ID}")
            )
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun getInstallerPackageName(): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager
                    .getInstallSourceInfo(BuildConfig.APPLICATION_ID)
                    .installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(BuildConfig.APPLICATION_ID)
            }
        } catch (_: RuntimeException) {
            null
        }
    }

    private companion object {
        const val GALAXY_STORE_PACKAGE = "com.sec.android.app.samsungapps"
        const val KEY_FIRST_LAUNCH_TIME = "first_launch_time"
        const val KEY_HAS_RATED = "has_rated"
        const val KEY_LAUNCH_COUNT = "launch_count"
        const val KEY_SCREEN_VISIT_COUNT = "screen_visit_count"
        const val KEY_SNOOZED_UNTIL = "snoozed_until"
        const val MIN_LAUNCH_COUNT = 5
        const val MIN_SCREEN_VISITS = 15
        const val PREFS_NAME = "app_rating"
        val MIN_USAGE_AGE_MILLIS: Long = TimeUnit.DAYS.toMillis(3)
        val SNOOZE_DURATION_MILLIS: Long = TimeUnit.DAYS.toMillis(14)
    }
}
