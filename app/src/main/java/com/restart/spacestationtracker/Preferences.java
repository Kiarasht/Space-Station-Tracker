package com.restart.spacestationtracker;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.restart.spacestationtracker.services.Alert;
import com.restart.spacestationtracker.view.SeekBarPreference;

/**
 * Manages the settings page of the apps from to have the user controls the visual and flow of the
 * application.
 *
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
        setContentView(R.layout.layout_general);

        if (savedInstanceState == null) {
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            SettingsFragment settingsFragment = new SettingsFragment();
            fragmentTransaction.add(android.R.id.content, settingsFragment, "SETTINGS_FRAGMENT");
            fragmentTransaction.commit();
        }
    }

    /**
     * Settings fragment where all the management of a users preference occurs.
     */
    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {
        private SharedPreferences mSharedPreferences;
        private PreferenceScreen mPreferenceScreen;
        private SeekBarPreference mRefreshRate;
        private SeekBarPreference mPredictionSize;
        private SeekBarPreference mDecimalPlaces;
        private SeekBarPreference mTextSize;
        private Context mContext;
        private Activity mActivity;

        /**
         * Setup variables and any click/change listeners to listen to user's new requests.
         *
         * @param savedInstanceState N/A
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.app_preferences);

            mActivity = getActivity();
            mContext = mActivity.getApplicationContext();
            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
            mPreferenceScreen = getPreferenceScreen();

            // Enable seekbars
            mRefreshRate = (SeekBarPreference) mPreferenceScreen.findPreference("refresh_Rate");
            mRefreshRate.setSummary(this.getString(R.string.refreshSummary).replace("$1", "" + (mSharedPreferences.getInt("refresh_Rate", 9) + 1)));
            mPredictionSize = (SeekBarPreference) mPreferenceScreen.findPreference("sizeType");
            mPredictionSize.setSummary(this.getString(R.string.sizeSummary).replace("$1", "" + mSharedPreferences.getInt("sizeType", 5)));
            mDecimalPlaces = (SeekBarPreference) mPreferenceScreen.findPreference("decimalType");
            mDecimalPlaces.setSummary(this.getString(R.string.decimalSummary).replace("$1", "" + mSharedPreferences.getInt("decimalType", 3)));
            mTextSize = (SeekBarPreference) mPreferenceScreen.findPreference("textSizeType");
            mTextSize.setSummary(this.getString(R.string.textSizeSummary).replace("$1", "" + mSharedPreferences.getInt("textSizeType", 12)));
            mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

            // OnClick methods for each of the check boxes
            mPreferenceScreen.findPreference("advertisement").setOnPreferenceClickListener(this);
            mPreferenceScreen.findPreference("fullPage").setOnPreferenceClickListener(this);
            mPreferenceScreen.findPreference("notification_ISS").setOnPreferenceClickListener(this);
        }

        /**
         * Unregister the listeners
         */
        @Override
        public void onStop() {
            super.onStop();
            mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }

        /**
         * Start or stop the Alert.java service.
         *
         * @param checked Are we starting or destroying the service?
         */
        public void iss_Service(boolean checked) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
            if (checked) {
                sharedPreferences.edit().putLong("time", 0).apply();
                Toast.makeText(mContext, R.string.startAlert, Toast.LENGTH_SHORT).show();
                mContext.startService(new Intent(mContext, Alert.class));
            } else {
                sharedPreferences.edit().putLong("time", 0).apply();
                Toast.makeText(mContext, R.string.stopAlert, Toast.LENGTH_SHORT).show();
                mContext.stopService(new Intent(mContext, Alert.class));
            }
        }

        /**
         * Get the permissions needed for the Alert.class
         */
        @TargetApi(Build.VERSION_CODES.M)
        public void getLocationPermission() {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        /**
         * If permission was granted, check notification service box for the user so they don't have
         * to accept the permission and then click on the preference again.
         *
         * If permission was denied, then uncheck it.
         *
         * @param requestCode  For managing requests, in this case it's just 1
         * @param permissions  Would be nice to get internet and location
         * @param grantResults The ACCESS_FINE_LOCATION must to be granted
         */
        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
            CheckBoxPreference iss_Tracker = (CheckBoxPreference) mPreferenceScreen.findPreference("notification_ISS");

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                iss_Tracker.setChecked(false);
            } else {
                iss_Tracker.setChecked(false);
            }
        }

        /**
         * Check to see if user has given us the permission to access their location.
         *
         * @return True if permission is granted. False if otherwise.
         */
        public boolean isLocationPermissionGranted() {
            return Build.VERSION.SDK_INT <= Build.VERSION_CODES.M || mContext.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        /**
         * Handle any preference changes. In this case we use this listener for our seekbars.
         *
         * @param sharedPreferences The incoming preference that was changed.
         * @param key               The key corresponding to the preference that was changed.
         */
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            switch (key) {
                case "refresh_Rate":
                    mRefreshRate.setSummary(this.getString(R.string.refreshSummary).replace("$1", "" + (mSharedPreferences.getInt("refresh_Rate", 9) + 1)));
                    break;
                case "sizeType":
                    mPredictionSize.setSummary(this.getString(R.string.sizeSummary).replace("$1", "" + mSharedPreferences.getInt("sizeType", 5)));
                    break;
                case "decimalType":
                    mDecimalPlaces.setSummary(this.getString(R.string.decimalSummary).replace("$1", "" + mSharedPreferences.getInt("decimalType", 3)));
                    break;
                case "textSizeType":
                    mTextSize.setSummary(this.getString(R.string.textSizeSummary).replace("$1", "" + mSharedPreferences.getInt("textSizeType", 12)));
                default:
                    break;
            }
        }

        /**
         * Called when a Preference has been clicked.
         *
         * @param preference The Preference that was clicked.
         * @return True if the click was handled.
         */
        @Override
        public boolean onPreferenceClick(Preference preference) {
            switch (preference.getKey()) {
                case "advertisement":
                    return onBannerAds(preference);
                case "fullPage":
                    return onFullPageAds(preference);
                case "notification_ISS":
                    return onISSNotification(preference);
                default:
                    return false;
            }
        }

        /**
         * When the preference corresponding with the banner ad was clicked.
         * We just inform the user if they unchecked it. We will later use whether if this checkbox
         * is checked or unchecked to display ads.
         *
         * @param preference The incoming preference that was clicked
         * @return We handled the click, so pass it back
         */
        private boolean onBannerAds(Preference preference) {
            boolean checked = preference.getSharedPreferences().getBoolean("advertisement", false);

            if (!checked) {
                Toast.makeText(mContext, R.string.startBannerAds, Toast.LENGTH_SHORT).show();
            }

            return true;
        }

        /**
         * When the preference corresponding with the full page ad was clicked.
         * We just inform the user if they unchecked it. We will later use whether if this checkbox
         * is checked or unchecked to display ads.
         *
         * @param preference The incoming preference that was clicked.
         * @return We handled the click, so pass it back.
         */
        private boolean onFullPageAds(Preference preference) {
            boolean checked = preference.getSharedPreferences().getBoolean("fullPage", false);

            if (!checked) {
                Toast.makeText(mContext, R.string.startFullAds, Toast.LENGTH_SHORT).show();
            }

            return true;
        }


        /**
         * Start or stop the notification service. But first also check if we location permission is
         * something we need to worry about.
         *
         * @param preference The incoming preference that was clicked.
         * @return We handled the click, so pass it back.
         */
        private boolean onISSNotification(Preference preference) {
            if (isLocationPermissionGranted()) {
                boolean checked = preference.getSharedPreferences().getBoolean("notification_ISS", false);
                iss_Service(checked);
                return true;
            } else {
                getLocationPermission();
                return true;
            }
        }
    }
}
