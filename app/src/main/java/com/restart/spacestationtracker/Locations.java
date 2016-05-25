package com.restart.spacestationtracker;


import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Contains solely based on Json parsing to read the expected time and dates the ISS
 * will pass by the user's location. This class will require to read user's location
 * to function properly.
 */
public class Locations extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private final String TAG = ".Locations";
    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;
    private TextView countrycity;
    private TextView isspasses;
    private String mLontitude;
    private String mLatitude;

    /**
     * Assign simple widgets while also use the Google API to get user's location.
     *
     * @param savedInstanceState on create method
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_locations);
        isStoragePermissionGranted();
        SharedPreferences sharedPref = getSharedPreferences("savefile", MODE_PRIVATE);
        startAnimation();
        countrycity = (TextView) findViewById(R.id.textView2);
        isspasses = (TextView) findViewById(R.id.textView3);
        buildGoogleApiClient();

        if (!sharedPref.getBoolean(getString(R.string.notificationcheck3), false)) {
            AdView adView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
    }

    /**
     * The method for building our Google API. On method of this class
     * are includes only because of this API. We will later try and find
     * user's location.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * API was successful in getting the location. Parse them into strings.
     *
     * @param connectionHint Bundle connectionHint
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "I don't have the permission to access your location"
            , Toast.LENGTH_LONG).show();
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            mLatitude = String.valueOf(mLastLocation.getLatitude());
            mLontitude = String.valueOf(mLastLocation.getLongitude());
            displayresults();
            displaypasses(null, null);
        } else {
            Toast.makeText(this, "Unable to find your location.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Unable to find your location.", Toast.LENGTH_LONG).show();
    }

    /**
     * After successfully getting a Latitude and Longitude from the API, search a database to see
     * what city and country do these correspond to.
     */
    private void displayresults() {
        AsyncTask.execute(new Runnable() {
            public void run() {
                String strContent = "";

                try {
                    URL urlHandle = new URL("http://maps.googleapis.com/maps/api/geocode/json?latlng=" +
                            mLatitude +
                            "," +
                            mLontitude +
                            "&sensor=false");
                    URLConnection urlconnectionHandle = urlHandle.openConnection();
                    InputStream inputstreamHandle = urlconnectionHandle.getInputStream();

                    try {
                        int intRead;
                        byte[] byteBuffer = new byte[1024];

                        do {
                            intRead = inputstreamHandle.read(byteBuffer);

                            if (intRead == 0) {
                                break;

                            } else if (intRead == -1) {
                                break;
                            }

                            strContent += new String(byteBuffer, 0, intRead, "UTF-8");
                        } while (true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    inputstreamHandle.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    JSONObject results = new JSONObject(strContent).getJSONArray("results").getJSONObject(1);
                    final String mLocation = results.getString("formatted_address");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            countrycity.setText(mLocation);
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * After successfully getting a city and country from the last JSON parsing, search a database
     * to see when ISS will pass by this city, country.
     */
    public Date[] displaypasses(final String mLatitudepar, final String mLontitudepar) {
        final Date[] passes = new Date[10];

        AsyncTask.execute(new Runnable() {
            public void run() {
                String strContent = "";

                try {
                    URL urlHandle;
                    if (mLatitudepar == null && mLontitudepar == null) {
                        urlHandle = new URL("http://api.open-notify.org/iss-pass.json?lat=" +
                                mLatitude + "&lon=" + mLontitude);
                    } else {
                        urlHandle = new URL("http://api.open-notify.org/iss-pass.json?lat=" +
                                mLatitudepar + "&lon=" + mLontitudepar);
                    }
                    URLConnection urlconnectionHandle = urlHandle.openConnection();
                    InputStream inputstreamHandle = urlconnectionHandle.getInputStream();

                    try {
                        int intRead;
                        byte[] byteBuffer = new byte[1024];

                        do {
                            intRead = inputstreamHandle.read(byteBuffer);

                            if (intRead == 0) {
                                break;

                            } else if (intRead == -1) {
                                break;
                            }

                            strContent += new String(byteBuffer, 0, intRead, "UTF-8");
                        } while (true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    inputstreamHandle.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    JSONArray aresults = new JSONObject(strContent).getJSONArray("response");
                    int[] duration = new int[aresults.length()];
                    Date[] date = new Date[aresults.length()];
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
                    final StringBuilder stringBuilder = new StringBuilder();

                    for (int i = 0; i < aresults.length(); ++i) {
                        JSONObject apass = aresults.getJSONObject(i);
                        date[i] = new Date(Long.parseLong(apass.getString("risetime")) * 1000L);
                        passes[i] = new Date(Long.parseLong(apass.getString("risetime")) * 1000L);
                        duration[i] = apass.getInt("duration") / 60;
                        stringBuilder.append(i + 1).append(".  ")
                                .append(simpleDateFormat.format(date[i]))
                                .append(" for ").append(duration[i])
                                .append(" minutes.\n\n");
                    }

                    if (mLatitudepar == null && mLontitudepar == null)
                        Locations.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                endAnimation();
                                isspasses.setVisibility(View.VISIBLE);
                                isspasses.setText(stringBuilder);

                                if (countrycity.getText().toString().trim().length() == 0) {
                                    final StringBuilder nocountrycity = new StringBuilder();
                                    nocountrycity.append("LAT: ")
                                            .append(mLatitude)
                                            .append(" LON: ")
                                            .append(mLontitude);
                                    countrycity.setText(nocountrycity);
                                }
                            }
                        });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        return passes;
    }

    void startAnimation() {
        findViewById(R.id.avloadingIndicatorView).setVisibility(View.VISIBLE);
    }

    void endAnimation() {
        findViewById(R.id.avloadingIndicatorView).setVisibility(View.GONE);
    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(android.Manifest.permission.INTERNET)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.INTERNET}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }


    }
}
