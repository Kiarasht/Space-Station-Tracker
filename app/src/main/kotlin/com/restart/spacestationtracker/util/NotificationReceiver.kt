package com.restart.spacestationtracker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.restart.spacestationtracker.shared.passes.CompassLabels
import android.os.Build
import androidx.core.app.NotificationCompat
import com.restart.spacestationtracker.MainActivity
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_PASS_START_TIME = "extra_pass_start_time"
        const val EXTRA_PASS_DURATION = "extra_pass_duration"
        const val EXTRA_PASS_MAGNITUDE = "extra_pass_magnitude"
        const val EXTRA_PASS_MAX_ELEVATION = "extra_pass_max_elevation"
        const val EXTRA_PASS_START_COMPASS = "extra_pass_start_compass"
        const val EXTRA_PASS_END_COMPASS = "extra_pass_end_compass"
        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val passStartTime = intent.getLongExtra(EXTRA_PASS_START_TIME, 0)
        val passDuration = intent.getIntExtra(EXTRA_PASS_DURATION, 0)
        val passMagnitude = intent.getDoubleExtra(EXTRA_PASS_MAGNITUDE, 0.0)

        val passMaxElevation = intent.getDoubleExtra(EXTRA_PASS_MAX_ELEVATION, Double.NaN)
        // Reminders store the English code ("NW"); show it in the reader's language.
        val passStartCompass = CompassLabels.localize(intent.getStringExtra(EXTRA_PASS_START_COMPASS).orEmpty())

        intent.getStringExtra(EXTRA_SCHEDULE_ID)?.let { scheduleId ->
            NotificationScheduler(context).onNotificationDelivered(scheduleId)
        }

        if (passStartTime == 0L) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(context, notificationManager)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_ROUTE, Screen.IssPasses.route)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val dateFormat = SimpleDateFormat("MMMM d, h:mm a", Locale.getDefault())
        val startTimeStr = dateFormat.format(Date(passStartTime))
        val durationInMinutes = TimeUnit.SECONDS.toMinutes(passDuration.toLong())

        val summaryText = context.getString(
            R.string.notification_iss_pass_alert_text_format,
            startTimeStr,
            durationInMinutes,
            context.getString(IssPassVisibility.labelResForMagnitude(passMagnitude))
        )
        val contentText = if (passStartCompass.isNotBlank() && !passMaxElevation.isNaN()) {
            summaryText + "\n" + context.getString(
                R.string.notification_iss_pass_alert_direction_format,
                passStartCompass,
                passMaxElevation.roundToInt()
            )
        } else {
            summaryText
        }

        val notification = NotificationCompat.Builder(context, "iss_pass_channel")
            .setSmallIcon(R.drawable.ic_iss)
            .setContentTitle(context.getString(R.string.notification_iss_pass_alert_title))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(passStartTime.toInt(), notification)
    }

    private fun createNotificationChannel(
        context: Context,
        notificationManager: NotificationManager
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "iss_pass_channel",
                context.getString(R.string.notification_channel_iss_pass_alerts),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_iss_pass_alerts_description)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
