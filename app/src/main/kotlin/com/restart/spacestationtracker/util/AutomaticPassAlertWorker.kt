package com.restart.spacestationtracker.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.data.settings.SettingsRepository
import com.restart.spacestationtracker.domain.iss_passes.use_case.GetIssPassesUseCase
import com.restart.spacestationtracker.domain.iss_passes.use_case.UserLocation
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import kotlinx.coroutines.flow.first

class AutomaticPassAlertWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            AutomaticPassAlertWorkerEntryPoint::class.java
        )
        val settingsRepository = entryPoint.settingsRepository()
        val settings = settingsRepository.appSettingsFlow.first()
        val notificationScheduler = NotificationScheduler(appContext)

        if (!settings.automaticPassAlertsEnabled) {
            notificationScheduler.cancelAutomaticNotifications(settings.automaticPassAlertScheduledIds)
            settingsRepository.clearAutomaticPassAlertScheduledIds()
            return Result.success()
        }

        val latitude = settings.automaticPassAlertLatitude
        val longitude = settings.automaticPassAlertLongitude
        if (latitude == null || longitude == null) {
            settingsRepository.setAutomaticPassAlertSyncStatus(
                timestampMillis = System.currentTimeMillis(),
                result = appContext.getString(R.string.auto_alert_sync_needs_location),
                message = appContext.getString(R.string.auto_alert_sync_needs_location_message)
            )
            return Result.success()
        }
        val altitude = settings.automaticPassAlertAltitude ?: 0.0

        val userLocation = UserLocation(
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            name = settings.automaticPassAlertLocationName
                ?: appContext.getString(R.string.auto_alert_saved_location)
        )

        return entryPoint.getIssPassesUseCase()(userLocation)
            .fold(
                onSuccess = { passes ->
                    notificationScheduler.cancelAutomaticNotifications(settings.automaticPassAlertScheduledIds)
                    val matchingPasses = passes
                        .filter {
                            IssPassVisibility.matchesMinimum(
                                magnitude = it.magnitude,
                                minimumVisibility = settings.automaticPassAlertMinVisibility
                            )
                        }
                    val scheduledIds = matchingPasses
                        .flatMap {
                            notificationScheduler.scheduleAutomaticNotifications(
                                pass = it,
                                notificationTimes = settings.automaticPassAlertNotificationTimes
                            )
                        }
                        .toSet()

                    settingsRepository.setAutomaticPassAlertScheduledIds(scheduledIds)
                    settingsRepository.setAutomaticPassAlertSyncStatus(
                        timestampMillis = System.currentTimeMillis(),
                        result = appContext.getString(R.string.auto_alert_sync_success),
                        message = if (scheduledIds.isEmpty()) {
                            appContext.getString(R.string.auto_alert_sync_no_matches)
                        } else {
                            appContext.getString(
                                R.string.auto_alert_sync_scheduled_format,
                                matchingPasses.size,
                                scheduledIds.size
                            )
                        }
                    )
                    Result.success()
                },
                onFailure = { throwable ->
                    settingsRepository.setAutomaticPassAlertSyncStatus(
                        timestampMillis = System.currentTimeMillis(),
                        result = appContext.getString(R.string.auto_alert_sync_failed),
                        message = throwable.localizedMessage
                            ?: appContext.getString(R.string.auto_alert_sync_retry_message)
                    )
                    Result.retry()
                }
            )
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AutomaticPassAlertWorkerEntryPoint {
    fun settingsRepository(): SettingsRepository
    fun getIssPassesUseCase(): GetIssPassesUseCase
}
