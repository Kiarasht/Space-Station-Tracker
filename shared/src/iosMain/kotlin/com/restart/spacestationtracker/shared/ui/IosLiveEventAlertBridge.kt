package com.restart.spacestationtracker.shared.ui

import com.restart.spacestationtracker.shared.liveevents.LiveEventAlert
import com.restart.spacestationtracker.shared.liveevents.LiveEventAlertPolicy
import com.restart.spacestationtracker.shared.network.KtorSpaceStationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

data class IosLiveEventAlertPayload(
    val identifier: String,
    val title: String,
    val scheduledStartMillis: Long,
    val triggerTimeMillis: Long,
    val watchUrl: String
)

/** Loads NASA's scheduled broadcasts and turns them into the alerts iOS should schedule. */
class IosLiveEventAlertBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val repository = KtorSpaceStationRepository()

    val identifierPrefix: String = LiveEventAlert.IDENTIFIER_PREFIX

    fun loadAlerts(
        nowMillis: Long,
        onSuccess: (List<IosLiveEventAlertPayload>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        scope.launch {
            repository.getUpcomingLiveEvents()
                .onSuccess { events ->
                    onSuccess(
                        LiveEventAlertPolicy.alertsFor(events, nowMillis).map { alert ->
                            IosLiveEventAlertPayload(
                                identifier = alert.identifier,
                                title = alert.title,
                                scheduledStartMillis = alert.scheduledStartMillis,
                                triggerTimeMillis = alert.triggerTimeMillis,
                                watchUrl = alert.watchUrl
                            )
                        }
                    )
                }
                .onFailure { error -> onFailure(error.message ?: "Unable to load scheduled broadcasts.") }
        }
    }
}
