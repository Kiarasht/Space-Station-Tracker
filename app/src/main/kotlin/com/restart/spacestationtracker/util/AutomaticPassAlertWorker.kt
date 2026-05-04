package com.restart.spacestationtracker.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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

        val latitude = settings.automaticPassAlertLatitude ?: return Result.success()
        val longitude = settings.automaticPassAlertLongitude ?: return Result.success()
        val altitude = settings.automaticPassAlertAltitude ?: 0.0

        val userLocation = UserLocation(
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            name = settings.automaticPassAlertLocationName ?: "Saved Location"
        )

        return entryPoint.getIssPassesUseCase()(userLocation)
            .fold(
                onSuccess = { passes ->
                    notificationScheduler.cancelAutomaticNotifications(settings.automaticPassAlertScheduledIds)
                    val scheduledIds = passes
                        .filter {
                            IssPassVisibility.matchesMinimum(
                                magnitude = it.magnitude,
                                minimumVisibility = settings.automaticPassAlertMinVisibility
                            )
                        }
                        .flatMap {
                            notificationScheduler.scheduleAutomaticNotifications(
                                pass = it,
                                notificationTimes = settings.automaticPassAlertNotificationTimes
                            )
                        }
                        .toSet()

                    settingsRepository.setAutomaticPassAlertScheduledIds(scheduledIds)
                    Result.success()
                },
                onFailure = {
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
