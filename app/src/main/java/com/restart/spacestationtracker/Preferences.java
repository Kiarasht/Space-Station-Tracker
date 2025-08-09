package com.restart.spacestationtracker;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

/**
 * Manages the settings page of the apps from to have the user controls the visual and flow of the
 * application.
 * <p>
 * TODO: As a user, I should have the option to reset an option or all options to their original values.
 */
public class Preferences extends AppCompatActivity {

    /**
     * Start the settings fragment and do it only if one already doesn't exists. The preference
     * activity only hosts the standalone fragment.
     *
     * @param savedInstanceState if it's null, then we need to create the fragment
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isNightMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;

        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(!isNightMode);
        setContentView(R.layout.layout_general);

        View fragmentContainer = findViewById(R.id.settings_container);

        ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            fragmentContainer.setPadding(fragmentContainer.getPaddingLeft(), insets.top, fragmentContainer.getPaddingRight(), insets.bottom);

            return WindowInsetsCompat.CONSUMED;
        });

        if (savedInstanceState == null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
    }

    /**
     * Settings fragment where all the management of a users preference occurs.
     */
    public static class SettingsFragment extends androidx.preference.PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        private SharedPreferences mSharedPreferences;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.app_preferences, rootKey);

            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

            androidx.preference.SeekBarPreference refreshRate = findPreference("refresh_Rate");
            if (refreshRate != null) {
                // Set a provider to format the summary text with the unit
                refreshRate.setSummaryProvider(preference -> {
                    int value = ((androidx.preference.SeekBarPreference) preference).getValue();
                    return value + " seconds";
                });
            }

            // You can do the same for your other seekbars
            androidx.preference.SeekBarPreference sizeType = findPreference("sizeType");
            if (sizeType != null) {
                sizeType.setSummaryProvider(preference -> {
                    int value = ((androidx.preference.SeekBarPreference) preference).getValue();
                    return value + " dp"; // Or whatever unit is appropriate
                });
            }
        }

        /**
         * Setup variables and any click/change listeners to listen to user's new requests.
         *
         * @param savedInstanceState N/A
         */
        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getListView().setClipToPadding(false);
        }

        /**
         * Unregister the listeners
         */
        @Override
        public void onStop() {
            super.onStop();
            mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        }
    }
}
