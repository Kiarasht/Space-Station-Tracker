package com.restart.spacestationtracker.shared.liveevents

import com.restart.spacestationtracker.domain.youtube.model.ScheduledLiveEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class LiveEventAlertPolicyTest {
    private val now = 1_000_000_000L
    private val minute = 60_000L

    @Test
    fun alertsFireTheLeadTimeBeforeEachBroadcastSoonestFirst() {
        val alerts = LiveEventAlertPolicy.alertsFor(
            listOf(
                ScheduledLiveEvent("later", "Launch", now + 3 * 60 * minute),
                ScheduledLiveEvent("sooner", "Spacewalk", now + 60 * minute)
            ),
            nowMillis = now
        )

        assertEquals(listOf("sooner", "later"), alerts.map { it.videoId })
        assertEquals(now + 50 * minute, alerts.first().triggerTimeMillis)
        assertEquals("live-event-sooner", alerts.first().identifier)
        assertEquals("https://www.youtube.com/watch?v=sooner", alerts.first().watchUrl)
    }

    @Test
    fun broadcastsTooCloseToWarnAboutOrDuplicatedAreLeftOut() {
        val alerts = LiveEventAlertPolicy.alertsFor(
            listOf(
                ScheduledLiveEvent("started", "Docking", now - minute),
                ScheduledLiveEvent("imminent", "Undocking", now + 5 * minute),
                ScheduledLiveEvent("ahead", "Spacewalk", now + 30 * minute),
                ScheduledLiveEvent("ahead", "Spacewalk", now + 30 * minute)
            ),
            nowMillis = now
        )

        assertEquals(listOf("ahead"), alerts.map { it.videoId })
    }

    @Test
    fun alertCountIsCapped() {
        val events = (1..15).map { ScheduledLiveEvent("v$it", "Event $it", now + it * 60 * minute) }

        val alerts = LiveEventAlertPolicy.alertsFor(events, nowMillis = now)

        assertEquals(LiveEventAlertPolicy.MAXIMUM_ALERTS, alerts.size)
        assertEquals("v1", alerts.first().videoId)
    }
}
