package com.restart.spacestationtracker;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.ksoichiro.android.observablescrollview.ObservableRecyclerView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.github.ksoichiro.android.observablescrollview.ScrollUtils;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.nineoldandroids.view.ViewHelper;
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
public class Locations extends AppCompatActivity implements ObservableScrollViewCallbacks {

    private final String TAG = ".Locations";
    private static final float MAX_TEXT_SCALE_DELTA = 0.3f;

    private ImageView mImageView;
    private View mOverlayView;
    private TextView mTitleView;
    private LocationAdapter mAdapter;
    private ObservableRecyclerView mRecyclerView;
    private RequestQueue requestQueue;
    private String mLongitude;
    private String mLatitude;
    private String mLocation;
    private AdView adView;
    private View mRecyclerViewBackground;
    private Activity mActivity;
    private int mActionBarSize;
    private int mFlexibleSpaceImageHeight;

    private int getActionBarSize() {
        TypedValue typedValue = new TypedValue();
        int[] textSizeAttr = new int[]{R.attr.actionBarSize};
        int indexOfAttrTextSize = 0;
        TypedArray a = obtainStyledAttributes(typedValue.data, textSizeAttr);
        int actionBarSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1);
        a.recycle();
        return actionBarSize;
    }

    /**
     * Assign simple widgets while also use the Google API to get user's location.
     *
     * @param savedInstanceState on create method
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycler_layout);
        mActivity = this;

        mFlexibleSpaceImageHeight = getResources().getDimensionPixelSize(R.dimen.flexible_space_image_height);
        mActionBarSize = getActionBarSize();

        mImageView = (ImageView) findViewById(R.id.image);
        mOverlayView = findViewById(R.id.overlay);

        mTitleView = (TextView) findViewById(R.id.title);
        mTitleView.setText(getTitle());
        setTitle(null);

        LinearLayoutManager layoutManager = new LinearLayoutManager(mActivity, LinearLayoutManager.VERTICAL, false);
        mRecyclerView = (ObservableRecyclerView) findViewById(R.id.recycler);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mRecyclerView.getLayoutParams();
        // Show an ad, or hide it if its disabled
        if (!sharedPreferences.getBoolean("advertisement", false)) {
            adView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().addTestDevice(getString(R.string.test_device)).build();
            params.addRule(RelativeLayout.ALIGN_TOP, R.id.adView);
            if (adView != null) {
                adView.loadAd(adRequest);
            }
        } else {
            findViewById(R.id.adView).setVisibility(View.GONE);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        }

        mRecyclerView.setLayoutParams(params);
        mRecyclerView.setScrollViewCallbacks(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setNestedScrollingEnabled(true);

        // mRecyclerViewBackground makes RecyclerView's background except header view.
        mRecyclerViewBackground = findViewById(R.id.list_background);

        //since you cannot programmatically add a header view to a RecyclerView we added an empty view as the header
        // in the adapter and then are shifting the views OnCreateView to compensate
        final float scale = 1 + MAX_TEXT_SCALE_DELTA;
        mRecyclerViewBackground.post(new Runnable() {
            @Override
            public void run() {
                ViewHelper.setTranslationY(mRecyclerViewBackground, mFlexibleSpaceImageHeight);
            }
        });
        ViewHelper.setTranslationY(mOverlayView, mFlexibleSpaceImageHeight);
        mTitleView.post(new Runnable() {
            @Override
            public void run() {
                ViewHelper.setTranslationY(mTitleView, (int) (mFlexibleSpaceImageHeight - mTitleView.getHeight() * scale));
                ViewHelper.setPivotX(mTitleView, 0);
                ViewHelper.setPivotY(mTitleView, 0);
                ViewHelper.setScaleX(mTitleView, scale);
                ViewHelper.setScaleY(mTitleView, scale);
            }
        });

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
                    "zoom=13&" +
                    "scale=1&" +
                    "size=640x640&" +
                    "markers=color:red%7CLAT,LNG&" +
                    "key=AIzaSyAtpWPhzhbtqTgofnQhAHjiG12MmrY2AAE";

            mLatitude = String.valueOf(location.getLatitude());
            mLongitude = String.valueOf(location.getLongitude());
            url = url.replace("LAT", mLatitude);
            url = url.replace("LNG", mLongitude);

            try {
                List<Address> matches = new Geocoder(this).getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                final Address bestMatch = (matches.isEmpty() ? null : matches.get(0));
                if (bestMatch != null) {
                    findViewById(R.id.title).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.e(TAG, bestMatch.toString());
                        }
                    });

                    String locationFormat = "";

                    if (!"null".equals(bestMatch.getLocality())) {
                        locationFormat += bestMatch.getLocality() + ", ";
                    }

                    if (!"null".equals(bestMatch.getAdminArea())) {
                        locationFormat += bestMatch.getAdminArea();
                    }

                    if (!"null".equals(bestMatch.getCountryCode())) {
                        locationFormat += bestMatch.getCountryCode();
                    }

                    if (!"null".equals(bestMatch.getPostalCode())) {
                        locationFormat += bestMatch.getPostalCode();
                    }

                    mTitleView.setText(locationFormat);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            Picasso.with(mActivity).load(url).into(mImageView);
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
                    SightSee.setLocation(mLocation);
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
        final List<Date> passes = new ArrayList<>(); // Used for Alert service
        final String url;

        if (latitude == null && longitude == null) { // Location.java is calling this method
            url = "http://api.open-notify.org/iss-pass.json?lat=" +
                    mLatitude + "&lon=" + mLongitude + "&n=20";
        } else {                                     // Alert.java is calling this method
            url = "http://api.open-notify.org/iss-pass.json?lat=" +
                    latitude + "&lon=" + longitude;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    final JSONArray results = response.getJSONArray("response");
                    final List<SightSee> dates = new ArrayList<>();

                    for (int i = 0; i < results.length(); ++i) {
                        final JSONObject aPass = results.getJSONObject(i);
                        passes.add(new Date(Long.parseLong(aPass.getString("risetime")) * 1000L));
                        final SightSee aSightSee = new SightSee(aPass.getInt("duration"), aPass.getInt("risetime"));
                        dates.add(aSightSee);
                    }

                    final View headerView = LayoutInflater.from(mActivity).inflate(R.layout.recycler_header, null);
                    headerView.post(new Runnable() {
                        @Override
                        public void run() {
                            headerView.getLayoutParams().height = mFlexibleSpaceImageHeight;
                        }
                    });
                    mAdapter = new LocationAdapter(mActivity, headerView);
                    mAdapter.setDataSet(dates);
                    mRecyclerView.setAdapter(mAdapter);
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

    /**
     * Called when the scroll change events occurred.
     * This won't be called just after the view is laid out, so if you'd like to
     * initialize the position of your views with this method, you should call this manually
     * or invoke scroll as appropriate.
     *
     * @param scrollY     scroll position in Y axis
     * @param firstScroll true when this is called for the first time in the consecutive motion events
     * @param dragging    true when the view is dragged and false when the view is scrolled in the inertia
     */
    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
