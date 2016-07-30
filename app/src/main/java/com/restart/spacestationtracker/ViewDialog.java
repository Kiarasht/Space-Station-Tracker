package com.restart.spacestationtracker;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceScreen;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class ViewDialog {

    private PreferenceScreen preferenceScreen;
    private SharedPreferences sharedPref;
    private Context applicationContext;
    private Activity mapsActivity;
    private Activity activity;
    private Context context;
    private String message;

    public ViewDialog(Context context, String message, SharedPreferences sharedPref, MapsActivity mapsActivity) {
        this.context = context;
        this.message = message;
        this.sharedPref = sharedPref;
        this.mapsActivity = mapsActivity;
    }

    public ViewDialog(Context context, String message, Context applicationContext, Activity activity,
                      PreferenceScreen preferenceScreen, SharedPreferences sharedPref) {
        this.context = context;
        this.message = message;
        this.applicationContext = applicationContext;
        this.activity = activity;
        this.preferenceScreen = preferenceScreen;
        this.sharedPref = sharedPref;
    }

    public void showDialog() {
        final Dialog dialog;

        if (context == null) {
            dialog = new Dialog(activity);
        } else {
            dialog = new Dialog(context);
        }

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.layout_dialog);
        dialog.setCancelable(false);

        TextView text = (TextView) dialog.findViewById(R.id.text_dialog);
        text.setText(message);

        Button dialogOK = (Button) dialog.findViewById(R.id.btn_dialog);
        Button dialogCancel = (Button) dialog.findViewById(R.id.btn_cancel);

        if (context == null) {
            dialogOK.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sharedPref.edit().putBoolean(applicationContext.getString(R.string.askPermission), false).apply();
                    ActivityCompat.requestPermissions(activity, new String[]{
                            android.Manifest.permission.INTERNET,
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                    CheckBoxPreference iss_Tracker = (CheckBoxPreference) preferenceScreen.findPreference("notification_ISS");
                    iss_Tracker.setChecked(false);
                    dialog.dismiss();
                }
            });

            dialogCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CheckBoxPreference iss_Tracker = (CheckBoxPreference) preferenceScreen.findPreference("notification_ISS");
                    iss_Tracker.setChecked(false);
                    dialog.dismiss();
                }
            });

        } else {
            dialogOK.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isLocationPermissionGranted();
                    dialog.dismiss();
                }
            });

            dialogCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }
        dialog.show();

    }

    public boolean isLocationPermissionGranted() {
        sharedPref.edit().putBoolean(context.getString(R.string.askPermission), false).apply();
        ActivityCompat.requestPermissions(mapsActivity, new String[]{
                android.Manifest.permission.INTERNET,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        return false;
    }
}

