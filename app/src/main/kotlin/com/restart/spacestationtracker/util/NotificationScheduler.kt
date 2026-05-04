package com.restart.spacestationtracker.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.restart.spacestationtracker.domain.iss_passes.model.IssPass
import java.util.concurrent.TimeUnit

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAutomaticNotifications(pass: IssPass, notificationTimes: Set<String>): Set<String> {
        val scheduledIds = mutableSetOf<String>()
        notificationTimes.forEach { time ->
            val triggerTime = calculateTriggerTime(pass.startTime.time, time)
            if (triggerTime <= System.currentTimeMillis()) {
                return@forEach
            }

            val scheduleId = getScheduleId(pass, time, AUTOMATIC_PREFIX)
            val pendingIntent = buildPendingIntent(pass, scheduleId)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            scheduledIds.add(scheduleId)
        }
        return scheduledIds
    }

    fun scheduleNotifications(pass: IssPass, notificationTimes: List<String>) {
        notificationTimes.forEach { time ->
            val triggerTime = calculateTriggerTime(pass.startTime.time, time)
            val pendingIntent = buildPendingIntent(pass, getScheduleId(pass, time, MANUAL_PREFIX))
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
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
            putExtra(NotificationReceiver.EXTRA_PASS_START_TIME, pass.startTime.time)
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

    private fun calculateTriggerTime(startTime: Long, notificationTime: String): Long {
        return when (notificationTime) {
            "At time of event" -> startTime
            "10 minutes before" -> startTime - TimeUnit.MINUTES.toMillis(10)
            "1 hour before" -> startTime - TimeUnit.HOURS.toMillis(1)
            "12 hours before" -> startTime - TimeUnit.HOURS.toMillis(12)
            "1 day before" -> startTime - TimeUnit.DAYS.toMillis(1)
            "1 week before" -> startTime - TimeUnit.DAYS.toMillis(7)
            else -> startTime
        }
    }

    private fun getScheduleId(pass: IssPass, notificationTime: String, prefix: String): String {
        return "$prefix:${pass.startTime.time}:$notificationTime"
    }

    private companion object {
        const val AUTOMATIC_PREFIX = "automatic"
        const val MANUAL_PREFIX = "manual"
    }
}
