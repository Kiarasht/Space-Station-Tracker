package com.restart.spacestationtracker

import android.app.Application
import com.restart.spacestationtracker.analytics.AppAnalytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppAnalytics.initialize(this)
    }
}
