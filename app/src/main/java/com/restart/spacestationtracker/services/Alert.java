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
    private NotificationManager mNotificationManagerupdate;
    private NotificationCompat.Builder mBuilderupdate;
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

        int LOCATION_TIME = 1800000; // (30 minutes) minimum time interval between location updates, in milliseconds
        int LOCATION_DISTANCE = 500; // (1500 meters) minimum distance between location updates, in meters
        int TIMER_REPEAT = 3540000;  // (59 minutes) Time to repeat a compare between ISS and user's location
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

                // Get ISSs passes, saving them in an array of dates
                dates = locations.displaypasses(String.valueOf(location.getLatitude()),
                        String.valueOf(location.getLongitude()), context);

                // Wait a tiny bit for displaypasses to fully respond or reject.
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
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
                            notification();
                            notificationupdate(Math.abs(date.getTime() - date1.getTime()));
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
        nMgr.cancel(1234);
    }

    /**
     * Notification system that is used for this app. All we need to do is call this function
     * when we need to trigger a notification.
     */
    private void notification() {
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.iss_2011);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setAutoCancel(true)
                        .setOnlyAlertOnce(true)
                        .setOngoing(false)
                        .setContentTitle("ISS Tracker")
                        .setContentText("Checking how far ISS is from you...")
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

        mNotificationManager.notify(1234, mBuilder.build());
        mBuilderupdate = mBuilder;
        mNotificationManagerupdate = mNotificationManager;
    }

    /**
     * Updates the manager and finishes with the time difference.
     *
     * @param time The difference between ISS' location to user's location
     */
    private void notificationupdate(long time) {
        int finalseconds = (int) Math.ceil(time / 1000 / 60);
        mBuilderupdate.setContentText("ISS is about " + finalseconds + " minutes away!");
        mNotificationManagerupdate.notify(1234, mBuilderupdate.build());
    }
}
