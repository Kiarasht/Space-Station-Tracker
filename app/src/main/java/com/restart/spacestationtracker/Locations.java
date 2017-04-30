package com.restart.spacestationtracker;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Json parsing to read the expected time and dates the ISS will pass by the user's location.
 * This class will require to read user's location.
 */
public class Locations extends AppCompatActivity {

    private final String TAG = ".Locations";

    private LocationAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private RequestQueue requestQueue;
    private String mLongitude;
    private String mLatitude;
    private String mLocation;
    private AdView adView;
    private Activity mActivity;

    /**
     * Assign simple widgets while also use the Google API to get user's location.
     *
     * @param savedInstanceState on create method
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycler_layout);
        mActivity = this;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        requestQueue = Volley.newRequestQueue(this);
        Connected();

        // Show an ad, or hide it if its disabled
        if (!sharedPreferences.getBoolean("advertisement", false)) {
            adView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().addTestDevice(getString(R.string.test_device)).build();
            if (adView != null) {
                adView.loadAd(adRequest);
            }
        } else {
            findViewById(R.id.adView).setVisibility(View.GONE);
        }
    }

    /**
     * Cancel any request on Volley after user goes to another activity.
     */
    protected void onPause() {
        super.onPause();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }

        if (adView != null) {
            adView.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    public void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }

    /**
     * Lets find user's location.
     */
    public void Connected() {
        // Check if we have the right permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.errorPermissionLocation, Toast.LENGTH_LONG).show();
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location location = null;

        for (int i = providers.size() - 1; i >= 0; i--) {
            location = locationManager.getLastKnownLocation(providers.get(i));
            if (location != null) break;
        }

        // If we got something back start parsing
        if (location != null) {
            mLatitude = String.valueOf(location.getLatitude());
            mLongitude = String.valueOf(location.getLongitude());
            displayResults();
            displayPasses(null, null, null);
        } else {
            Toast.makeText(this, R.string.errorLocation, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * After successfully getting a Latitude and Longitude from the API, search a database to see
     * what city and country do these correspond to.
     */
    private void displayResults() {
        // Returns a JSONObject
        final String url = "http://maps.googleapis.com/maps/api/geocode/json?latlng=" +
                mLatitude +
                "," +
                mLongitude +
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
                Toast.makeText(Locations.this, R.string.errorNetwork, Toast.LENGTH_LONG).show();
            }
        });
        jsonObjectRequest.setTag(TAG);
        requestQueue.add(jsonObjectRequest);
    }

    /**
     * After successfully getting a city and country from the last JSON parsing, search a database
     * to see when ISS will pass by this city, country.
     */
    public List<Date> displayPasses(final String latitude, final String longitude, final Context applicationContext) {
        // Usually we get 4 to 6 dates. So 10 just to be a bit safe
        final List<Date> passes = new ArrayList<>(); // Used for Alert service

        final String url;
        if (latitude == null && longitude == null) { // Location.java is calling this method
            url = "http://api.open-notify.org/iss-pass.json?lat=" +
                    mLatitude + "&lon=" + mLongitude;
        } else {                                            // Alert.java is calling this method
            url = "http://api.open-notify.org/iss-pass.json?lat=" +
                    latitude + "&lon=" + longitude;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray results = response.getJSONArray("response");
                    int[] duration = new int[results.length()]; // An array of ISS flyby durations
                    Date[] date = new Date[results.length()]; // An array of ISS flyby dates
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
                    StringBuilder stringBuilder;
                    final List<String> dates = new ArrayList<>(); // This is what we print for user

                    Resources resources = applicationContext != null ? applicationContext.getResources() : getResources();

                    // Go through all the JSON Arrays parsing through each JSON Object.
                    for (int i = 0; i < results.length(); ++i) {
                        JSONObject aPass = results.getJSONObject(i);
                        date[i] = new Date(Long.parseLong(aPass.getString("risetime")) * 1000L); // Turn into milliseconds
                        passes.add(new Date(Long.parseLong(aPass.getString("risetime")) * 1000L)); // Same thing
                        duration[i] = aPass.getInt("duration") / 60; // Turn each duration to minutes.
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(resources.getString(R.string.date)).append(": ").append(simpleDateFormat.format(date[i])
                                .replace(" ", "\n" + resources.getString(R.string.time) + ": ")).append("\n")
                                .append(resources.getString(R.string.duration)).append(": ").append(duration[i])
                                .append(" ").append(resources.getString(R.string.minutes));
                        dates.add(stringBuilder.toString()); // Save the parsed message
                    }

                    // If Locations.java called us lets create a ListView and run it on a UiThread
                    if (latitude == null && longitude == null) {
                        dates.add(resources.getString(R.string.location) + ": " + mLocation);  // The first index is User's location. That's why we did +1

                        // Fail safe. Sometimes, mLocation isn't found by the previous JSON call.
                        if (mLocation == null) {
                            final DecimalFormat decimalFormat = new DecimalFormat("0.000");
                            final String LAT = decimalFormat.format(Double.parseDouble(mLatitude));
                            final String LNG = decimalFormat.format(Double.parseDouble(mLongitude));
                            dates.add(resources.getString(R.string.location) + ": " + LAT + "° N, " + LNG + "° E");
                        }

                        LinearLayoutManager layoutManager = new LinearLayoutManager(mActivity, LinearLayoutManager.VERTICAL, false);
                        mRecyclerView = (RecyclerView) findViewById(R.id.recycler);
                        mRecyclerView.setLayoutManager(layoutManager);
                        mAdapter = new LocationAdapter(mActivity, dates);
                        mRecyclerView.setHasFixedSize(true);
                        mRecyclerView.setNestedScrollingEnabled(true);
                        mAdapter.setDataSet(dates);
                        mRecyclerView.setAdapter(mAdapter);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                if (latitude == null && longitude == null) {
                    Toast.makeText(Locations.this, R.string.errorConnection, Toast.LENGTH_LONG).show();
                }
            }
        });

        // If Alert.java called us
        if (requestQueue == null) {
            RequestQueue requestQueue = Volley.newRequestQueue(applicationContext);
            requestQueue.add(jsonObjectRequest);
        } else { // If Locations.java called us
            jsonObjectRequest.setTag(TAG);
            requestQueue.add(jsonObjectRequest);
        }

        return passes; // Only Alert.java benefits from this return
    }
}
