package com.restart.spacestationtracker;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.text.DecimalFormat;

/**
 * Contains various widgets that the user can change to change the behaviour the
 * map activity class.
 */
public class Settings extends MapsActivity implements SeekBar.OnSeekBarChangeListener {

    private static final String TAG = ".Settings";
    private SeekBar seekBar;
    private TextView textView;
    private CheckBox checkBox;
    private int refreshrate;

    /**
     * Create and assign widgets to ones in the layout
     *
     * @param savedInstanceState on create method
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_settings);

        seekBar = ((SeekBar) findViewById(R.id.seekBar));
        seekBar.setOnSeekBarChangeListener(this);
        textView = ((TextView) findViewById(R.id.textView));
        checkBox = ((CheckBox) findViewById(R.id.checkBox));

        AdView adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    /**
     * Make sure the seekbar is at the right value and we will warn
     * the user again if they put the value to low
     */
    protected void onResume() {
        super.onResume();
        refreshrate = sharedPref.getInt(getString(R.string.freshsave), 2500);
        Boolean notification = sharedPref.getBoolean(getString(R.string.notificationcheck), true);
        checkBox.setChecked(notification);
        if (refreshrate == 1000) {
            String result = "Refresh Rate (1.00 sec/refresh)";
            textView.setText(result);
        }
        seekBar.setProgress(refreshrate - 1000);
        Log.i(TAG, "Settings Restart " + refreshrate);
    }

    /**
     * When the progress of the seekbar is changed we make sure that we format it
     * correctly to only two digits. If we can also warn them, put up a Toast.
     * This is very similar to increasing phone's volume with headphones.
     * "High volume for long periods may damage your hearing."
     *
     * @param seekBar  The only seekbar in the layout
     * @param progress An int representing progress of seekbar
     * @param fromUser Is the change from the user?
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        DecimalFormat df = new DecimalFormat("0.00");
        String calculate = df.format((float) (progress + 1000) / 1000);

        String result = "Refresh Rate (" + calculate + " sec/refresh)";
        textView.setText(result);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    /**
     * We can't accept a seekbar result of zero. We will, so we will give it a 1 otherwise
     * put what ever it was set to.
     *
     * @param seekBar The seekbar widget that was stopped changing
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        editor = sharedPref.edit();
        refreshrate = seekBar.getProgress() + 1000;
        editor.putInt(getString(R.string.freshsave), refreshrate);
        editor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    /**
     * Onclick method for the check box. Either starts or stops an android service.
     *
     * @param view A view of the check box
     */
    public void onCheckboxClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        sharedPref.edit().putBoolean(getString(R.string.notificationcheck), checked).apply();

        if (checked) {
            Toast.makeText(this, "I'll notify you when ISS is close by :)", Toast.LENGTH_SHORT).show();
            startService(new Intent(this, Alert.class));
        } else {
            Toast.makeText(this, "I'll make sure not to annoy you anymore :(", Toast.LENGTH_SHORT).show();
            stopService(new Intent(this, Alert.class));
        }
    }
}
