package com.restart.spacestationtracker;


import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;

/**
 * Contains various widgets that the user can change to change the behaviour the
 * map activity class.
 */
public class Settings extends MapsActivity implements SeekBar.OnSeekBarChangeListener {

    private static final String TAG = ".Settings";
    private SeekBar seekBar;
    private TextView textView;
    private static int refreshrate;
    private static boolean warning;

    /**
     * Create and assign widgets to ones in the layout
     * @param savedInstanceState on create method
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_settings);
        seekBar = ((SeekBar) findViewById(R.id.seekBar));
        seekBar.setOnSeekBarChangeListener(this);
        textView = ((TextView) findViewById(R.id.textView));
    }

    /**
     * Make sure the seekbar is at the right value and we will warn
     * the user again if they put the value to low
     */
    protected void onResume() {
        super.onResume();
        refreshrate = sharedPref.getInt(getString(R.string.freshsave), 2500);
        seekBar.setProgress(refreshrate);
        warning = true;
        Log.i(TAG, "Settings Restart " + refreshrate);
    }

    /**
     * When the progress of the seekbar is changed we make sure that we format it
     * correctly to only two digits. If we can also warn them, put up a Toast.
     * This is very similar to increasing phone's volume with headphones.
     * "High volume for long periods may damage your hearing."
     * @param seekBar The only seekbar in the layout
     * @param progress An int representing progress of seekbar
     * @param fromUser Is the change from the user?
     */
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

    /**
     * We can't accept a seekbar result of zero. We will, so we will give it a 1 otherwise
     * put what ever it was set to.
     * @param seekBar The seekbar widget that was stopped changing
     */
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
}
