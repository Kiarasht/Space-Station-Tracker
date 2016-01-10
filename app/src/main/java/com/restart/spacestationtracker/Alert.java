package com.restart.spacestationtracker;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class Alert extends Service {
    private static final String TAG = ".Alert";
    private Timer timer;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
    }

    @Override
    public void onStart(Intent intent, int startid) {
        Log.d(TAG, "onStart");
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "Test");
            }
        }, 3600000, 3600000);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        timer.cancel();
        timer.purge();
        timer = null;
    }
}
