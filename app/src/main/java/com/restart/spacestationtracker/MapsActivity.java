package com.restart.spacestationtracker;

import static com.restart.spacestationtracker.util.IsRunningTestKt.isRunningTest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
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
import com.google.firebase.crash.FirebaseCrash;
import com.nightonke.boommenu.BoomButtons.ButtonPlaceEnum;
import com.nightonke.boommenu.BoomButtons.HamButton;
import com.nightonke.boommenu.BoomMenuButton;
import com.nightonke.boommenu.ButtonEnum;
import com.nightonke.boommenu.Piece.PiecePlaceEnum;
import com.wooplr.spotlight.SpotlightView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Contains the google map and uses volley to grab JSON objects and display
 * the position of the ISS as a customized marker on the map and update it occasionally.
 */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {

    public static final String CHANNEL_ID = "iss_notification"; // Notification for Android 8.0 and higher
    private static final String TAG = ".MapsActivity";  // Used for volley and occasional Log

    private SharedPreferences mSharedPreferences;       // Managing options from Settings
    private BoomMenuButton mBoomMenu;                   // Manages the drawer pop menu
    private InterstitialAd mInterstitialAd;             // Managing interstitial ads with AdMob sdk
    private MarkerOptions mMarkerOptions;               // Marker options, uses ISS drawable
    private DecimalFormat mDecimalFormat;               // For number decimal places
    private RequestQueue mRequestQueue;                 // Volley for JSONObject/JSONArray requests
    private GoogleMap mMap;                             // Google maps
    private Polyline[] mPolyArray;                      // An array of polyline. Need 200 for a nice curve
    private TextView mDescription;                      // A description of additional ISS info. Optional
    private TextView mLatLong;                          // Lat & Lon of ISS
    private Polyline mPolyLine;                         // A single poly to help compare any changes from settings
    private Context mContext;                           // Application context
    private Marker mMarker;                             // Marker representing ISS that moves alone the prediction line
    private AdView mAdView;                             // Optional ads
    private LatLng mLast;                               // Used for connecting polyline.
    private Timer mPolyTimer;                           // Updates the poly lines
    private Timer mTimer;                               // Updates map based on refresh rate

    private int mInterstitialAdActivity;                // Keeps tracks of what activity was requested to open but was paused for ad
    private int mPolyCounter;                           // Counts how many polyline there are
    private int mCurrentColor;                          // Holds the current colors of polylines
    private int mCurrentWidth;                          // Holds the current width of polylines
    private int mRefreshRate;                           // Millisecond between each timer repeat of updating ISS's location
    private int mSuccess;                               // Tracks # times server failed to respond
    private int mPoly;                                  // Used for adding on to a timestamp and getting future locations
    private int mProgress;                              // Progress on polylines, make sure we finish one before moving on to next

    private boolean mThreadManager;                     // Checks if polyLines are already being created.
    private boolean mFirstTime;                         // Menu Tutorial
    private boolean mStart;                             // Opened app or returned to activity?
    private boolean mOnce;                              // Move map to ISS's location on start

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
        //initializeNotificationChannel();
    }

    /**
     * Create the NotificationChannel, but only on API 26+ because the NotificationChannel class
     * is new and not in the support library
     * <p>
     * Removed until push notification is back on
     */
    private void initializeNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.notificationTitle);
            String description = getString(R.string.notificationSummary);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null)
                notificationManager.createNotificationChannel(channel);
        }
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
        @SuppressLint("InflateParams") View actionBar = mInflater.inflate(R.layout.custom_actionbar, null);
        final TextView mTitleTextView = actionBar.findViewById(R.id.title_text);
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
        mLatLong = findViewById(R.id.textView);
        mDescription = findViewById(R.id.textView2);
        PreferenceManager.setDefaultValues(this, R.xml.app_preferences, false);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mRefreshRate = 1000 * (mSharedPreferences.getInt("refresh_Rate", 9) + 1);
        mFirstTime = mSharedPreferences.getBoolean(getString(R.string.firstTime), true);

        // Animation process for first time users. Skip if not first time (mFirstTime)
        final Activity activity = this;
        //  When view is shown, start our animations for first time users
        mLatLong.post(() -> {
            if (mFirstTime) {
                startAnimation(mTitleTextView, activity);
            }
        });
    }

    /**
     * Initialize ads when the activity is started for the first time
     */
    private void initializeAds() {
        List<String> testDevices = new ArrayList<>();
        testDevices.add(getString(R.string.test_device));
        MobileAds.setRequestConfiguration(new RequestConfiguration.Builder()
                .setTestDeviceIds(testDevices)
                .build());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        MobileAds.initialize(this, initializationStatus -> {
        });

        // Initiate the interstitial ad and onAdClosed listener
        InterstitialAd.load(this, getString(R.string.interstitial_ad_unit_id), new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                requestNewInterstitial();
                                switch (mInterstitialAdActivity) {
                                    case 0:
                                        startActivity(new Intent(mContext, Locations.class));
                                        break;
                                    case 1:
                                        startActivity(new Intent(mContext, PeopleInSpace.class));
                                        break;
                                }
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                mInterstitialAd = null;
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mInterstitialAd = null;
                    }
                });

        mAdView = findViewById(R.id.adView);
        mAdView.loadAd(new AdRequest.Builder().build());
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
        StringBuilder format = new StringBuilder("0");
        for (int i = 0; i < mSharedPreferences.getInt("decimalType", 3); ++i) {
            if (i == 0) format.append(".");
            format.append("#");
        }
        mDecimalFormat = new DecimalFormat(format.toString());

        // When activity was just paused
        if (mStart) {
            mRefreshRate = 1000 * (mSharedPreferences.getInt("refresh_Rate", 9) + 1);
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
            }, 0, mRefreshRate);

            mMaptype();
            // When activity is killed or created for first time
        } else {
            mStart = true;
            mTimer = new Timer();
        }

        mAdView = findViewById(R.id.adView);
        mAdView.loadAd(new AdRequest.Builder().build());

        // Update the color and size of polylines if they are different in settings than what they are right now
        mCurrentColor = Color.YELLOW;
        mCurrentWidth = 5;

        try {
            mCurrentColor = mSharedPreferences.getInt("colorType", Color.YELLOW);
            mCurrentWidth = mSharedPreferences.getInt("sizeType", 5);
        } catch (ClassCastException e) {
            Toast.makeText(mContext, R.string.data_corrupted, Toast.LENGTH_LONG).show();
            mSharedPreferences.edit().clear().apply();
        }

        if (mPolyLine != null) {
            if (mMap != null) {
                // Color needs updating?
                if (mPolyLine.getColor() != mCurrentColor) {
                    for (int i = 0; mPolyArray[i] != null && i < mPolyArray.length - 1; ++i) {
                        mPolyArray[i].setColor(mCurrentColor);
                    }
                }
                // What about their size?
                if (mPolyLine.getWidth() != mCurrentWidth) {
                    for (int i = 0; mPolyArray[i] != null && i < mPolyArray.length - 1; ++i) {
                        mPolyArray[i].setWidth(mCurrentWidth);
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
    public void onMapReady(@NonNull GoogleMap googleMap) {
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
        }, 0, mRefreshRate);
    }

    /**
     * Background call for updatePolyline
     */
    private void asyncTaskPolyline() {
        runOnUiThread(() -> {
            mMap.clear();
            trackISS();
            mPoly = 0;
            mPolyCounter = 0;
        });

        AsyncTask.execute(() -> {
            try {
                mThreadManager = true;
                Date currentDate = new Date();
                mProgress = 0;
                for (int i = 0; i < 20; ++i) {
                    int mLastPatience = 0;
                    while (i > 0 && mLastPatience < 10 && mProgress < i) {
                        Thread.sleep(300);
                        ++mLastPatience;
                    }

                    if (mLastPatience >= 10) {
                        runOnUiThread(() -> Toast.makeText(mContext, R.string.polyError, Toast.LENGTH_SHORT).show());
                        break;
                    }

                    updatePolyline(currentDate);
                    Thread.sleep(1500);
                }
                mThreadManager = false;
            } catch (Exception e) {
                e.printStackTrace();
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
                null, response -> {
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

                MapsActivity.this.runOnUiThread(() -> {
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
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, e -> {
            // Server did not respond. Got 5 chances before stop trying.
            if (++mSuccess <= 4) {
                Toast.makeText(MapsActivity.this, getResources().getString(R.string.errorLessFive, mSuccess, mRefreshRate), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MapsActivity.this, R.string.errorFiveTimes, Toast.LENGTH_LONG).show();
                if (mTimer != null) {
                    mTimer.cancel();
                    mTimer.purge();
                }
                mTimer = null;
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
            futureTen[i] = currentLong + (30L * mPoly++);
        }

        final StringBuilder urlBuilder = new StringBuilder();
        for (long aFutureTen : futureTen) {
            urlBuilder.append(aFutureTen).append(",");
        }
        urlBuilder.setLength(urlBuilder.length() - 1);

        //TODO: As a user, I would like the option of changing the units from metric to imperial
        final String units = "miles";
        final String url = "https://api.wheretheiss.at/v1/satellites/25544/positions?timestamps=" +
                urlBuilder +
                "&units=" +
                units;

        final int finalStart = mPoly;
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(url, response -> {
            try {
                final LatLng[] latLngs = new LatLng[10];

                for (int i = 0; i < response.length(); ++i) {
                    latLngs[i] = new LatLng(response.getJSONObject(i).getDouble("latitude"),
                            response.getJSONObject(i).getDouble("longitude"));
                }

                MapsActivity.this.runOnUiThread(() -> {
                    if (finalStart == 10) {
                        for (int i = 0; i < futureTen.length - 1; ++i) {
                            mPolyLine = mMap.addPolyline(new PolylineOptions()
                                    .add(latLngs[i], latLngs[i + 1])
                                    .width(mCurrentWidth)
                                    .color(mCurrentColor));
                            mPolyArray[mPolyCounter++] = mPolyLine;
                        }
                    } else {
                        mPolyArray[mPolyCounter++] = mMap.addPolyline(new PolylineOptions()
                                .add(mLast, latLngs[0])
                                .width(mCurrentWidth)
                                .color(mCurrentColor));
                        for (int i = 0; i < futureTen.length - 1; ++i) {
                            mPolyArray[mPolyCounter++] = mMap.addPolyline(new PolylineOptions()
                                    .add(latLngs[i], latLngs[i + 1])
                                    .width(mCurrentWidth)
                                    .color(mCurrentColor));
                        }
                    }
                    mLast = latLngs[latLngs.length - 1];
                    ++mProgress;
                });
            } catch (Exception e) {
                if (mLast == null) {
                    FirebaseCrash.report(new Exception("mLast was null"));
                }
                e.printStackTrace();
            }
        }, e -> {

        });
        mRequestQueue.add(jsonArrayRequest);
    }

    /**
     * Initiate the drawer library by giving each of them their properties such as color, texts, and
     * onClickListeners
     */
    private void initiateBoomMenu() {
        mBoomMenu = findViewById(R.id.action_bar_right_bmb);
        mBoomMenu.setButtonEnum(ButtonEnum.Ham);
        mBoomMenu.setPiecePlaceEnum(PiecePlaceEnum.HAM_4);
        mBoomMenu.setButtonPlaceEnum(ButtonPlaceEnum.HAM_4);
        mBoomMenu.setDuration(850);

        for (int i = 0; i < mBoomMenu.getPiecePlaceEnum().pieceNumber(); i++) {
            HamButton.Builder builder;
            switch (i) {
                case 0:
                    builder = new HamButton.Builder()
                            .listener(index -> {
                                Intent intent;
                                if (isLocationPermissionGranted()) {
                                    getLocationPermission();
                                } else {
                                    if (mInterstitialAd != null) {
                                        mInterstitialAd.show(MapsActivity.this);
                                        mInterstitialAdActivity = 0;
                                    } else {
                                        intent = new Intent(mContext, Locations.class);
                                        startActivity(intent);
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
                            .listener(index -> {
                                Intent intent;
                                if (mInterstitialAd != null) {
                                    mInterstitialAd.show(MapsActivity.this);
                                    mInterstitialAdActivity = 1;
                                } else {
                                    intent = new Intent(mContext, PeopleInSpace.class);
                                    startActivity(intent);
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
                            .listener(index -> {
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
                            .listener(index -> {
                                Intent intent;
                                intent = new Intent(mContext, AboutActivity.class);
                                startActivity(intent);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (mInterstitialAd != null && !mSharedPreferences.getBoolean("fullPage", false)) {
                mInterstitialAd.show(MapsActivity.this);
                mInterstitialAdActivity = 0;
            } else {
                startActivity(new Intent(mContext, Locations.class));
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
                .enableRevealAnimation(true)
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
                .setListener(s -> new SpotlightView.Builder(activity)
                        .introAnimationDuration(400)
                        .enableRevealAnimation(true)
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
                        .usageId("2").show()).show();
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
            // Initiate the interstitial ad and onAdClosed listener
            InterstitialAd.load(this, getString(R.string.interstitial_ad_unit_id), new AdRequest.Builder().build(),
                    new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                            mInterstitialAd = interstitialAd;
                            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                                @Override
                                public void onAdDismissedFullScreenContent() {
                                    requestNewInterstitial();
                                    switch (mInterstitialAdActivity) {
                                        case 0:
                                            startActivity(new Intent(mContext, Locations.class));
                                            break;
                                        case 1:
                                            startActivity(new Intent(mContext, PeopleInSpace.class));
                                            break;
                                    }
                                }

                                @Override
                                public void onAdShowedFullScreenContent() {
                                    mInterstitialAd = null;
                                }
                            });
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            mInterstitialAd = null;
                        }
                    });
        }
    }

    @Override
    public void onClick(View v) {
    }
}
