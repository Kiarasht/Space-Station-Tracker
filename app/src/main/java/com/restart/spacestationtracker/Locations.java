package com.restart.spacestationtracker;


import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

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
public class Locations extends MapsActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = ".Locations";
    private TextView countrycity;
    private TextView isspasses;
    private String mLatitude;
    private String mLontitude;
    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;

    /**
     * Assign simple widgets while also use the Google API to get user's location.
     * @param savedInstanceState on create method
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_locations);
        countrycity = (TextView) findViewById(R.id.textView2);
        isspasses = (TextView) findViewById(R.id.textView3);
        buildGoogleApiClient();
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
     * @param connectionHint Bundle connectionHint
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            mLatitude = String.valueOf(mLastLocation.getLatitude());
            mLontitude = String.valueOf(mLastLocation.getLongitude());
            displayresults();
            displaypasses();
        } else {
            Toast.makeText(this, "Unable to find your location.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " +
                connectionResult.getErrorCode());
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
                    URL urlHandle = new URL("http://scatter-otl.rhcloud.com/location?lat=" +
                            mLatitude + "&long=" + mLontitude);
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
                    JSONObject results = new JSONObject(strContent);

                    final String mCountry = results.getString("countrycode");
                    final String mCity = results.getString("city");
                    final String mLocation = mCountry + ", " + mCity;

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
    private void displaypasses() {
        AsyncTask.execute(new Runnable() {
            public void run() {
                String strContent = "";

                try {
                    URL urlHandle = new URL("http://api.open-notify.org/iss-pass.json?lat=" +
                            mLatitude + "&lon=" + mLontitude);
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
                        duration[i] = apass.getInt("duration") / 60;
                        stringBuilder.append(i + 1).append(".  ")
                                .append(simpleDateFormat.format(date[i]))
                                .append(" for ").append(duration[i])
                                .append(" m\n\n");
                    }

                    Locations.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isspasses.setText(stringBuilder);
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
