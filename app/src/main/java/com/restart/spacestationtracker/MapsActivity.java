package com.restart.spacestationtracker;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.nightonke.boommenu.BoomButtons.ButtonPlaceEnum;
import com.nightonke.boommenu.BoomButtons.HamButton;
import com.nightonke.boommenu.BoomButtons.OnBMClickListener;
import com.nightonke.boommenu.BoomMenuButton;
import com.nightonke.boommenu.ButtonEnum;
import com.nightonke.boommenu.Piece.PiecePlaceEnum;
import com.wooplr.spotlight.SpotlightView;
import com.wooplr.spotlight.utils.SpotlightListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Contains the google map and uses volley to grab JSON objects and display
 * the position of the ISS as a customized marker on the map and update it occasionally.
 */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {

    private static final String TAG = ".MapsActivity";   // Used for volley and occasional Log

    private SharedPreferences mSharedPreferences;        // Managing options from Settings
    private BoomMenuButton mBoomMenu;                    // Manages the drawer pop menu
    private InterstitialAd mInterstitialAd;              // Managing interstitial ads with AdMob sdk
    private MarkerOptions mMarkerOptions;                // Marker options, uses ISS drawable
    private DecimalFormat mDecimalFormat;                // For number decimal places
    private RequestQueue mRequestQueue;                  // Volley for JSONObject/JSONArray requests
    private GoogleMap mMap;                              // Google maps
    private Polyline[] mPolyArray;                       // An array of polyline. Need 200 for a nice curve
    private TextView mDescription;                       // A description of additional ISS info. Optional
    private TextView mLatLong;                           // Lat & Lon of ISS
    private Polyline mPolyLine;                          // A single poly to help compare any changes from settings
    private Context mContext;                            // Application context
    private Marker mMarker;                              // Marker representing ISS that moves alone the prediction line
    private AdView mAdView;                              // Optional ads
    private LatLng mLast;                                // Used for connecting polyline.
    private Timer mPolyTimer;                            // Updates the poly lines
    private Timer mTimer;                                // Updates map based on refresh rate

    private int mInterstitialAdActivity;                 // Keeps tracks of what activity was requested to open but was paused for ad
    private int mPolyCounter;                            // Counts how many polyline there are
    private int mRefreshrate;                            // Millisecond between each timer repeat of updating ISS's location
    private int mSuccess;                                // Tracks # times server failed to respond
    private int mPoly;                                   // Used for adding on to a timestamp and getting future locations

    private boolean mThreadManager;                      // Checks if polyLines are already being created.
    private boolean mFirstTime;                          // Menu Tutorial
    private boolean mStart;                              // Opened app or returned to activity?
    private boolean mOnce;                               // Move map to ISS's location on start

    /**
     * When the application begins try to read from SharedPreferences to get ready. Run first
     * time setups if needed.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        initializeVariables();
        initiateBoomMenu();
        initializeAds();
    }

    /**
     * Initiate all variables when the activity is started for the first time
     */
    private void initializeVariables() {
        MapsInitializer.initialize(getApplicationContext());

        // Title on the action bar next to the BoomMenu drawer which we will initialize later
        ActionBar mActionBar = getSupportActionBar();
        assert mActionBar != null;
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(false);
        LayoutInflater mInflater = LayoutInflater.from(this);
        View actionBar = mInflater.inflate(R.layout.custom_actionbar, null);
        final TextView mTitleTextView = (TextView) actionBar.findViewById(R.id.title_text);
        mTitleTextView.setText(R.string.map_activity);
        mActionBar.setCustomView(actionBar);
        mActionBar.setDisplayShowCustomEnabled(true);
        ((Toolbar) actionBar.getParent()).setContentInsetsAbsolute(0, 0);

        // Other variables to make the activity start and function
        mPoly = 0;
        mOnce = true;
        mAdView = null;
        mStart = false;
        mPolyCounter = 0;
        mPolyArray = new Polyline[200];
        mContext = getApplicationContext();
        mRequestQueue = Volley.newRequestQueue(this);
        mLatLong = ((TextView) findViewById(R.id.textView));
        mDescription = (TextView) findViewById(R.id.textView2);
        PreferenceManager.setDefaultValues(this, R.xml.app_preferences, false);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mRefreshrate = 1000 * mSharedPreferences.getInt("refresh_Rate", 10);
        mFirstTime = mSharedPreferences.getBoolean(getString(R.string.firstTime), true);

        // Animation process for first time users. Skip if not first time (mFirstTime)
        final Activity activity = this;
        //  When view is shown, start our animations for first time users
        mLatLong.post(new Runnable() {
            @Override
            public void run() {
                if (mFirstTime) {
                    startAnimation(mTitleTextView, activity);
                }
            }
        });
    }

    /**
     * Initialize ads when the activity is started for the first time
     */
    private void initializeAds() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        MobileAds.initialize(mContext, getString(R.string.app_ID_Main));
        // Initiate the interstitial ad and onAdClosed listener
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
                switch (mInterstitialAdActivity) {
                    case 0:
                        startActivity(new Intent(mContext, Locations.class));
                        break;
                    case 1:
                        startActivity(new Intent(mContext, PeopleinSpace.class));
                        break;
                }
            }
        });

        if (!mSharedPreferences.getBoolean("advertisement", false)) {
            mAdView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().addTestDevice(getString(R.string.test_device)).build();
            mAdView.loadAd(adRequest);
        } else if (mAdView == null) {
            findViewById(R.id.adView).setVisibility(View.GONE);
        }
    }

    /**
     * Reread the refreshrate and update views if needed such as prediction line, texts and their
     * properties (Color, size, etc.).
     */
    protected void onResume() {
        super.onResume();

        // Asks for a interstitial ad now so next time user starts a new activity we have it ready
        requestNewInterstitial();

        // Update decimal format
        String format = "0";
        for (int i = 0; i < mSharedPreferences.getInt("decimalType", 3); ++i) {
            if (i == 0) format += ".";
            format += "#";
        }
        mDecimalFormat = new DecimalFormat(format);

        // When activity was just paused
        if (mStart) {
            mRefreshrate = 1000 * (mSharedPreferences.getInt("refresh_Rate", 9) + 1);
            if (mTimer != null) {
                mTimer.cancel();
                mTimer.purge();
                mTimer = null;
            }
            mTimer = new Timer();                // Track ISS based on refreshrate
            mTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    trackISS();
                }
            }, 0, mRefreshrate);

            mMaptype();
            // When activity is killed or created for first time
        } else {
            mStart = true;
            mTimer = new Timer();
        }

        // Provide optional advertisements that is visible/hidden by a checkbox in Preferences
        if (mSharedPreferences.getBoolean("advertisement", false) && mAdView != null) {
            mAdView.setVisibility(View.GONE);            // User disabled ads
        } else if (!mSharedPreferences.getBoolean("advertisement", false)) {
            if (mAdView == null) {                       // User wants ads but instance is null
                mAdView = (AdView) findViewById(R.id.adView);
                AdRequest adRequest = new AdRequest.Builder().addTestDevice(getString(R.string.test_device)).build();
                mAdView.loadAd(adRequest);
            } else {                                    // User wants ads, instance already got one
                mAdView.setVisibility(View.VISIBLE);
            }
        }

        // Update the color and size of polylines if they are different in settings than what they are right now
        int currentColor = Color.YELLOW;
        int currentWidth = 5;

        try {
            currentColor = mSharedPreferences.getInt("colorType", Color.YELLOW);
            currentWidth = mSharedPreferences.getInt("sizeType", 5);
        } catch (ClassCastException e) {
            Toast.makeText(mContext, "App data is corrupted. Resetting...", Toast.LENGTH_LONG).show();
            mSharedPreferences.edit().clear().apply();
        }

        if (mPolyLine != null) {
            if (mMap != null) {
                // Color needs updating?
                if (mPolyLine.getColor() != currentColor) {
                    for (int i = 0; mPolyArray[i] != null && i < mPolyArray.length - 1; ++i) {
                        mPolyArray[i].setColor(currentColor);
                    }
                }
                // What about their size?
                if (mPolyLine.getWidth() != currentWidth) {
                    for (int i = 0; mPolyArray[i] != null && i < mPolyArray.length - 1; ++i) {
                        mPolyArray[i].setWidth(currentWidth);
                    }
                }
            } else {
                Toast.makeText(mContext, R.string.errorMap, Toast.LENGTH_SHORT).show();
            }
        }

        // If user even wants the additional info, and their properties have been changed, update them
        if (mDescription.getVisibility() == View.VISIBLE && !mSharedPreferences.getBoolean("info_ISS", true)) {
            mDescription.setVisibility(View.GONE);
        } else if (mDescription.getVisibility() == View.GONE && mSharedPreferences.getBoolean("info_ISS", true)) {
            mDescription.setVisibility(View.VISIBLE);
        }

        // Update text color if different than settings
        int textColor = mSharedPreferences.getInt("colorText", Color.YELLOW);
        if (mLatLong.getCurrentTextColor() != textColor) {
            mLatLong.setTextColor(textColor);
            mDescription.setTextColor(textColor);
        }

        // Update text outline color if different
        int textOutlineColor = mSharedPreferences.getInt("colorHighlightText", Color.BLACK);
        if (mLatLong.getShadowColor() != mSharedPreferences.getInt("colorHighlightText", Color.BLACK)) {
            mLatLong.setShadowLayer(6, 0, 0, textOutlineColor);
            mDescription.setShadowLayer(6, 0, 0, textOutlineColor);
        }

        // Update text size as well if different
        int textSize = mSharedPreferences.getInt("textSizeType", 12);
        if (mLatLong.getTextSize() != textSize) {
            mLatLong.setTextSize(textSize);
            mDescription.setTextSize(textSize);
        }

        if (mAdView != null) {
            mAdView.resume();
        }
        mSuccess = 0;
    }

    /**
     * Cancel any request on Volley after user goes to another activity.
     */
    protected void onPause() {
        super.onPause();
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(TAG);
        }

        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
        }

        if (mAdView != null) {
            mAdView.pause();
        }
        mTimer = null;
    }

    /**
     * Destroy additional members when activity is gone.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAdView != null) {
            mAdView.destroy();
        }

        if (mPolyTimer != null) {
            mPolyTimer.cancel();
            mPolyTimer.purge();
        }
        mPolyTimer = null;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMaptype();

        if (mTimer == null) {
            mTimer = new Timer();
        }

        // Every 90 minutes, update the polylines automatically
        if (mPolyTimer == null) {
            mPolyTimer = new Timer();
            TimerTask hourlyTask = new TimerTask() {
                @Override
                public void run() {
                    asyncTaskPolyline();
                }
            };
            mPolyTimer.schedule(hourlyTask, 0L, 5400000); // 90 minutes
        }

        mMarkerOptions = new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.iss_2011));
        mMarkerOptions.anchor(0.5f, 0.5f);

        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                trackISS();
            }
        }, 0, mRefreshrate);
    }

    /**
     * Background call for updatePolyline
     */
    private void asyncTaskPolyline() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMap.clear();
                trackISS();
                mPoly = 0;
                mPolyCounter = 0;
            }
        });

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mThreadManager = true;
                    Date currentDate = new Date();
                    for (int i = 0; i < 20; ++i) {
                        updatePolyline(currentDate);
                        Thread.sleep(1000);
                    }
                    mThreadManager = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Changes the map type to match the one from settings. If it's the same just return.
     */
    private void mMaptype() {
        int current = Integer.parseInt(mSharedPreferences.getString("mapType", "2"));

        if (mMap != null) {
            if (mMap.getMapType() == current) {
                return;
            }

            mMap.setMapType(current);
        }
    }

    /**
     * Get the Lat and Lon of ISS and move the map to that position when called.
     */
    private void trackISS() {
        final String url = "https://api.wheretheiss.at/v1/satellites/25544";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    mSuccess = 0;        // Server responded successfully
                    final double latParameter = Double.parseDouble(response.getString("latitude"));
                    final double lngParameter = Double.parseDouble(response.getString("longitude"));
                    final LatLng ISS = new LatLng(latParameter, lngParameter);
                    final String LAT;
                    final String LNG;

                    if (latParameter < 0) {
                        LAT = mDecimalFormat.format(latParameter) + "° S";
                    } else {
                        LAT = mDecimalFormat.format(latParameter) + "° N";
                    }

                    if (lngParameter < 0) {
                        LNG = mDecimalFormat.format(lngParameter) + "° W";
                    } else {
                        LNG = mDecimalFormat.format(lngParameter) + "° E";
                    }

                    final String position = LAT + ", " + LNG;
                    final String visibility = response.getString("visibility");

                    final StringBuilder moreInfo = new StringBuilder();
                    moreInfo.append("Altitude: ").append(mDecimalFormat.format(Double.parseDouble(response.getString("altitude")))).append(" km").append("\n")
                            .append("Velocity: ").append(mDecimalFormat.format(Double.parseDouble(response.getString("velocity")))).append(" kph").append("\n")
                            .append("Footprint: ").append(mDecimalFormat.format(Double.parseDouble(response.getString("footprint")))).append(" km").append("\n")
                            .append("Solar LAT: ").append(mDecimalFormat.format(Double.parseDouble(response.getString("solar_lat")))).append("°").append("\n")
                            .append("Solar LON: ").append(mDecimalFormat.format(Double.parseDouble(response.getString("solar_lon")))).append("°").append("\n")
                            .append("Visibility: ").append(visibility.substring(0, 1).toUpperCase()).append(visibility.substring(1)).append("\n");

                    MapsActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            if (mOnce) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(ISS));
                                if (mMap.getUiSettings().isScrollGesturesEnabled()) {
                                    mMap.getUiSettings().setScrollGesturesEnabled(false);
                                }
                                mOnce = false;
                            } else if (mSharedPreferences.getBoolean("lock_ISS", false)) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(ISS));
                                if (mMap.getUiSettings().isScrollGesturesEnabled()) {
                                    mMap.getUiSettings().setScrollGesturesEnabled(false);
                                }
                            } else if (!mSharedPreferences.getBoolean("lock_ISS", false)) {
                                mMap.getUiSettings().setScrollGesturesEnabled(true);
                            }
                            if (mMarker != null) {
                                mMarker.remove();
                            }

                            mDescription.setText(moreInfo.toString());
                            mLatLong.setText(position);
                            mMarkerOptions.position(ISS);
                            mMarker = mMap.addMarker(mMarkerOptions);
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                // Server did not respond. Got 5 chances before stop trying.
                if (++mSuccess <= 4) {
                    Toast.makeText(MapsActivity.this, getResources().getString(R.string.errorLessFive, mSuccess, mRefreshrate), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MapsActivity.this, R.string.errorFiveTimes, Toast.LENGTH_LONG).show();
                    if (mTimer != null) {
                        mTimer.cancel();
                        mTimer.purge();
                    }
                    mTimer = null;
                }
            }
        });
        jsonObjectRequest.setTag(TAG);
        mRequestQueue.add(jsonObjectRequest);
    }

    /**
     * Update polyline sets the future predictions of ISS's position for up to 90 minutes.
     */
    private void updatePolyline(Date currentDate) {
        long currentLong = currentDate.getTime() / 1000;
        final long[] futureTen = new long[10];

        for (int i = 0; i < futureTen.length; ++i) {
            futureTen[i] = currentLong + (30 * mPoly++);
        }

        final StringBuilder urlBuilder = new StringBuilder();
        for (long aFutureTen : futureTen) {
            urlBuilder.append(aFutureTen).append(",");
        }
        urlBuilder.setLength(urlBuilder.length() - 1);

        final String units = "miles";
        final String url = "https://api.wheretheiss.at/v1/satellites/25544/positions?timestamps=" +
                urlBuilder.toString() +
                "&units=" +
                units; //TODO: maybe user decides on unit?

        final int finalStart = mPoly;
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(url, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    final LatLng[] latLngs = new LatLng[10];

                    for (int i = 0; i < response.length(); ++i) {
                        latLngs[i] = new LatLng(response.getJSONObject(i).getDouble("latitude"),
                                response.getJSONObject(i).getDouble("longitude"));
                    }

                    MapsActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            if (finalStart == 10) {
                                for (int i = 0; i < futureTen.length - 1; ++i) {
                                    mPolyLine = mMap.addPolyline(new PolylineOptions()
                                            .add(latLngs[i], latLngs[i + 1])
                                            .width(mSharedPreferences.getInt("sizeType", 5))
                                            .color(mSharedPreferences.getInt("colorType", Color.YELLOW)));
                                    mPolyArray[mPolyCounter++] = mPolyLine;
                                }
                                mLast = latLngs[latLngs.length - 1];
                            } else {
                                mPolyArray[mPolyCounter++] = mMap.addPolyline(new PolylineOptions()
                                        .add(mLast, latLngs[0])
                                        .width(mSharedPreferences.getInt("sizeType", 5))
                                        .color(mSharedPreferences.getInt("colorType", Color.YELLOW)));
                                for (int i = 0; i < futureTen.length - 1; ++i) {
                                    mPolyArray[mPolyCounter++] = mMap.addPolyline(new PolylineOptions()
                                            .add(latLngs[i], latLngs[i + 1])
                                            .width(mSharedPreferences.getInt("sizeType", 5))
                                            .color(mSharedPreferences.getInt("colorType", Color.YELLOW)));
                                }
                                mLast = latLngs[latLngs.length - 1];
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

            }
        });
        mRequestQueue.add(jsonArrayRequest);
    }

    /**
     * Initiate the drawer library by giving each of them their properties such as color, texts, and
     * onClickListeners
     */
    private void initiateBoomMenu() {
        mBoomMenu = (BoomMenuButton) findViewById(R.id.action_bar_right_bmb);
        assert mBoomMenu != null;
        mBoomMenu.setButtonEnum(ButtonEnum.Ham);
        mBoomMenu.setPiecePlaceEnum(PiecePlaceEnum.HAM_4);
        mBoomMenu.setButtonPlaceEnum(ButtonPlaceEnum.HAM_4);

        for (int i = 0; i < mBoomMenu.getPiecePlaceEnum().pieceNumber(); i++) {
            HamButton.Builder builder;
            switch (i) {
                case 0:
                    builder = new HamButton.Builder()
                            .listener(new OnBMClickListener() {
                                @Override
                                public void onBoomButtonClick(int index) {
                                    Intent intent;
                                    if (isLocationPermissionGranted()) {
                                        getLocationPermission();
                                    } else {
                                        if (mInterstitialAd.isLoaded() && !mSharedPreferences.getBoolean("fullPage", false)) {
                                            mInterstitialAd.show();
                                            mInterstitialAdActivity = 0;
                                        } else {
                                            intent = new Intent(mContext, Locations.class);
                                            startActivity(intent);
                                        }
                                    }
                                }
                            })
                            .normalImageRes(R.drawable.iss)
                            .normalTextRes(R.string.flybys_title)
                            .subNormalTextRes(R.string.flybys_summary)
                            .normalColor(Color.parseColor("#807E57C2"))
                            .highlightedColor(Color.parseColor("#807E57C2"))
                            .unableColor(Color.parseColor("#807E57C2"))
                            .rotateImage(false);
                    break;
                case 1:
                    builder = new HamButton.Builder()
                            .listener(new OnBMClickListener() {
                                @Override
                                public void onBoomButtonClick(int index) {
                                    Intent intent;
                                    if (mInterstitialAd.isLoaded() && !mSharedPreferences.getBoolean("fullPage", false)) {
                                        mInterstitialAd.show();
                                        mInterstitialAdActivity = 1;
                                    } else {
                                        intent = new Intent(mContext, PeopleinSpace.class);
                                        startActivity(intent);
                                    }
                                }
                            })
                            .normalImageRes(R.drawable.astronaut)
                            .normalTextRes(R.string.space_title)
                            .subNormalTextRes(R.string.space_summary)
                            .normalColor(Color.parseColor("#80EF5350"))
                            .highlightedColor(Color.parseColor("#80EF5350"))
                            .unableColor(Color.parseColor("#80EF5350"))
                            .rotateImage(false);
                    break;
                case 2:
                    builder = new HamButton.Builder()
                            .listener(new OnBMClickListener() {
                                @Override
                                public void onBoomButtonClick(int index) {
                                    Intent intent;
                                    /* Update the check box representing the app's service. If for example the service
                                     exited not by the app, it shouldn't be checked. */
                                    boolean Alert = isMyServiceRunning(com.restart.spacestationtracker.services.Alert.class);

                                    if (Alert != mSharedPreferences.getBoolean("notification_ISS", false)) {
                                        Toast.makeText(MapsActivity.this, R.string.errorNotByMe, Toast.LENGTH_LONG).show();
                                        mSharedPreferences.edit().putBoolean("notification_ISS", Alert).apply();
                                    }

                                    intent = new Intent(mContext, Preferences.class);
                                    startActivity(intent);
                                }
                            })
                            .normalImageRes(R.drawable.ic_settings)
                            .normalTextRes(R.string.settings_title)
                            .subNormalTextRes(R.string.settings_summary)
                            .normalColor(Color.parseColor("#8066BB6A"))
                            .highlightedColor(Color.parseColor("#8066BB6A"))
                            .unableColor(Color.parseColor("#8066BB6A"))
                            .rotateImage(false);
                    break;
                case 3:
                    builder = new HamButton.Builder()
                            .listener(new OnBMClickListener() {
                                @Override
                                public void onBoomButtonClick(int index) {
                                    Intent intent;
                                    intent = new Intent(mContext, Help.class);
                                    startActivity(intent);
                                }
                            })
                            .normalImageRes(R.drawable.ic_help_outline)
                            .normalTextRes(R.string.help_title)
                            .subNormalTextRes(R.string.help_summary)
                            .normalColor(Color.parseColor("#8029B6F6"))
                            .highlightedColor(Color.parseColor("#8029B6F6"))
                            .unableColor(Color.parseColor("#8029B6F6"))
                            .rotateImage(false);
                    break;
                default:
                    return;
            }
            mBoomMenu.addBuilder(builder);
        }
    }

    /**
     * Check to see if user has given us the permission to access their location.
     *
     * @return True or false
     */
    private boolean isLocationPermissionGranted() {
        return Build.VERSION.SDK_INT >= 23 && mContext.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED;
    }

    /**
     * Get the permissions needed for the Locations.class
     */
    private void getLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }

    /**
     * If permission was granted, send the user to the new activity.
     *
     * @param requestCode  For managing requests, in this case it's just 1
     * @param permissions  Would be nice to get internet and location
     * @param grantResults The ACCESS_FINE_LOCATION must be granted
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (mInterstitialAd.isLoaded() && !mSharedPreferences.getBoolean("fullPage", false)) {
                mInterstitialAd.show();
                mInterstitialAdActivity = 0;
            } else {
                startActivity(new Intent(mContext, Locations.class));
            }
        }
    }

    /**
     * Checks to see if a service is running on the phone and to update the check points
     * on Settings based on its return.
     *
     * @param serviceClass Class service searching for
     * @return Returns if user's phone is running the given service.
     */
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Starts a two step animation process. One gets executed after another.
     *
     * @param mTitleTextView A view to the top text widget
     * @param activity       MapsActivity.java
     */
    private void startAnimation(final View mTitleTextView, final Activity activity) {
        mSharedPreferences.edit().putBoolean(getString(R.string.firstTime), false).apply();
        mFirstTime = false;
        new SpotlightView.Builder(activity)
                .introAnimationDuration(400)
                .enableRevalAnimation(true)
                .performClick(true)
                .fadeinTextDuration(400)
                .headingTvColor(Color.parseColor("#6441A5"))
                .headingTvSize(32)
                .headingTvText("Hey There :)")
                .subHeadingTvColor(Color.parseColor("#ffffff"))
                .subHeadingTvSize(16)
                .subHeadingTvText(getString(R.string.tutorialOne))
                .maskColor(Color.parseColor("#dc000000"))
                .target(mTitleTextView)
                .lineAnimDuration(400)
                .lineAndArcColor(Color.parseColor("#6441A5"))
                .dismissOnTouch(true)
                .usageId("1")
                .setListener(new SpotlightListener() {
                    @Override
                    public void onUserClicked(String s) {
                        new SpotlightView.Builder(activity)
                                .introAnimationDuration(400)
                                .enableRevalAnimation(true)
                                .performClick(true)
                                .fadeinTextDuration(400)
                                .headingTvColor(Color.parseColor("#6441A5"))
                                .headingTvSize(32)
                                .headingTvText("Main Features")
                                .subHeadingTvColor(Color.parseColor("#ffffff"))
                                .subHeadingTvSize(16)
                                .subHeadingTvText(getString(R.string.tutorialTwo))
                                .maskColor(Color.parseColor("#dc000000"))
                                .target(mBoomMenu)
                                .lineAnimDuration(400)
                                .lineAndArcColor(Color.parseColor("#6441A5"))
                                .dismissOnTouch(true)
                                .usageId("2").show();
                    }
                }).show();
    }

    /**
     * Creates switch case for action bar icons
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reset:
                if (!mThreadManager) {
                    asyncTaskPolyline();
                } else {
                    Toast.makeText(mContext, R.string.errorWaitPoly, Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.stream:
                onISS();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Creates the icons in the action bar
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    /**
     * Start the LiveStream activity
     */
    private void onISS() {
        Intent intent = new Intent(mContext, LiveStream.class);
        startActivity(intent);
    }

    /**
     * Request for a new interstitial ad
     */
    private void requestNewInterstitial() {
        if (!mSharedPreferences.getBoolean("fullPage", false)) {
            AdRequest adRequest = new AdRequest.Builder()
                    //.addTestDevice(getString(R.string.test_device))
                    .build();

            mInterstitialAd.loadAd(adRequest);
        }
    }

    @Override
    public void onClick(View v) {
    }
}
