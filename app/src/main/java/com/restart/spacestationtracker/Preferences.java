package com.restart.spacestationtracker;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class Preferences extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_general);
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        SettingsFragment settingsFragment = new SettingsFragment();
        fragmentTransaction.add(android.R.id.content, settingsFragment, "SETTINGS_FRAGMENT");
        fragmentTransaction.commit();
        loadPreferences();
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.app_preferences);
            getPreferenceScreen().findPreference("refresh_Rate").setEnabled(true);

            getPreferenceScreen().findPreference("advertisement").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    boolean checked = preference.getSharedPreferences().getBoolean("advertisement", false);
                    Context context = getActivity().getApplicationContext();

                    if (checked) {
                        Toast.makeText(context, "Ads disabled. Consider enabling them when non-intrusive", Toast.LENGTH_LONG).show();
                        com.restart.spacestationtracker.AdPreference mCheckBoxPref = (com.restart.spacestationtracker.AdPreference) findPreference("ad_Preference");
                        PreferenceCategory mCategory = (PreferenceCategory) findPreference("Advertisement");
                        mCategory.removePreference(mCheckBoxPref);
                    } else {
                        Toast.makeText(context, "Ads enabled. Thanks for the support ;)", Toast.LENGTH_SHORT).show();
                    }

                    return true;
                }
            });

            getPreferenceScreen().findPreference("notification_ISS").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Toast.makeText(getActivity().getApplicationContext(), preference.getSharedPreferences().getBoolean("notification_ISS", false) + "", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });

            getPreferenceScreen().findPreference("notification_Astro").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Toast.makeText(getActivity().getApplicationContext(), preference.getSharedPreferences().getBoolean("notification_Astro", false) + "", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }
    }

    private void loadPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        boolean advertisement = sharedPreferences.getBoolean("advertisement", false);

        boolean notification_ISS = sharedPreferences.getBoolean("notification_ISS", false);
        boolean notification_Astro = sharedPreferences.getBoolean("notification_Astro", false);
        int refresh_Rate = sharedPreferences.getInt("refresh_Rate", 15);
    }
}
