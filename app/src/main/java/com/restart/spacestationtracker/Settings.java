package com.restart.spacestationtracker;


import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;

public class Settings extends MapsActivity implements SeekBar.OnSeekBarChangeListener {

    private static final String TAG = ".Settings";
    private SeekBar seekBar;
    private TextView textView;
    private static int refreshrate;
    private static boolean warning;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_settings);
        seekBar = ((SeekBar) findViewById(R.id.seekBar));
        seekBar.setOnSeekBarChangeListener(this);
        textView = ((TextView) findViewById(R.id.textView));
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
        DecimalFormat df = new DecimalFormat("0.00");
        String calculate = df.format((float) progress / 1000);

        if (calculate.equals("0.00")) {
            calculate = "0.01";
        }

        String result = "Refresh Rate (" + calculate + " sec/refresh)";
        textView.setText(result);
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
