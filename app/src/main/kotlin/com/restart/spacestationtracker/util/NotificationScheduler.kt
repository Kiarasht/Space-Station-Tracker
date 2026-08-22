package com.restart.spacestationtracker.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.restart.spacestationtracker.domain.iss_passes.model.IssPass
import com.restart.spacestationtracker.shared.passes.PassAlertPolicy

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAutomaticNotifications(pass: IssPass, notificationTimes: Set<String>): Set<String> {
        val scheduledIds = mutableSetOf<String>()
        notificationTimes.forEach { time ->
            val reminder = PassAlertPolicy.remindersFor(
                startTimeMillis = pass.startTimeMillis,
                notificationTimes = listOf(time),
                nowMillis = System.currentTimeMillis()
            ).firstOrNull() ?: return@forEach

            val scheduleId = getScheduleId(pass, time, AUTOMATIC_PREFIX)
            val pendingIntent = buildPendingIntent(pass, scheduleId)
            scheduleAlarm(reminder.triggerTimeMillis, pendingIntent)
            scheduledIds.add(scheduleId)
        }
        return scheduledIds
    }

    fun scheduleNotifications(pass: IssPass, notificationTimes: List<String>) {
        PassAlertPolicy.remindersFor(
            startTimeMillis = pass.startTimeMillis,
            notificationTimes = notificationTimes,
            nowMillis = System.currentTimeMillis()
        ).forEach { reminder ->
            val pendingIntent = buildPendingIntent(
                pass,
                getScheduleId(pass, reminder.notificationTime, MANUAL_PREFIX)
            )
            scheduleAlarm(reminder.triggerTimeMillis, pendingIntent)
        }
    }

    fun cancelAutomaticNotifications(scheduleIds: Set<String>) {
        scheduleIds.forEach { scheduleId ->
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                scheduleId.hashCode(),
                Intent(context, NotificationReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }

    private fun buildPendingIntent(pass: IssPass, scheduleId: String): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_PASS_START_TIME, pass.startTimeMillis)
            putExtra(NotificationReceiver.EXTRA_PASS_DURATION, pass.durationInSeconds)
            putExtra(NotificationReceiver.EXTRA_PASS_MAGNITUDE, pass.magnitude)
        }
        return PendingIntent.getBroadcast(
            context,
            scheduleId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleAlarm(triggerTimeMillis: Long, pendingIntent: PendingIntent) {
        val canScheduleExactly = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        if (canScheduleExactly) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
                return
            } catch (_: SecurityException) {
                // Permission can be revoked between the capability check and scheduling.
            }
        }

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTimeMillis,
            pendingIntent
        )
    }

    private fun getScheduleId(pass: IssPass, notificationTime: String, prefix: String): String {
        return "$prefix:${pass.startTimeMillis}:$notificationTime"
    }

    private companion object {
        const val AUTOMATIC_PREFIX = "automatic"
        const val MANUAL_PREFIX = "manual"
    }
}
