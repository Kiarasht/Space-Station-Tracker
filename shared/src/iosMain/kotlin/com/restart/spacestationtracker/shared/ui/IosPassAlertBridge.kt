package com.restart.spacestationtracker.shared.ui

import com.restart.spacestationtracker.shared.network.KtorSpaceStationRepository
import com.restart.spacestationtracker.shared.passes.PassAlertPolicy
import com.restart.spacestationtracker.shared.preferences.NSUserDefaultsPreferenceStore
import com.restart.spacestationtracker.shared.settings.SharedSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

data class IosNotificationSchedule(
    val notificationTime: String,
    val triggerTimeMillis: Long,
    val identifierSuffix: String
)

data class IosPassAlertPayload(
    val startTimeMillis: Long,
    val durationInSeconds: Int,
    val magnitude: Double,
    val maxElevation: Double,
    val startAzimuthCompass: String,
    val endAzimuthCompass: String,
    val notificationTime: String,
    val triggerTimeMillis: Long,
    val identifierSuffix: String
)

class IosPassAlertBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val repository = KtorSpaceStationRepository()

    fun createNotificationSchedules(
        startTimeMillis: Long,
        notificationTimes: List<String>,
        nowMillis: Long
    ): List<IosNotificationSchedule> {
        return PassAlertPolicy.remindersFor(
            startTimeMillis = startTimeMillis,
            notificationTimes = notificationTimes,
            nowMillis = nowMillis
        ).map { reminder ->
            IosNotificationSchedule(
                notificationTime = reminder.notificationTime,
                triggerTimeMillis = reminder.triggerTimeMillis,
                identifierSuffix = reminder.identifierSuffix
            )
        }
    }

    fun loadAutomaticSchedules(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        nowMillis: Long,
        onSuccess: (List<IosPassAlertPayload>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        scope.launch {
            val settings = SharedSettingsRepository(
                NSUserDefaultsPreferenceStore(SETTINGS_SUITE_NAME)
            ).settings.value
            repository.getIssPasses(latitude, longitude, altitude)
                .onSuccess { passes ->
                    onSuccess(
                        passes
                            .filter { pass ->
                                PassAlertPolicy.matchesMinimumVisibility(
                                    magnitude = pass.magnitude,
                                    minimumVisibility = settings.automaticPassAlertMinVisibility
                                )
                            }
                            .take(MAXIMUM_AUTOMATIC_PASSES)
                            .flatMap { pass ->
                                PassAlertPolicy.remindersFor(
                                    startTimeMillis = pass.startTimeMillis,
                                    notificationTimes = settings.automaticPassAlertNotificationTimes,
                                    nowMillis = nowMillis
                                ).map { reminder ->
                                    IosPassAlertPayload(
                                        startTimeMillis = pass.startTimeMillis,
                                        durationInSeconds = pass.durationInSeconds,
                                        magnitude = pass.magnitude,
                                        maxElevation = pass.maxElevation,
                                        startAzimuthCompass = pass.startAzimuthCompass,
                                        endAzimuthCompass = pass.endAzimuthCompass,
                                        notificationTime = reminder.notificationTime,
                                        triggerTimeMillis = reminder.triggerTimeMillis,
                                        identifierSuffix = reminder.identifierSuffix
                                    )
                                }
                            }
                    )
                }
                .onFailure { error ->
                    onFailure(error.message ?: "Unable to load visible passes.")
                }
        }
    }

    private companion object {
        const val SETTINGS_SUITE_NAME = "settings"
        const val MAXIMUM_AUTOMATIC_PASSES = 10
    }
}
