package com.restart.spacestationtracker.services;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.restart.spacestationtracker.Locations;
import com.restart.spacestationtracker.R;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Alert extends Service {
    final int LOCATION_TIME = 1800000; // (30 minutes) minimum time interval between location updates, in milliseconds
    final int LOCATION_DISTANCE = 500; // (1500 meters) minimum distance between location updates, in meters
    final int TIMER_REPEAT = 3540000;  // (59 minutes) Time to repeat a compare between ISS and user's location
    private final int NOTIFICATION_ID = 1234;
    private LocationManager locationManager;
    private Locations locations;
    private Location location;
    private Context context;
    private Date[] dates;
    private Timer timer;


    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        locations = new Locations();
        context = getBaseContext();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    }

    LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    /**
     * Starting a service for ISS location updater.
     *
     * @param intent  N/A
     * @param startId N/A
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return START_STICKY;
        }

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                LOCATION_TIME, LOCATION_DISTANCE, locationListener);
        timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() { // Check if permission are granted
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                // Find user's last known location
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                if (location == null) {
                    return;
                }

                dates = null;

                while (dates == null) { // Loop till we finally get a response.
                    // Get ISSs passes, saving them in an array of dates
                    dates = locations.displaypasses(String.valueOf(location.getLatitude()),
                            String.valueOf(location.getLongitude()), context);

                    // Wait a tiny bit for displaypasses to fully respond or reject.
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // Get user's current date
                Date date = new Date();

                // Compare dates from displaypasses to user's current date
                for (Date date1 : dates) {
                    if (date1 != null) {
                        // Check if they are within an hour
                        boolean withinhour = Math.abs(date.getTime() - date1.getTime()) < 3600000L;
                        if (withinhour) {
                            // push a notification
                            notification(Math.abs(date.getTime() - date1.getTime()));
                            break;
                        }
                    }
                }
            }
        }, 0, TIMER_REPEAT);

        return START_STICKY;
    }

    /**
     * When destroying the service make sure to get ride of any timers and notifications since
     * the user no longer wants them.
     */
    @Override
    public void onDestroy() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
        timer = null;

        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) context.getSystemService(ns);
        nMgr.cancel(NOTIFICATION_ID);
    }

    /**
     * Notification system that is used for this app. All we need to do is call this function
     * when we need to trigger a notification.
     */
    private void notification(long time) {
        int finalseconds = (int) Math.ceil(time / 1000 / 60);
        final String contentText = "ISS is about " + finalseconds + " minutes away!";

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.iss_2011);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setAutoCancel(true)
                        .setOnlyAlertOnce(true)
                        .setOngoing(false)
                        .setContentTitle("ISS Tracker")
                        .setContentText(contentText)
                        .setSmallIcon(R.drawable.iss_2011)
                        .setLargeIcon(icon)
                        .setSound(soundUri)
                        .setWhen(System.currentTimeMillis());

        Intent resultIntent = new Intent(context, Locations.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        stackBuilder.addParentStack(Locations.class);

        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
