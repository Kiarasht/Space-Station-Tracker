package com.restart.spacestationtracker.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.restart.spacestationtracker.domain.iss_passes.model.IssPass
import com.restart.spacestationtracker.shared.passes.PassAlertPolicy
import org.json.JSONException
import org.json.JSONObject

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val manualAlertStore =
        context.getSharedPreferences(MANUAL_ALERTS_PREFS_NAME, Context.MODE_PRIVATE)

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

    /**
     * Schedules the manual reminders that are still in the future and persists them so they can
     * be restored after a reboot or app update. Returns how many reminders were scheduled.
     */
    fun scheduleNotifications(pass: IssPass, notificationTimes: List<String>): Int {
        val reminders = PassAlertPolicy.remindersFor(
            startTimeMillis = pass.startTimeMillis,
            notificationTimes = notificationTimes,
            nowMillis = System.currentTimeMillis()
        )
        if (reminders.isEmpty()) return 0

        val editor = manualAlertStore.edit()
        reminders.forEach { reminder ->
            val scheduleId = getScheduleId(pass, reminder.notificationTime, MANUAL_PREFIX)
            scheduleAlarm(reminder.triggerTimeMillis, buildPendingIntent(pass, scheduleId))
            editor.putString(scheduleId, encodeManualAlert(pass, reminder.triggerTimeMillis))
        }
        editor.apply()
        return reminders.size
    }

    /** Re-registers persisted manual reminders and drops the ones whose time has passed. */
    fun rescheduleManualNotifications() {
        val nowMillis = System.currentTimeMillis()
        val editor = manualAlertStore.edit()
        manualAlertStore.all.forEach { (scheduleId, value) ->
            val alert = (value as? String)?.let(::decodeManualAlert)
            if (alert == null || alert.triggerTimeMillis <= nowMillis) {
                editor.remove(scheduleId)
            } else {
                scheduleAlarm(alert.triggerTimeMillis, buildPendingIntent(alert.pass, scheduleId))
            }
        }
        editor.apply()
    }

    fun onNotificationDelivered(scheduleId: String) {
        if (scheduleId.startsWith("$MANUAL_PREFIX:")) {
            manualAlertStore.edit().remove(scheduleId).apply()
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
            putExtra(NotificationReceiver.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(NotificationReceiver.EXTRA_PASS_START_TIME, pass.startTimeMillis)
            putExtra(NotificationReceiver.EXTRA_PASS_DURATION, pass.durationInSeconds)
            putExtra(NotificationReceiver.EXTRA_PASS_MAGNITUDE, pass.magnitude)
            putExtra(NotificationReceiver.EXTRA_PASS_MAX_ELEVATION, pass.maxElevation)
            putExtra(NotificationReceiver.EXTRA_PASS_START_COMPASS, pass.startAzimuthCompass)
            putExtra(NotificationReceiver.EXTRA_PASS_END_COMPASS, pass.endAzimuthCompass)
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

    private fun encodeManualAlert(pass: IssPass, triggerTimeMillis: Long): String {
        return JSONObject()
            .put(KEY_TRIGGER_TIME, triggerTimeMillis)
            .put(KEY_START_TIME, pass.startTimeMillis)
            .put(KEY_DURATION, pass.durationInSeconds)
            .put(KEY_MAGNITUDE, pass.magnitude)
            .put(KEY_MAX_ELEVATION, pass.maxElevation)
            .put(KEY_START_COMPASS, pass.startAzimuthCompass)
            .put(KEY_END_COMPASS, pass.endAzimuthCompass)
            .toString()
    }

    private fun decodeManualAlert(value: String): ManualAlert? {
        return try {
            val json = JSONObject(value)
            ManualAlert(
                triggerTimeMillis = json.getLong(KEY_TRIGGER_TIME),
                pass = IssPass(
                    startTimeMillis = json.getLong(KEY_START_TIME),
                    durationInSeconds = json.getInt(KEY_DURATION),
                    magnitude = json.getDouble(KEY_MAGNITUDE),
                    maxElevation = json.optDouble(KEY_MAX_ELEVATION, 0.0),
                    startAzimuthCompass = json.optString(KEY_START_COMPASS),
                    endAzimuthCompass = json.optString(KEY_END_COMPASS)
                )
            )
        } catch (_: JSONException) {
            null
        }
    }

    private data class ManualAlert(val triggerTimeMillis: Long, val pass: IssPass)

    private companion object {
        const val AUTOMATIC_PREFIX = "automatic"
        const val MANUAL_PREFIX = "manual"
        const val MANUAL_ALERTS_PREFS_NAME = "manual_pass_alerts"

        const val KEY_TRIGGER_TIME = "trigger_time"
        const val KEY_START_TIME = "start_time"
        const val KEY_DURATION = "duration"
        const val KEY_MAGNITUDE = "magnitude"
        const val KEY_MAX_ELEVATION = "max_elevation"
        const val KEY_START_COMPASS = "start_compass"
        const val KEY_END_COMPASS = "end_compass"
    }
}
