package com.restart.spacestationtracker.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutomaticPassAlertWorkScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val workManager = WorkManager.getInstance(context)

    fun scheduleDailySync() {
        val request = PeriodicWorkRequestBuilder<AutomaticPassAlertWorker>(1, TimeUnit.DAYS)
            .setConstraints(networkConstraints())
            .build()
        workManager.enqueueUniquePeriodicWork(
            DAILY_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun runImmediateSync() {
        val request = OneTimeWorkRequestBuilder<AutomaticPassAlertWorker>()
            .setConstraints(networkConstraints())
            .build()
        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelDailySync() {
        workManager.cancelUniqueWork(DAILY_WORK_NAME)
        workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME)
    }

    private fun networkConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    companion object {
        const val DAILY_WORK_NAME = "automatic_iss_pass_alert_daily_sync"
        const val IMMEDIATE_WORK_NAME = "automatic_iss_pass_alert_immediate_sync"
    }
}
