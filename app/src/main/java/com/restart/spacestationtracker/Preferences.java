package com.restart.spacestationtracker;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
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
            getPreferenceScreen().findPreference("refresh_Rate").setEnabled(true);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            boolean advertisement = sharedPreferences.getBoolean("advertisement", false);

            if (advertisement) {
                AdPreference myPref = (AdPreference) findPreference("ad_Preference");
                PreferenceCategory mCategory = (PreferenceCategory) findPreference("Advertisement");
                adPreference = myPref;
                mCategory.removePreference(myPref);
            }

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
                    boolean checked = preference.getSharedPreferences().getBoolean("notification_ISS", false);
                    Context context = getActivity().getApplicationContext();
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

    public static void iss_Service(boolean checked, Context context) {
        if (checked) {
            Toast.makeText(context, "Notify when ISS is close by", Toast.LENGTH_SHORT).show();
            context.startService(new Intent(context, Alert.class));
        } else {
            Toast.makeText(context, "Stop notify when ISS is close by", Toast.LENGTH_SHORT).show();
            context.stopService(new Intent(context, Alert.class));
        }
    }

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
