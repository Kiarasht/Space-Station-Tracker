package com.restart.spacestationtracker;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;

public class Settings extends Activity implements SeekBar.OnSeekBarChangeListener {

    private String TAG = "com.restart.spacestationtracker";
    private SeekBar seekBar;
    private static int refreshrate;
    private static boolean warning;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_settings);
        seekBar = ((SeekBar) findViewById(R.id.seekBar));
        seekBar.setOnSeekBarChangeListener(this);
    }

    protected void onResume() {
        super.onResume();
        refreshrate = MapsActivity.getRefreshrate();
        seekBar.setProgress(refreshrate);
        warning = true;
        Log.i(TAG, "Settings Restart " + refreshrate);
    }

    static public int getRefreshrate() {
        return refreshrate;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (progress < 1000 && warning) {
            Toast.makeText(getApplicationContext(), "Reducing the refresh rate doesn't guarantee" +
                    " a smoother result since your phone may not be able to handle it."
                    , Toast.LENGTH_LONG).show();
            warning = false;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (seekBar.getProgress() == 0) {
            refreshrate = 1;
        } else {
            refreshrate = seekBar.getProgress();
        }
    }
}
