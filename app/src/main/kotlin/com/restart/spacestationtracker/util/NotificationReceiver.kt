package com.restart.spacestationtracker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.ui.iss_passes.getBrightnessRating
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_PASS_START_TIME = "extra_pass_start_time"
        const val EXTRA_PASS_DURATION = "extra_pass_duration"
        const val EXTRA_PASS_MAGNITUDE = "extra_pass_magnitude"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val passStartTime = intent.getLongExtra(EXTRA_PASS_START_TIME, 0)
        val passDuration = intent.getIntExtra(EXTRA_PASS_DURATION, 0)
        val passMagnitude = intent.getDoubleExtra(EXTRA_PASS_MAGNITUDE, 0.0)

        if (passStartTime == 0L) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val dateFormat = SimpleDateFormat("MMMM d, h:mm a", Locale.getDefault())
        val startTimeStr = dateFormat.format(Date(passStartTime))
        val durationInMinutes = TimeUnit.SECONDS.toMinutes(passDuration.toLong())

        val notification = NotificationCompat.Builder(context, "iss_pass_channel")
            .setSmallIcon(R.drawable.ic_iss)
            .setContentTitle("ISS Pass Alert")
            .setContentText("A pass is starting at $startTimeStr for $durationInMinutes min. Visibility is ${getBrightnessRating(passMagnitude)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(passStartTime.toInt(), notification)
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "iss_pass_channel",
                "ISS Pass Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming ISS passes"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}