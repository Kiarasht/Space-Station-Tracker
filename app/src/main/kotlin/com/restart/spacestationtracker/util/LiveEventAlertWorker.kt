package com.restart.spacestationtracker.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.restart.spacestationtracker.data.settings.SettingsRepository
import com.restart.spacestationtracker.domain.youtube.repository.YouTubeRepository
import com.restart.spacestationtracker.shared.liveevents.LiveEventAlertPolicy
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

/** Replaces the live-event alarms with alerts for NASA's currently scheduled broadcasts. */
class LiveEventAlertWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            LiveEventAlertWorkerEntryPoint::class.java
        )
        val settingsRepository = entryPoint.settingsRepository()
        val settings = settingsRepository.appSettingsFlow.first()
        val scheduler = LiveEventAlertScheduler(appContext)

        if (!settings.liveEventAlertsEnabled) {
            scheduler.cancel(settings.liveEventAlertScheduledIds)
            settingsRepository.setLiveEventAlertScheduledIds(emptySet())
            return Result.success()
        }

        return entryPoint.youTubeRepository().getUpcomingLiveEvents().fold(
            onSuccess = { events ->
                // Alerts may have been switched off while the list was loading.
                if (!settingsRepository.appSettingsFlow.first().liveEventAlertsEnabled) return@fold Result.success()
                scheduler.cancel(settings.liveEventAlertScheduledIds)
                val alerts = LiveEventAlertPolicy.alertsFor(events, nowMillis = System.currentTimeMillis())
                settingsRepository.setLiveEventAlertScheduledIds(scheduler.schedule(alerts))
                Result.success()
            },
            // Keeps the alarms already set; the next run tries again.
            onFailure = { Result.retry() }
        )
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LiveEventAlertWorkerEntryPoint {
    fun settingsRepository(): SettingsRepository
    fun youTubeRepository(): YouTubeRepository
}
