package com.restart.spacestationtracker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.shared.liveevents.LiveEventAlertPolicy
import java.text.DateFormat
import java.util.Date

/** Posts a live-event alert; tapping it opens the broadcast on YouTube. */
class LiveEventAlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val videoId = intent.getStringExtra(EXTRA_VIDEO_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val startTimeMillis = intent.getLongExtra(EXTRA_START_TIME, 0L)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_live_events),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_live_events_description)
                }
            )
        }

        val watchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(LiveEventAlertPolicy.watchUrl(videoId)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val contentIntent = PendingIntent.getActivity(
            context,
            videoId.hashCode(),
            watchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val startTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(startTimeMillis))
        val text = context.getString(R.string.notification_live_event_text_format, title, startTime)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_iss)
            .setContentTitle(context.getString(R.string.notification_live_event_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(videoId.hashCode(), notification)
    }

    companion object {
        const val EXTRA_VIDEO_ID = "extra_live_event_video_id"
        const val EXTRA_TITLE = "extra_live_event_title"
        const val EXTRA_START_TIME = "extra_live_event_start_time"
        private const val CHANNEL_ID = "live_event_channel"
    }
}
