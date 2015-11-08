package com.restart.spacestationtracker;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;

public class Settings extends Activity implements SeekBar.OnSeekBarChangeListener {

    private String TAG = "com.restart.spacestationtracker";
    private SeekBar seekBar;
    private static int refreshrate;
    private boolean start = false;

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
        Log.i(TAG, "Settings Restart " + refreshrate);
    }

    static public int getRefreshrate() {
        return refreshrate;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        refreshrate = seekBar.getProgress();
    }
}