// Translate overlay and image
        float flexibleRange = mFlexibleSpaceImageHeight - mActionBarSize;
        int minOverlayTransitionY = mActionBarSize - mOverlayView.getHeight();
        ViewHelper.setTranslationY(mOverlayView, ScrollUtils.getFloat(-scrollY, minOverlayTransitionY, 0));
        ViewHelper.setTranslationY(mImageView, ScrollUtils.getFloat(-scrollY / 2, minOverlayTransitionY, 0));

        // Translate list background
        ViewHelper.setTranslationY(mRecyclerViewBackground, Math.max(0, -scrollY + mFlexibleSpaceImageHeight));

        // Change alpha of overlay
        ViewHelper.setAlpha(mOverlayView, ScrollUtils.getFloat((float) scrollY / flexibleRange, 0, 1));

        // Scale title text
        float scale = 1 + ScrollUtils.getFloat((flexibleRange - scrollY) / flexibleRange, 0, MAX_TEXT_SCALE_DELTA);
        setPivotXToTitle();
        ViewHelper.setPivotY(mTitleView, 0);
        ViewHelper.setScaleX(mTitleView, scale);
        ViewHelper.setScaleY(mTitleView, scale);

        // Translate title text
        int maxTitleTranslationY = (int) (mFlexibleSpaceImageHeight - mTitleView.getHeight() * scale);
        int titleTranslationY = maxTitleTranslationY - scrollY;
        ViewHelper.setTranslationY(mTitleView, titleTranslationY);
    }

    /**
     * Called when the down motion event occurred.
     */
    @Override
    public void onDownMotionEvent() {}

    /**
     * Called when the dragging ended or canceled.
     *
     * @param scrollState state to indicate the scroll direction
     */
    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {}

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void setPivotXToTitle() {
        Configuration config = getResources().getConfiguration();
        if (Build.VERSION_CODES.JELLY_BEAN_MR1 <= Build.VERSION.SDK_INT
                && config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            ViewHelper.setPivotX(mTitleView, findViewById(android.R.id.content).getWidth());
        } else {
            ViewHelper.setPivotX(mTitleView, 0);
        }
    }
}
