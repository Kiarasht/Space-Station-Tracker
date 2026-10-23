package com.restart.spacestationtracker

import android.app.Application
import com.restart.spacestationtracker.analytics.AppAnalytics
import com.restart.spacestationtracker.shared.ui.installSpaceImageLoader
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppAnalytics.initialize(this)
        // The crew screens load photos through Coil; give it the app's HTTP client and User-Agent.
        installSpaceImageLoader()
    }
}
