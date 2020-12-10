package com.restart.spacestationtracker.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.preference.PreferenceManager;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.restart.spacestationtracker.Locations;
import com.restart.spacestationtracker.MapsActivity;
import com.restart.spacestationtracker.Preferences;
import com.restart.spacestationtracker.R;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The type Alert. Pushes a notification to the phone if ISS is flying expected to fly near by.
 */
public class Alert extends Service {

    private static final int NOTIFICATION_ID = 1234;

    private SharedPreferences sharedPreferences;
    private LocationManager locationManager;
    private Locations locations;
    private Location mLocation;
    private Context context;
    private List<Date> dates;
    private Date accept;
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
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            mLocation = location;
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    /**
     * Starting a service for ISS Location updater.
     *
     * @param intent  N/A
     * @param startId N/A
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return START_STICKY;
        }

        Intent clickIntent = new Intent(this, Preferences.class);
        clickIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, clickIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, MapsActivity.CHANNEL_ID)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setContentTitle("ISS Notification Service")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.iss_2011)
                .build();

        startForeground(1234, notification);

        final int locationTime = 1800000; // (30 minutes) minimum time interval between mLocation updates, in milliseconds
        final int locationDistance = 500; // (1500 meters) minimum distance between mLocation updates, in meters
        final int timerRepeat = 3540000;  // (59 minutes) Time to repeat a compare between ISS and user's mLocation
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                locationTime, locationDistance, locationListener);
        timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() { // Check if permission are granted
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                // Find user's last known mLocation
                mLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                if (mLocation == null) {
                    return;
                }

                dates = null;

                for (int i = 0; i < 30 && sharedPreferences.getBoolean("notification_ISS", false)
                        && (dates == null || (dates.size() > 0 && dates.get(0) == null)); ++i) {
                    // Get ISSs passes, saving them in an array of dates
                    dates = locations.displayPasses(String.valueOf(mLocation.getLatitude()),
                            String.valueOf(mLocation.getLongitude()), context);
                    // Wait a tiny bit for displayPasses to fully respond or reject. Also gives
                    // api service a breathing room before calling again if it failed.
                    // Try 30 times for 10 minutes.
                    try {
                        Thread.sleep(20000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // Get user's current date
                Date date = new Date();

                long last = sharedPreferences.getLong("time", 0);

                if (last == 0) {
                    accept = new Date(System.currentTimeMillis() - 3600 * 1000);
                } else {
                    accept = new Date(last);
                }

                // Compare dates from displayPasses to user's current date
                for (Date date1 : dates) {
                    if (date1 != null) {
                        boolean withinHour = Math.abs(date.getTime() - date1.getTime()) < 3600000L; // 1 hour
                        boolean duplicate = Math.abs(accept.getTime() - date.getTime()) < 3540000L; // 59 minutes
                        if (withinHour && !duplicate) {
                            sharedPreferences.edit().putLong("time", date.getTime()).apply();
                            notification(Math.abs(date.getTime() - date1.getTime()));
                            break;
                        }
                    }
                }
            }
        }, 0, timerRepeat);

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

        NotificationManager nMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nMgr != null)
            nMgr.cancel(NOTIFICATION_ID);
    }

    /**
     * Notification system that is used for this app. All we need to do is call this function
     * when we need to trigger a notification.
     */
    private void notification(long time) {
        final int finalSeconds = (int) Math.ceil(time / 1000 / 60);
        final String contentText;

        if (finalSeconds > 0) {
            contentText = "ISS is about " + finalSeconds + " minutes away!";
        } else {
            contentText = "ISS is right above you!";
        }

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.iss_2011);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, MapsActivity.CHANNEL_ID)
                        .setAutoCancel(true)
                        .setOnlyAlertOnce(true)
                        .setOngoing(false)
                        .setContentTitle(context.getString(R.string.app_name))
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

        if (mNotificationManager != null)
            mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
