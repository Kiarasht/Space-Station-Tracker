package com.restart.spacestationtracker.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import com.restart.spacestationtracker.BuildConfig
import com.restart.spacestationtracker.shared.policy.AppReviewRepository
import com.restart.spacestationtracker.shared.preferences.AndroidPreferenceStore

class AppRatingManager(
    private val context: Context
) {
    private val repository = AppReviewRepository(
        AndroidPreferenceStore(
            context = context.applicationContext,
            name = AppReviewRepository.PREFS_NAME
        )
    )

    fun recordAppLaunch(nowMillis: Long = System.currentTimeMillis()) {
        repository.recordAppOpened(nowMillis)
    }

    fun recordScreenVisit(route: String?, nowMillis: Long = System.currentTimeMillis()) {
        if (route == null || route.startsWith("legal")) return
        repository.recordMeaningfulInteraction(nowMillis)
    }

    fun shouldShowPrompt(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return repository.isEligibleForReviewPrompt(nowMillis)
    }

    fun snoozePrompt(
        durationMillis: Long = AppReviewRepository.USER_DISMISS_DELAY_MILLIS,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        repository.snoozeReviewPrompt(durationMillis, nowMillis)
    }

    fun markReviewFlowCompleted(nowMillis: Long = System.currentTimeMillis()) {
        repository.markReviewFlowCompleted(nowMillis)
    }

    fun markRatedAndOpenStore() {
        markReviewFlowCompleted()
        val installerPackageName = getInstallerPackageName()
        val uri = if (installerPackageName == GALAXY_STORE_PACKAGE) {
            "samsungapps://ProductDetail/${BuildConfig.APPLICATION_ID}".toUri()
        } else {
            "market://details?id=${BuildConfig.APPLICATION_ID}".toUri()
        }

        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}".toUri()
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun getInstallerPackageName(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                context.packageManager
                    .getInstallSourceInfo(BuildConfig.APPLICATION_ID)
                    .installingPackageName
            }.getOrNull()
        } else {
            @Suppress("DEPRECATION")
            runCatching {
                context.packageManager.getInstallerPackageName(BuildConfig.APPLICATION_ID)
            }.getOrNull()
        }
    }

    private companion object {
        const val GALAXY_STORE_PACKAGE = "com.sec.android.app.samsungapps"
    }
}
