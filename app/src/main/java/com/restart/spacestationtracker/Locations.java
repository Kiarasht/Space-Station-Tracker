package com.restart.spacestationtracker;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.restart.spacestationtracker.adapter.LocationAdapter;
import com.restart.spacestationtracker.data.SightSee;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Json parsing to read the expected time and dates the ISS will pass by the user's location.
 * This class will require to read user's location.
 */
public class Locations extends AppCompatActivity {
    private final String TAG = ".Locations";

    private ImageView mImageView;
    private LocationAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private RequestQueue requestQueue;
    private String mLongitude;
    private String mLatitude;
    private Activity mActivity;
    private CollapsingToolbarLayout mCollapsingToolbar;

    /**
     * Assign simple widgets while also use the Google API to get user's location.
     *
     * @param savedInstanceState on create method
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.locations_layout);
        Toolbar mToolbar = findViewById(R.id.toolbar);
        mToolbar.setNavigationIcon(R.drawable.ic_menu_home);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        mActivity = this;

        LinearLayoutManager layoutManager = new LinearLayoutManager(mActivity, LinearLayoutManager.VERTICAL, false);
        mRecyclerView = findViewById(R.id.recycler);
        AdView mAdView = findViewById(R.id.adView);
        mAdView.loadAd(new AdRequest.Builder().build());

        mImageView = findViewById(R.id.image);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setNestedScrollingEnabled(true);
        mCollapsingToolbar = findViewById(R.id.collapsing_toolbar);
        mCollapsingToolbar.setTitle(getString(R.string.flybys_title));

        // mRecyclerViewBackground makes RecyclerView's background except header view.
        requestQueue = Volley.newRequestQueue(this);
        Connected();
    }

    /**
     * Cancel any request on Volley after user goes to another activity.
     */
    protected void onPause() {
        super.onPause();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }

    /**
     * Lets find user's location.
     */
    private void Connected() {
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
            String url = "https://maps.googleapis.com/maps/api/staticmap?" +
                    "center=LAT,LNG&" +
                    "zoom=10&" +
                    "scale=1&" +
                    "size=640x640&" +
                    "maptype=hybrid&" +
                    "style=feature:road|visibility:off&" +
                    "style=feature:poi|visibility:off&" +
                    "style=feature:landscape|visibility:off&" +
                    "style=feature:transit|visibility:off&" +
                    "style=feature:administrative.province|visibility:off&" +
                    "style=feature:administrative.neighborhood|visibility:off&" +
                    "key=AIzaSyAtpWPhzhbtqTgofnQhAHjiG12MmrY2AAE";

            mLatitude = String.valueOf(location.getLatitude());
            mLongitude = String.valueOf(location.getLongitude());
            url = url.replace("LAT", mLatitude);
            url = url.replace("LNG", mLongitude);

            try {
                List<Address> matches = new Geocoder(this).getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                final Address bestMatch = (matches.isEmpty() ? null : matches.get(0));
                if (bestMatch != null) {
                    String locationFormat = "";

                    if (!"null".equals(bestMatch.getLocality())) {
                        locationFormat += bestMatch.getLocality() + ", ";
                    }

                    if (!"null".equals(bestMatch.getAdminArea())) {
                        locationFormat += bestMatch.getAdminArea() + " ";
                    }

                    if (!"null".equals(bestMatch.getCountryCode())) {
                        locationFormat += bestMatch.getCountryCode() + " ";
                    }

                    if (!"null".equals(bestMatch.getPostalCode())) {
                        locationFormat += bestMatch.getPostalCode();
                    }

                    SightSee.setLocation(locationFormat);
                    mCollapsingToolbar.setTitle(bestMatch.getLocality() + ", " + bestMatch.getAdminArea());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            Picasso.get().load(url).into(mImageView);
            displayPasses(null, null, null);
        } else {
            Toast.makeText(this, R.string.errorLocation, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * After successfully getting a city and country from the last JSON parsing, search a database
     * to see when ISS will pass by this city, country.
     */
    public List<Date> displayPasses(final String latitude, final String longitude, final Context applicationContext) {
        final List<Date> passes = new ArrayList<>(); // Used for Alert service
        final String url;

        if (latitude == null && longitude == null) { // Location.java is calling this method
            url = "http://api.open-notify.org/iss-pass.json?lat=" + mLatitude + "&lon=" + mLongitude + "&n=20";
        } else { // Alert.java is calling this method
            url = "http://api.open-notify.org/iss-pass.json?lat=" + latitude + "&lon=" + longitude;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            try {
                final JSONArray results = response.getJSONArray("response");
                final List<SightSee> dates = new ArrayList<>();

                for (int i = 0; i < results.length(); ++i) {
                    final JSONObject aPass = results.getJSONObject(i);
                    passes.add(new Date(Long.parseLong(aPass.getString("risetime")) * 1000L));
                    final SightSee aSightSee = new SightSee(aPass.getInt("duration"), aPass.getInt("risetime"));
                    dates.add(aSightSee);
                }

                if (mActivity != null) {
                    mAdapter = new LocationAdapter(mActivity);
                    mAdapter.setDataSet(dates);
                    mRecyclerView.setAdapter(mAdapter);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, e -> {
            if (latitude == null && longitude == null) {
                Toast.makeText(Locations.this, R.string.errorConnection, Toast.LENGTH_LONG).show();
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
