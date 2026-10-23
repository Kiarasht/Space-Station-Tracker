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

/**
 * Keeps live-event alerts current. The list is one small download, so it's checked every 6 hours (passes are
 * checked daily): NASA usually schedules broadcasts a day or more ahead.
 */
@Singleton
class LiveEventAlertWorkScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<LiveEventAlertWorker>(6, TimeUnit.HOURS)
            .setConstraints(networkConstraints())
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun runImmediateSync() {
        val request = OneTimeWorkRequestBuilder<LiveEventAlertWorker>()
            .setConstraints(networkConstraints())
            .build()
        workManager.enqueueUniqueWork(IMMEDIATE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelSync() {
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
        workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME)
    }

    private fun networkConstraints(): Constraints =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    private companion object {
        const val PERIODIC_WORK_NAME = "live_event_alert_periodic_sync"
        const val IMMEDIATE_WORK_NAME = "live_event_alert_immediate_sync"
    }
}
