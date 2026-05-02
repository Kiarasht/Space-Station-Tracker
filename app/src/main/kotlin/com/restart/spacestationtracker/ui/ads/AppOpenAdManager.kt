package com.restart.spacestationtracker.ui.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppOpenAdManager @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val adsConsentManager: AdsConsentManager
) : Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var currentActivity: Activity? = null
    private var isAppInForeground = false
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTimeMillis = 0L
    private var pendingForegroundOpen = false
    private var pendingShowForCurrentForeground = false
    private var isRegistered = false
    private var suppressNextResumeAfterAd = false
    private var shownForCurrentForeground = false
    private var startedActivityCount = 0

    fun register(application: Application) {
        if (isRegistered) {
            return
        }
        isRegistered = true
        application.registerActivityLifecycleCallbacks(this)
    }

    fun onAdsReady(activity: Activity) {
        currentActivity = activity
        isAppInForeground = true
        Log.d(TAG, "Ads ready for current activity.")
        if (!shownForCurrentForeground && !pendingShowForCurrentForeground) {
            onAppForegrounded(activity)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (activity.isAdMobActivity()) {
            return
        }

        isAppInForeground = true
        if (startedActivityCount == 0) {
            pendingForegroundOpen = true
            pendingShowForCurrentForeground = false
            shownForCurrentForeground = false
        }
        startedActivityCount++
        currentActivity = activity
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity.isAdMobActivity()) {
            return
        }

        startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
        if (startedActivityCount == 0) {
            isAppInForeground = false
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity.isAdMobActivity()) {
            return
        }

        currentActivity = activity
        if (suppressNextResumeAfterAd) {
            suppressNextResumeAfterAd = false
            pendingForegroundOpen = false
            return
        }
        if (pendingForegroundOpen) {
            pendingForegroundOpen = false
            onAppForegrounded(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }

    private fun onAppForegrounded(activity: Activity) {
        if (settingsRepository.isAdFreeNow()) {
            Log.d(TAG, "Skipping app open ad because current session is ad-free.")
            return
        }
        if (!adsConsentManager.canRequestAds.value) {
            Log.d(TAG, "Skipping app open ad because consent does not allow ad requests yet.")
            return
        }

        val foregroundOpenCount = incrementForegroundOpenCount(activity.application)
        if (foregroundOpenCount >= FOREGROUND_OPENS_BEFORE_SHOWING) {
            pendingShowForCurrentForeground = true
            loadAndShowAd(activity)
        }
    }

    private fun loadAndShowAd(activity: Activity) {
        val application = activity.application
        if (
            isLoadingAd ||
            isShowingAd ||
            shownForCurrentForeground ||
            settingsRepository.isAdFreeNow() ||
            !adsConsentManager.canRequestAds.value
        ) {
            return
        }

        appOpenAd = null
        isLoadingAd = true
        AppOpenAd.load(
            application,
            application.getString(R.string.app_open_ad_unit_id),
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    loadTimeMillis = System.currentTimeMillis()
                    isLoadingAd = false
                    Log.d(TAG, "App open ad loaded.")
                    if (
                        currentActivity == activity &&
                        isAppInForeground &&
                        pendingShowForCurrentForeground
                    ) {
                        showAdIfAvailable(activity)
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isLoadingAd = false
                    Log.d(TAG, "App open ad failed to load: ${adError.message}")
                }
            }
        )
    }

    private fun showAdIfAvailable(activity: Activity) {
        if (
            isShowingAd ||
            shownForCurrentForeground ||
            settingsRepository.isAdFreeNow() ||
            !adsConsentManager.canRequestAds.value
        ) {
            return
        }

        val ad = appOpenAd.takeIf { isAdAvailable() }
        if (ad == null) {
            appOpenAd = null
            Log.d(TAG, "App open ad was not ready to show.")
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                suppressNextResumeAfterAd = true
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                Log.d(TAG, "App open ad failed to show: ${adError.message}")
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                pendingShowForCurrentForeground = false
                shownForCurrentForeground = true
                Log.d(TAG, "App open ad showed.")
            }
        }
        ad.show(activity)
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null &&
            System.currentTimeMillis() - loadTimeMillis < APP_OPEN_AD_MAX_AGE_MILLIS
    }

    private fun incrementForegroundOpenCount(application: Application): Int {
        val preferences = application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE)
        val foregroundOpenCount = preferences.getInt(KEY_FOREGROUND_OPEN_COUNT, 0) + 1
        preferences.edit().putInt(KEY_FOREGROUND_OPEN_COUNT, foregroundOpenCount).apply()
        Log.d(TAG, "Foreground open count: $foregroundOpenCount")
        return foregroundOpenCount
    }

    private fun getForegroundOpenCount(application: Application): Int {
        return application
            .getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE)
            .getInt(KEY_FOREGROUND_OPEN_COUNT, 0)
    }

    private fun Activity.isAdMobActivity(): Boolean {
        return javaClass.name == ADMOB_ACTIVITY_CLASS_NAME
    }

    private companion object {
        const val ADMOB_ACTIVITY_CLASS_NAME = "com.google.android.gms.ads.AdActivity"
        const val APP_OPEN_AD_MAX_AGE_MILLIS = 4L * 60 * 60 * 1000
        const val FOREGROUND_OPENS_BEFORE_SHOWING = 5
        const val KEY_FOREGROUND_OPEN_COUNT = "foreground_open_count"
        const val PREFS_NAME = "app_open_ads"
        const val TAG = "AppOpenAdManager"
    }
}
