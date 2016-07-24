package com.restart.spacestationtracker;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.restart.spacestationtracker.services.Alert;
import com.restart.spacestationtracker.services.AlertPeople;

public class Preferences extends AppCompatActivity {
    private static AdPreference adPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_general);
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        SettingsFragment settingsFragment = new SettingsFragment();
        fragmentTransaction.add(android.R.id.content, settingsFragment, "SETTINGS_FRAGMENT");
        fragmentTransaction.commit();
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.app_preferences);
            // Enable the seekbar
            getPreferenceScreen().findPreference("refresh_Rate").setEnabled(true);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            boolean advertisement = sharedPreferences.getBoolean("advertisement", false);

            // Advertisement management, if true remove it
            if (advertisement) {
                AdPreference myPref = (AdPreference) findPreference("ad_Preference");
                PreferenceCategory mCategory = (PreferenceCategory) findPreference("Advertisement");
                adPreference = myPref;
                mCategory.removePreference(myPref);
            }

            // Onclick methods for each of the check boxes
            getPreferenceScreen().findPreference("advertisement").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    boolean checked = preference.getSharedPreferences().getBoolean("advertisement", false);
                    Context context = getActivity().getApplicationContext();

                    if (checked) {
                        Toast.makeText(context, "Ads disabled. Consider enabling them when non-intrusive", Toast.LENGTH_LONG).show();
                        AdPreference myPref = (AdPreference) findPreference("ad_Preference");
                        PreferenceCategory mCategory = (PreferenceCategory) findPreference("Advertisement");
                        adPreference = myPref;
                        mCategory.removePreference(myPref);
                    } else {
                        Toast.makeText(context, "Ads enabled. Thanks for the support ;)", Toast.LENGTH_SHORT).show();
                        PreferenceCategory mCategory = (PreferenceCategory) findPreference("Advertisement");
                        mCategory.addPreference(adPreference);
                    }

                    return true;
                }
            });

            getPreferenceScreen().findPreference("notification_ISS").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Context context = getActivity().getApplicationContext();

                    // Starting Flyby service? Check location permission
                    if (Build.VERSION.SDK_INT >= 23) {
                        if (context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED &&
                                context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                                        == PackageManager.PERMISSION_GRANTED &&
                                context.checkSelfPermission(android.Manifest.permission.INTERNET)
                                        == PackageManager.PERMISSION_GRANTED) {
                            return true;
                        } else {
                            ActivityCompat.requestPermissions(getActivity(), new String[]{
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.INTERNET}, 1);
                            return false;
                        }
                    }

                    boolean checked = preference.getSharedPreferences().getBoolean("notification_ISS", false);
                    iss_Service(checked, context);

                    return true;
                }
            });

            getPreferenceScreen().findPreference("notification_Astro").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    boolean checked = preference.getSharedPreferences().getBoolean("notification_Astro", false);
                    Context context = getActivity().getApplicationContext();
                    astro_Service(checked, context);
                    return true;
                }
            });
        }
    }

    /**
     * Start or stop the Alert.java service.
     *
     * @param checked Are we starting or destroying the service?
     * @param context Application's context
     */
    public static void iss_Service(boolean checked, Context context) {
        if (checked) {
            Toast.makeText(context, "Notify when ISS is close by", Toast.LENGTH_SHORT).show();
            context.startService(new Intent(context, Alert.class));
        } else {
            Toast.makeText(context, "Stop notify when ISS is close by", Toast.LENGTH_SHORT).show();
            context.stopService(new Intent(context, Alert.class));
        }
    }

    /**
     * Start or stop the AlertPeople.java service.
     *
     * @param checked Are we starting or destroying the service?
     * @param context Application's context
     */
    public static void astro_Service(boolean checked, Context context) {
        if (checked) {
            Toast.makeText(context, "Notify when people in space change", Toast.LENGTH_SHORT).show();
            context.startService(new Intent(context, AlertPeople.class));
        } else {
            Toast.makeText(context, "Stop notify when people in space change", Toast.LENGTH_SHORT).show();
            context.stopService(new Intent(context, AlertPeople.class));
        }
    }
}
