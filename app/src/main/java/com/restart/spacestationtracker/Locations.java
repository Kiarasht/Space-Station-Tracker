package com.restart.spacestationtracker;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
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
 * Json parsing to read the expected time and dates the ISS will pass by the user's location.
 * This class will require to read user's location.
 */
public class Locations extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private RequestQueue requestQueue;
    private String mLontitude;
    private String mLatitude;
    private String mLocation;
    protected Location mLastLocation;
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Assign simple widgets while also use the Google API to get user's location.
     *
     * @param savedInstanceState on create method
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_locations);
        isStoragePermissionGranted();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        startAnimation();

        requestQueue = Volley.newRequestQueue(this);
        buildGoogleApiClient();

        // Show an ad, or hide it if its disabled
        if (!sharedPreferences.getBoolean("advertisement", false)) {
            AdView adView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().addTestDevice("998B51E0DA18B35E1A4C4E6D78084ABB").build();
            if (adView != null) {
                adView.loadAd(adRequest);
            }
        } else {
            findViewById(R.id.adView).setVisibility(View.GONE);
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
     * API was successful in Connecting. Lets find user's location.
     *
     * @param connectionHint Bundle connectionHint
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Check if we have the right permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Call the api to get user's last known location
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        // If we got something back start parsing
        if (mLastLocation != null) {
            mLatitude = String.valueOf(mLastLocation.getLatitude());
            mLontitude = String.valueOf(mLastLocation.getLongitude());
            displayresults();
            displaypasses(null, null, getApplicationContext());
        } else {
            Toast.makeText(this, "Unable to find your location.", Toast.LENGTH_LONG).show();
            endAnimation();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Unable to find your location.", Toast.LENGTH_LONG).show();
        endAnimation();
    }

    /**
     * After successfully getting a Latitude and Longitude from the API, search a database to see
     * what city and country do these correspond to.
     */
    private void displayresults() {
        // Returns a JSONObject
        final String url = "http://maps.googleapis.com/maps/api/geocode/json?latlng=" +
                mLatitude +
                "," +
                mLontitude +
                "&sensor=false";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    // Save the formatted address, we will use it later
                    JSONObject results = response.getJSONArray("results").getJSONObject(1);
                    mLocation = results.getString("formatted_address");

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
                endAnimation();
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

    /**
     * After successfully getting a city and country from the last JSON parsing, search a database
     * to see when ISS will pass by this city, country.
     */
    public Date[] displaypasses(final String mLatitudepar, final String mLontitudepar, Context applicationContext) {
        // Usually we get 4 to 6 dates. So 10 just to be a bit safe
        final Date[] passes = new Date[10];

        String url;
        if (mLatitudepar == null && mLontitudepar == null) { // Location.java is calling this method
            url = "http://api.open-notify.org/iss-pass.json?lat=" +
                    mLatitude + "&lon=" + mLontitude;
        } else {                                            // Alert.java is calling this method
            url = "http://api.open-notify.org/iss-pass.json?lat=" +
                    mLatitudepar + "&lon=" + mLontitudepar;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray aresults = response.getJSONArray("response");
                    int[] duration = new int[aresults.length()]; // An array of ISS flyby durations
                    Date[] date = new Date[aresults.length()]; // An array of ISS flyby dates
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
                    StringBuilder stringBuilder;
                    final String[] dates = new String[aresults.length() + 1]; // This is what we print for user
                    dates[0] = "Location: " + mLocation; // The first index is User's location. That's why we did +1

                    // Go through all the JSON Arrays parsing through each JSON Object.
                    for (int i = 0; i < aresults.length(); ++i) {
                        JSONObject apass = aresults.getJSONObject(i);
                        date[i] = new Date(Long.parseLong(apass.getString("risetime")) * 1000L); // Turn into milliseconds
                        passes[i] = new Date(Long.parseLong(apass.getString("risetime")) * 1000L); // Same thing
                        duration[i] = apass.getInt("duration") / 60; // Turn each duration to minutes.
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Date: ").append(simpleDateFormat.format(date[i]).replace(" ", "\nTime: ")).append("\n")
                                .append("Duration: ").append(duration[i])
                                .append(" minutes");
                        dates[i + 1] = stringBuilder.toString(); // Save the parsed message
                    }

                    // If Locations.java called us lets create a ListView and run it on a UiThread
                    if (mLatitudepar == null && mLontitudepar == null) {
                        final ListAdapter datesAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.text_layout, dates);
                        final ListView datesListView = (ListView) findViewById(R.id.listView);
                        Locations.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                endAnimation();
                                datesListView.setAdapter(datesAdapter);

                                // If no city, country came back we still got our LAT and LON. Oh well! ¯\_(ツ)_/¯
                                if (dates[0].length() == 0) {
                                    String nocountrycity = "LAT: " +
                                            mLatitude +
                                            " LON: " +
                                            mLontitude;
                                    dates[0] = nocountrycity;
                                }
                            }
                        });
                    }

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

                Toast.makeText(Locations.this, "Either you have no connection or server is overloaded.", Toast.LENGTH_LONG).show();
                endAnimation();
            }
        });

        // If Alert.java called us
        if (requestQueue == null) {
            RequestQueue requestQueue = Volley.newRequestQueue(applicationContext);
            requestQueue.add(jsonObjectRequest);
        } else { // If Locations.java called us
            requestQueue.add(jsonObjectRequest);
        }
        return passes; // Only Alert.java benefits from this return
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
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.INTERNET}, 1);
                return false;
            }
        } else { // Permission is automatically granted on sdk < 23 upon installation
            return true;
        }
    }
}
