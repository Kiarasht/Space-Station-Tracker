package com.restart.spacestationtracker.ui.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.restart.spacestationtracker.BuildConfig
import com.restart.spacestationtracker.data.settings.SettingsRepository
import com.restart.spacestationtracker.shared.preferences.AndroidPreferenceStore
import com.restart.spacestationtracker.shared.policy.AppOpenRepository
import com.restart.spacestationtracker.shared.policy.MonetizationPolicy
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
    private var loadTimeElapsedRealtime = 0L
    private var pendingForegroundOpen = false
    private var pendingShowForCurrentForeground = false
    private var isRegistered = false
    private var areAdsReady = false
    private var suppressNextResumeAfterAd = false
    private var shownForCurrentForeground = false
    private var handledCurrentForeground = false
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
        areAdsReady = true
        if (BuildConfig.DEBUG) Log.d(TAG, "Ads ready for current activity.")
        if (startedActivityCount > 0) {
            isAppInForeground = true
            onAppForegrounded(activity)
        } else {
            loadAdIfNeeded(activity.application)
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
            handledCurrentForeground = false
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
        if (handledCurrentForeground) {
            return
        }
        if (settingsRepository.isAdFreeNow()) {
            handledCurrentForeground = true
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Skipping app open ad because current session is ad-free.")
            }
            return
        }
        if (!areAdsReady || !adsConsentManager.canRequestAds.value) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Skipping app open ad because consent does not allow ad requests yet.")
            }
            return
        }

        handledCurrentForeground = true
        val foregroundOpenCount = incrementForegroundOpenCount(activity.application)
        if (foregroundOpenCount >= MonetizationPolicy.APP_OPEN_START_THRESHOLD) {
            pendingShowForCurrentForeground = true
            showAdIfAvailable(activity)
        } else {
            loadAdIfNeeded(activity.application)
        }
    }

    private fun loadAdIfNeeded(application: Application) {
        if (
            isLoadingAd ||
            isShowingAd ||
            isAdAvailable() ||
            !areAdsReady ||
            settingsRepository.isAdFreeNow() ||
            !adsConsentManager.canRequestAds.value
        ) {
            return
        }

        appOpenAd = null
        isLoadingAd = true
        AppOpenAd.load(
            application,
            AdMobIds.appOpen(application),
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    loadTimeElapsedRealtime = SystemClock.elapsedRealtime()
                    isLoadingAd = false
                    if (BuildConfig.DEBUG) Log.d(TAG, "App open ad loaded.")
                    val activity = currentActivity
                    if (activity != null && isAppInForeground && pendingShowForCurrentForeground) {
                        showAdIfAvailable(activity)
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    appOpenAd = null
                    isLoadingAd = false
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "App open ad failed to load: ${adError.message}")
                    }
                }
            }
        )
    }

    private fun showAdIfAvailable(activity: Activity) {
        if (
            isShowingAd ||
            shownForCurrentForeground ||
            !isAppInForeground ||
            activity.isFinishing ||
            activity.isDestroyed ||
            settingsRepository.isAdFreeNow() ||
            !adsConsentManager.canRequestAds.value
        ) {
            return
        }

        val ad = appOpenAd.takeIf { isAdAvailable() }
        if (ad == null) {
            appOpenAd = null
            if (BuildConfig.DEBUG) Log.d(TAG, "App open ad was not ready to show.")
            loadAdIfNeeded(activity.application)
            return
        }

        pendingShowForCurrentForeground = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                suppressNextResumeAfterAd = true
                loadAdIfNeeded(activity.application)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "App open ad failed to show: ${adError.message}")
                }
                loadAdIfNeeded(activity.application)
            }

            override fun onAdShowedFullScreenContent() {
                appOpenAd = null
                isShowingAd = true
                shownForCurrentForeground = true
                if (BuildConfig.DEBUG) Log.d(TAG, "App open ad showed.")
            }
        }
        ad.show(activity)
    }

    private fun isAdAvailable(): Boolean {
        val ageMillis = SystemClock.elapsedRealtime() - loadTimeElapsedRealtime
        return appOpenAd != null &&
            ageMillis < MonetizationPolicy.APP_OPEN_EXPIRATION_MILLIS
    }

    private fun incrementForegroundOpenCount(application: Application): Int {
        val foregroundOpenCount = AppOpenRepository(
            AndroidPreferenceStore(
                context = application,
                name = AppOpenRepository.PREFS_NAME
            )
        ).recordAppOpen()
        if (BuildConfig.DEBUG) Log.d(TAG, "Foreground open count: $foregroundOpenCount")
        return foregroundOpenCount
    }

    private fun Activity.isAdMobActivity(): Boolean {
        return javaClass.name == ADMOB_ACTIVITY_CLASS_NAME
    }

    private companion object {
        const val ADMOB_ACTIVITY_CLASS_NAME = "com.google.android.gms.ads.AdActivity"
        const val TAG = "AppOpenAdManager"
    }
}
