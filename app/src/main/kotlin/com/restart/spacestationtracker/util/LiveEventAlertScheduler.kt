package com.restart.spacestationtracker.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.restart.spacestationtracker.shared.liveevents.LiveEventAlert

/** Schedules and cancels the alarms that post live-event alerts (see [LiveEventAlertReceiver]). */
class LiveEventAlertScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Schedules each alert and returns their identifiers, for cancelling them later. */
    fun schedule(alerts: List<LiveEventAlert>): Set<String> {
        alerts.forEach { alert -> scheduleAlarm(alert.triggerTimeMillis, pendingIntent(alert)) }
        return alerts.map { it.identifier }.toSet()
    }

    fun cancel(identifiers: Set<String>) {
        identifiers.forEach { identifier ->
            val intent = Intent(context, LiveEventAlertReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                identifier.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: return@forEach
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun pendingIntent(alert: LiveEventAlert): PendingIntent {
        val intent = Intent(context, LiveEventAlertReceiver::class.java).apply {
            putExtra(LiveEventAlertReceiver.EXTRA_VIDEO_ID, alert.videoId)
            putExtra(LiveEventAlertReceiver.EXTRA_TITLE, alert.title)
            putExtra(LiveEventAlertReceiver.EXTRA_START_TIME, alert.scheduledStartMillis)
        }
        return PendingIntent.getBroadcast(
            context,
            alert.identifier.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleAlarm(triggerTimeMillis: Long, pendingIntent: PendingIntent) {
        val canScheduleExactly = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (canScheduleExactly) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                return
            } catch (_: SecurityException) {
                // Permission can be revoked between the capability check and scheduling.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
    }
}
