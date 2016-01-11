package com.restart.spacestationtracker;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
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
import android.util.Log;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Alert extends Service {
    private static final String TAG = ".Alert";
    private LocationManager locationManager;
    private Context context;
    private Locations locations;
    private Location location;
    private Date[] dates;
    private Timer timer;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        locations = new Locations();
        context = getApplicationContext();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    }

    LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            Log.d(TAG, "Location changed: " + location.getLongitude() + " " + location.getLatitude());
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
        Log.d(TAG, "onStart");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
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
                Log.d(TAG, "Location run: " + location.getLongitude() + " " + location.getLatitude());
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Date date = new Date();

                for (Date date1 : dates) {
                    if (date1 != null) {
                        boolean withinhour = Math.abs(date.getTime() - date1.getTime()) < 3600000L;
                        Log.d(TAG, "Date Only: " + date);
                        Log.d(TAG, "Dates: " + date1);
                        Log.d(TAG, "Boolean: " + withinhour);
                        if (withinhour) {
                            notification();
                            break;
                        }
                    }
                }
            }
        }, 0, 3600000);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        timer.cancel();
        timer.purge();
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
                        .setContentText("ISS is an hour away from your location!")
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

        int mId = 1234;
        mNotificationManager.notify(mId, mBuilder.build());
    }
}
