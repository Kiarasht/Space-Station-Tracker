package com.restart.spacestationtracker.shared.liveevents

import com.restart.spacestationtracker.domain.youtube.model.ScheduledLiveEvent

/** A local notification shortly before a scheduled NASA broadcast. */
data class LiveEventAlert(
    val videoId: String,
    val title: String,
    val scheduledStartMillis: Long,
    val triggerTimeMillis: Long
) {
    /** Stable per broadcast, so a refresh replaces its alert instead of adding a second one. */
    val identifier: String get() = "$IDENTIFIER_PREFIX$videoId"
    val watchUrl: String get() = LiveEventAlertPolicy.watchUrl(videoId)

    companion object {
        const val IDENTIFIER_PREFIX = "live-event-"
    }
}

object LiveEventAlertPolicy {
    /** Alerts go out this long before a broadcast is scheduled to start. */
    const val LEAD_TIME_MILLIS = 10L * 60 * 1_000

    /** Keeps room in iOS's 64 pending notifications for pass alerts. */
    const val MAXIMUM_ALERTS = 10

    /** Alerts for the broadcasts still far enough ahead to warn about, soonest first. */
    fun alertsFor(events: List<ScheduledLiveEvent>, nowMillis: Long): List<LiveEventAlert> =
        events
            .filter { it.scheduledStartMillis - LEAD_TIME_MILLIS > nowMillis }
            .distinctBy { it.videoId }
            .sortedBy { it.scheduledStartMillis }
            .take(MAXIMUM_ALERTS)
            .map { event ->
                LiveEventAlert(
                    videoId = event.videoId,
                    title = event.title,
                    scheduledStartMillis = event.scheduledStartMillis,
                    triggerTimeMillis = event.scheduledStartMillis - LEAD_TIME_MILLIS
                )
            }

    fun watchUrl(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"
}
