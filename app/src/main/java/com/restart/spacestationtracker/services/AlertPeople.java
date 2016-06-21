package com.restart.spacestationtracker.services;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.support.v4.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.restart.spacestationtracker.PeopleinSpace;
import com.restart.spacestationtracker.R;

import java.util.Timer;
import java.util.TimerTask;

public class AlertPeople extends Service {
    private SharedPreferences sharedPref;
    private PeopleinSpace peopleinSpace;
    private Context context;
    private Timer timer;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        peopleinSpace = new PeopleinSpace();
        sharedPref = getSharedPreferences("savefile", MODE_PRIVATE);
        context = getBaseContext();
    }

    /**
     * Start the finding astronauts service. It checks for updates everyday.
     * @param intent N/A
     * @param startId N/A
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        timer = new Timer();
        int TIMER_REPEAT = 86400000; // 1 day
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String astro_detail = peopleinSpace.display_people(true, context);
                String astro_onfile = sharedPref.getString(getString(R.string.astro_detail), "");

                if (!astro_detail.equals(astro_onfile)) {
                    sharedPref.edit().putString(getString(R.string.astro_detail), astro_detail).apply();
                    notification();
                }
            }
        }, 0, TIMER_REPEAT);

        return START_STICKY;
    }

    /**
     * Destroy all timers and objects if the service ever gets destroyed.
     */
    @Override
    public void onDestroy() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            String ns = Context.NOTIFICATION_SERVICE;
            NotificationManager nMgr = (NotificationManager) context.getSystemService(ns);
            nMgr.cancel(4321);
        }
        timer = null;
    }

    /**
     * Notification system that is used for this app. All we need to do is call this function
     * when we need to trigger a notification.
     */
    private void notification() {
        Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.iss_2011);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setAutoCancel(true)
                        .setContentTitle("ISS Tracker")
                        .setContentText("People in space changed!")
                        .setSmallIcon(R.drawable.iss_2011)
                        .setLargeIcon(icon);

        Intent resultIntent = new Intent(context, PeopleinSpace.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        stackBuilder.addParentStack(PeopleinSpace.class);

        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int mId = 4321;
        mNotificationManager.notify(mId, mBuilder.build());
    }
}
