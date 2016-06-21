package com.restart.spacestationtracker;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;

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

    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.app_preferences);
            getPreferenceScreen().findPreference("refresh_Rate").setEnabled(true);
        }
    }
}
