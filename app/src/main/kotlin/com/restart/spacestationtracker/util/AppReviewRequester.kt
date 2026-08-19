package com.restart.spacestationtracker.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import com.google.android.play.core.review.ReviewManagerFactory
import com.restart.spacestationtracker.shared.policy.AppReviewRepository

object AppReviewRequester {
    private const val GOOGLE_PLAY_INSTALLER = "com.android.vending"
    private const val GALAXY_STORE_INSTALLER = "com.sec.android.app.samsungapps"
    private const val LEGACY_GALAXY_STORE_INSTALLER = "com.samsung.android.app.galaxyapps"

    private var requestInFlight = false
    private var galaxyDialog: AlertDialog? = null

    fun maybeRequestReview(
        activity: Activity,
        preferences: AppRatingManager
    ) {
        if (requestInFlight || activity.isFinishing || activity.isDestroyed) return
        if (!preferences.shouldShowPrompt()) return

        when (activity.installStore()) {
            InstallStore.GooglePlay -> requestGooglePlayReview(activity, preferences)
            InstallStore.GalaxyStore -> showGalaxyStoreReviewPrompt(activity, preferences)
            InstallStore.Other -> {
                preferences.snoozePrompt(AppReviewRepository.SHORT_RETRY_DELAY_MILLIS)
            }
        }
    }

    private fun requestGooglePlayReview(
        activity: Activity,
        preferences: AppRatingManager
    ) {
        requestInFlight = true
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow()
            .addOnCompleteListener { requestTask ->
                if (!requestTask.isSuccessful) {
                    requestInFlight = false
                    preferences.snoozePrompt(AppReviewRepository.SHORT_RETRY_DELAY_MILLIS)
                    return@addOnCompleteListener
                }

                manager.launchReviewFlow(activity, requestTask.result)
                    .addOnCompleteListener {
                        requestInFlight = false
                        preferences.markReviewFlowCompleted()
                    }
            }
    }

    private fun showGalaxyStoreReviewPrompt(
        activity: Activity,
        preferences: AppRatingManager
    ) {
        if (galaxyDialog?.isShowing == true) return

        galaxyDialog = AlertDialog.Builder(activity)
            .setTitle("Enjoying ISS Tracker?")
            .setMessage("A quick Galaxy Store review helps other sky watchers find the app.")
            .setPositiveButton("Review") { _, _ ->
                preferences.markReviewFlowCompleted()
                activity.openGalaxyStoreReviewPage()
            }
            .setNegativeButton("Not Now") { _, _ ->
                preferences.snoozePrompt(AppReviewRepository.USER_DISMISS_DELAY_MILLIS)
            }
            .create()
            .also { dialog ->
                dialog.setOnCancelListener {
                    preferences.snoozePrompt(AppReviewRepository.USER_DISMISS_DELAY_MILLIS)
                }
                dialog.setOnDismissListener {
                    if (galaxyDialog === dialog) {
                        galaxyDialog = null
                    }
                }
                dialog.show()
            }
    }

    private fun Context.openGalaxyStoreReviewPage() {
        val appPackageName = packageName
        val reviewIntent = Intent(
            Intent.ACTION_VIEW,
            "samsungapps://AppRating/$appPackageName".toUri()
        ).apply {
            setPackage(GALAXY_STORE_INSTALLER)
        }
        if (reviewIntent.resolveActivity(packageManager) != null) {
            startActivity(reviewIntent)
            return
        }

        runCatching {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://apps.samsung.com/appquery/AppRating.as?appId=$appPackageName".toUri()
                )
            )
        }
    }

    private fun Context.installStore(): InstallStore {
        val installerPackageName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            }.getOrNull()
        } else {
            @Suppress("DEPRECATION")
            runCatching {
                packageManager.getInstallerPackageName(packageName)
            }.getOrNull()
        }

        return when (installerPackageName) {
            GOOGLE_PLAY_INSTALLER -> InstallStore.GooglePlay
            GALAXY_STORE_INSTALLER,
            LEGACY_GALAXY_STORE_INSTALLER -> InstallStore.GalaxyStore
            else -> InstallStore.Other
        }
    }

    private enum class InstallStore {
        GooglePlay,
        GalaxyStore,
        Other
    }
}
