package com.restart.spacestationtracker;


import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    private RequestQueue requestQueue;
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

        requestQueue = Volley.newRequestQueue(this);
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
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Unable to find your location.", Toast.LENGTH_LONG).show();
    }

    /**
     * After successfully getting a Latitude and Longitude from the API, search a database to see
     * what city and country do these correspond to.
     */
    private void displayresults() {
        String url = "http://maps.googleapis.com/maps/api/geocode/json?latlng=" +
                mLatitude +
                "," +
                mLontitude +
                "&sensor=false";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject results = response.getJSONArray("results").getJSONObject(1);
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
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                e.printStackTrace();
                NetworkResponse networkResponse = e.networkResponse;

                if (networkResponse != null && networkResponse.statusCode == HttpStatus.SC_UNAUTHORIZED) {
                    int error = networkResponse.statusCode;
                    String message = e.getMessage();
                    String reason = message + " Error: " + error;
                    Toast.makeText(Locations.this, reason + ".", Toast.LENGTH_LONG).show();

                    return;
                }

                Toast.makeText(Locations.this, "An unknown error has occurred. Error: 401", Toast.LENGTH_LONG).show();
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

    /**
     * After successfully getting a city and country from the last JSON parsing, search a database
     * to see when ISS will pass by this city, country.
     */
    public Date[] displaypasses(final String mLatitudepar, final String mLontitudepar) {
        final Date[] passes = new Date[10];

        String url;
        if (mLatitudepar == null && mLontitudepar == null) {
            url = "http://api.open-notify.org/iss-pass.json?lat=" +
                    mLatitude + "&lon=" + mLontitude;
        } else {
            url = "http://api.open-notify.org/iss-pass.json?lat=" +
                    mLatitudepar + "&lon=" + mLontitudepar;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray aresults = response.getJSONArray("response");
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
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                e.printStackTrace();
                NetworkResponse networkResponse = e.networkResponse;

                if (networkResponse != null && networkResponse.statusCode == HttpStatus.SC_UNAUTHORIZED) {
                    int error = networkResponse.statusCode;
                    String message = e.getMessage();
                    String reason = message + " Error: " + error;
                    Toast.makeText(Locations.this, reason + ".", Toast.LENGTH_LONG).show();

                    return;
                }

                Toast.makeText(Locations.this, "An unknown error has occurred. Error: 401", Toast.LENGTH_LONG).show();
            }
        });
        requestQueue.add(jsonObjectRequest);
        return passes;
    }

    void startAnimation() {
        View view = findViewById(R.id.avloadingIndicatorView);

        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }

    void endAnimation() {
        View view = findViewById(R.id.avloadingIndicatorView);

        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(android.Manifest.permission.INTERNET)
                            == PackageManager.PERMISSION_GRANTED) {
                //Toast.makeText(getApplicationContext(), "Cool beans! I got the permission, try that again", Toast.LENGTH_LONG).show();
                Log.v(TAG, "Permission is granted");
                return true;
            }/* else {

                Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.INTERNET}, 1);
                return false;
            }*/
            Toast.makeText(getApplicationContext(), "Can't find flybys without your location!", Toast.LENGTH_LONG).show();
            return false;
        } else { // Permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted");
            return true;
        }
    }
}
