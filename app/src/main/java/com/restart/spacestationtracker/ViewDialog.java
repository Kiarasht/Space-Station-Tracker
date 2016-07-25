package com.restart.spacestationtracker;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class ViewDialog {
    private Context context;
    private Context applicationContext;
    private String message;
    private Preference preference;
    private Activity activity;
    private PreferenceScreen preferenceScreen;
    private SharedPreferences sharedPref;

    public ViewDialog(Context context, String message, SharedPreferences sharedPref) {
        this.context = context;
        this.message = message;
        this.sharedPref = sharedPref;
    }

    public ViewDialog(Context context, String message, Context applicationContext, Preference preference, Activity activity, PreferenceScreen preferenceScreen, SharedPreferences sharedPref) {
        this.context = context;
        this.message = message;
        this.applicationContext = applicationContext;
        this.preference = preference;
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
                    // Starting Flyby service? Check location permission
                    if (Build.VERSION.SDK_INT >= 23) {
                        if (applicationContext.checkSelfPermission(android.Manifest.permission.INTERNET)
                                == PackageManager.PERMISSION_GRANTED &&
                                applicationContext.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                                        == PackageManager.PERMISSION_GRANTED &&
                                applicationContext.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                                        == PackageManager.PERMISSION_GRANTED) {
                            boolean checked = preference.getSharedPreferences().getBoolean("notification_ISS", false);
                            sharedPref.edit().putBoolean(applicationContext.getString(R.string.askPermission), false).apply();
                            Preferences.iss_Service(checked, applicationContext);
                        } else {
                            ActivityCompat.requestPermissions(activity, new String[]{
                                    android.Manifest.permission.INTERNET,
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                        }
                    }
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
                    sharedPref.edit().putBoolean(context.getString(R.string.askPermission), false).apply();
                    context.startActivity(new Intent(context, Locations.class));
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
}
