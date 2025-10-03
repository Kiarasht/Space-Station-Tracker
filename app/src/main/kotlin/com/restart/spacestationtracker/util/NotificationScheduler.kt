package com.restart.spacestationtracker.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.restart.spacestationtracker.domain.iss_passes.model.IssPass
import java.util.concurrent.TimeUnit

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNotifications(pass: IssPass, notificationTimes: List<String>) {
        notificationTimes.forEach { time ->
            val triggerTime = calculateTriggerTime(pass.startTime.time, time)
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra(NotificationReceiver.EXTRA_PASS_START_TIME, pass.startTime.time)
                putExtra(NotificationReceiver.EXTRA_PASS_DURATION, pass.durationInSeconds)
                putExtra(NotificationReceiver.EXTRA_PASS_MAGNITUDE, pass.magnitude)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                getRequestCode(pass, time),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
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

    private fun getRequestCode(pass: IssPass, notificationTime: String): Int {
        return (pass.startTime.time.toString() + notificationTime).hashCode()
    }
}