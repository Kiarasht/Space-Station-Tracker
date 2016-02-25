package com.restart.spacestationtracker;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Alert extends Service {
    private static final String TAG = ".Alert";
    private static final int LOCATION_TIME = 900000; // 15 minutes
    private static final int LOCATION_DISTANCE = 500; // 1500 meters
    private static final int TIMER_REPEAT = 850000; // 14 minutes
    private NotificationManager mNotificationManagerupdate;
    private NotificationCompat.Builder mBuilderupdate;
    private LocationManager locationManager;
    private SharedPreferences sharedPref;
    private Boolean continuesupdate;
    private Locations locations;
    private Location location;
    private Timer timerupdate;
    private Context context;
    private Date[] dates;
    private Timer timer;
    private int loop;


    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        loop = 0;
        locations = new Locations();
        context = getApplicationContext();
        sharedPref = getSharedPreferences("savefile", MODE_PRIVATE);
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

    @Override
    public void onStart(Intent intent, int startid) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                LOCATION_TIME, LOCATION_DISTANCE, locationListener);
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                dates = locations.displaypasses(String.valueOf(location.getLatitude()),
                        String.valueOf(location.getLongitude()));
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Date date = new Date();

                for (Date date1 : dates) {
                    if (date1 != null) {
                        boolean withinhour = Math.abs(date.getTime() - date1.getTime()) < 3600000L;
                        if (withinhour) {
                            continuesupdate = sharedPref.getBoolean(getString(R.string.notificationcheck), false);
                            notification();
                            if (continuesupdate) {
                                updatemanager(Math.abs(date.getTime() - date1.getTime()));
                            }
                            break;
                        }
                    }
                }
            }
        }, 0, TIMER_REPEAT);
    }

    @Override
    public void onDestroy() {
        loop = 0;
        timer.cancel();
        timer.purge();
        timer = null;
        continuesupdate = sharedPref.getBoolean(getString(R.string.notificationcheck), false);
        if (continuesupdate) {
            timerupdate.cancel();
            timerupdate.purge();
            timerupdate = null;
        }
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
                        .setOnlyAlertOnce(true)
                        .setContentTitle("ISS Tracker")
                        .setContentText("ISS is about an hour away!")
                        .setSmallIcon(R.drawable.iss_2011)
                        .setLargeIcon(icon);

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

    private void updatemanager(final long time) {
        timerupdate = new Timer();
        timerupdate.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                notificationupdate(time);
            }
        }, 0, 1000);
    }

    private  void notificationupdate(long time) {
        long finalseconds = (time / 100) - loop++;
        mBuilderupdate.setContentText("Iss is " + finalseconds + " seconds away!");
        mNotificationManagerupdate.notify(1234, mBuilderupdate.build());
    }
}
