package com.restart.spacestationtracker;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.restart.spacestationtracker.services.Alert;
import com.restart.spacestationtracker.services.AlertPeople;

import java.text.DecimalFormat;

/**
 * Contains various widgets that the user can change to change the behaviour the
 * map activity class.
 */
public class Settings extends MapsActivity implements SeekBar.OnSeekBarChangeListener {

    private static final String TAG = ".Settings";
    private CheckBox checkBox3;
    private CheckBox checkBox2;
    private TextView textView;
    private CheckBox checkBox;
    private SeekBar seekBar;
    private Context context;
    private int refreshrate;
    private boolean reference;
    private AdView adView;

    /**
     * Create and assign widgets to ones in the layout
     *
     * @param savedInstanceState on create method
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_settings);

        adView = null;
        reference = false;
        context = getApplicationContext();
        seekBar = ((SeekBar) findViewById(R.id.seekBar));
        seekBar.setOnSeekBarChangeListener(this);
        textView = ((TextView) findViewById(R.id.textView));
        checkBox = ((CheckBox) findViewById(R.id.checkBox));
        checkBox2 = ((CheckBox) findViewById(R.id.checkBox2));
        checkBox3 = ((CheckBox) findViewById(R.id.checkBox3));

        if (!sharedPref.getBoolean(getString(R.string.notificationcheck3), false)) {
            adView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
    }

    /**
     * Make sure the seekbar is at the right value and we will warn
     * the user again if they put the value to low
     */
    protected void onResume() {
        super.onResume();
        refreshrate = sharedPref.getInt(getString(R.string.freshsave), 15000);
        Boolean notification = sharedPref.getBoolean(getString(R.string.notificationcheck), false);
        checkBox.setChecked(notification);
        notification = sharedPref.getBoolean(getString(R.string.notificationcheck2), false);
        checkBox2.setChecked(notification);
        notification = sharedPref.getBoolean(getString(R.string.notificationcheck3), false);
        checkBox3.setChecked(notification);

        if (refreshrate == 1000) {
            String result = "Refresh Rate (1.00 sec/refresh)";
            textView.setText(result);
        }
        seekBar.setProgress(refreshrate - 1000);
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
        int progress = seekBar.getProgress() + 1000;
        if (progress < 5000 && !reference) {
            Toast.makeText(context, "A Low refresh rate could lead to performance issues!",
                    Toast.LENGTH_SHORT).show();
            reference = true;
        }
        editor = sharedPref.edit();
        refreshrate = progress;
        editor.putInt(getString(R.string.freshsave), refreshrate);
        editor.apply();
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
            Toast.makeText(this, "Notify when ISS is close by", Toast.LENGTH_SHORT).show();
            startService(new Intent(this, Alert.class));
        } else {
            Toast.makeText(this, "Stop notify when ISS is close by", Toast.LENGTH_SHORT).show();
            stopService(new Intent(this, Alert.class));
        }
    }

    /**
     * Onclick method for the check box. Either starts or stops an android service.
     *
     * @param view A view of the check box
     */
    public void onCheckboxClicked2(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        sharedPref.edit().putBoolean(getString(R.string.notificationcheck2), checked).apply();

        if (checked) {
            Toast.makeText(this, "Notify when people in space change", Toast.LENGTH_SHORT).show();
            startService(new Intent(this, AlertPeople.class));
        } else {
            Toast.makeText(this, "Stop notify when people in space change", Toast.LENGTH_SHORT).show();
            stopService(new Intent(this, AlertPeople.class));
        }
    }

    /**
     * Onclick method for the check box. Either allows or stops ads.
     *
     * @param view A view of the check box
     */
    public void onCheckboxClicked3(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        sharedPref.edit().putBoolean(getString(R.string.notificationcheck3), checked).apply();

        if (checked) {
            Toast.makeText(this, "Ads disabled. Consider enabling them when non-intrusive", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Ads enabled", Toast.LENGTH_SHORT).show();
        }

        if (sharedPref.getBoolean(getString(R.string.notificationcheck3), false) && adView != null) {
            adView.setVisibility(View.INVISIBLE);
        } else if (!sharedPref.getBoolean(getString(R.string.notificationcheck3), false)) {
            if (adView == null) {
                adView = (AdView) findViewById(R.id.adView);
                AdRequest adRequest = new AdRequest.Builder().build();
                adView.loadAd(adRequest);
            } else {
                adView.setVisibility(View.VISIBLE);
            }
        }
    }
}
