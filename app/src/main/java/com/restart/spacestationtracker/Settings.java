package com.restart.spacestationtracker;


import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;

public class Settings extends MapsActivity implements SeekBar.OnSeekBarChangeListener {

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
        refreshrate = sharedPref.getInt(getString(R.string.freshsave), 2500);
        seekBar.setProgress(refreshrate);
        warning = true;
        Log.i(TAG, "Settings Restart " + refreshrate);
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
        sharedPref = getPreferences(Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        if (seekBar.getProgress() == 0) {
            refreshrate = 1;
            editor.putInt(getString(R.string.freshsave), 1);
        } else {
            refreshrate = seekBar.getProgress();
            editor.putInt(getString(R.string.freshsave), refreshrate);
        }
        editor.apply();
    }
}
