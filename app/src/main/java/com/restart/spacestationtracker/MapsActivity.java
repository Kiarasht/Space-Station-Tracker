package com.restart.spacestationtracker;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
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
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Contains the google map and uses volley to grab JSON objects and display
 * the position of the ISS as a customized marker on the map and update it occasionally.
 */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {

    public static final String CHANNEL_ID = "iss_notification";     // Notification for Android 8.0 and higher
    private static final String TAG = ".MapsActivity";              // Used for volley and occasional Log

    private final AtomicBoolean isMobileAdsInitializeCalled = new AtomicBoolean(false);
    private ConsentInformation consentInformation;      // Need privacy consent before showing ads
    private SharedPreferences mSharedPreferences;       // Managing options from Settings
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
    private boolean mStart;                             // Opened app or returned to activity?
    private boolean mOnce;                              // Move map to ISS's location on start

    /**
     * When the application begins try to read from SharedPreferences to get ready. Run first
     * time setups if needed.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        View mainLayout = findViewById(R.id.main);
        View adContainer = findViewById(R.id.ad_view_container);
        View topTextView = findViewById(R.id.textView);

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            topTextView.setPadding(
                    topTextView.getPaddingLeft(),
                    insets.top,
                    topTextView.getPaddingRight(),
                    topTextView.getPaddingBottom()
            );

            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) adContainer.getLayoutParams();

            lp.leftMargin = insets.left;
            lp.rightMargin = insets.right;
            lp.bottomMargin = insets.bottom;

            adContainer.setLayoutParams(lp);

            return WindowInsetsCompat.CONSUMED;
        });

        initializeVariables();
        initializeConsent();
        if (consentInformation.canRequestAds()) {
            initializeAds();
        }
    }

    private void initializeConsent() {
        ConsentRequestParameters params = new ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build();

        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        consentInformation.requestConsentInfoUpdate(
                this,
                params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                        this,
                        loadAndShowError -> {
                            if (consentInformation.canRequestAds()) {
                                initializeAds();
                            }
                        }
                ),
                requestConsentError -> {
                });

        if (consentInformation.canRequestAds()) {
            initializeAds();
        }
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

        findViewById(R.id.fab_flybys).setOnClickListener(this);
        findViewById(R.id.fab_on_duty).setOnClickListener(this);
        findViewById(R.id.fab_settings).setOnClickListener(this);
        findViewById(R.id.fab_help).setOnClickListener(this);
        findViewById(R.id.fab_live).setOnClickListener(this);
    }

    /**
     * Initialize ads when the activity is started for the first time
     */
    private void initializeAds() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return;
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        MobileAds.initialize(this, initializationStatus -> {
        });

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

        FrameLayout mAdViewContainer = findViewById(R.id.ad_view_container);
        mAdView = new AdView(this);
        mAdView.setAdUnitId(getString(R.string.banner_ad_unit_id));
        mAdViewContainer.addView(mAdView);

        mAdViewContainer.post(() -> {
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int adWidth = (int) (mAdViewContainer.getWidth() / displayMetrics.density);
            AdSize adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
            mAdView.setAdSize(adSize);

            AdManagerAdRequest adRequest = new AdManagerAdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        });
    }

    /**
     * Reread the refresh rate and update views if needed such as prediction line, texts and their
     * properties (Color, size, etc.).
     */
    protected void onResume() {
        super.onResume();

        requestNewInterstitial();

        StringBuilder format = new StringBuilder("0");
        for (int i = 0; i < mSharedPreferences.getInt("decimalType", 3); ++i) {
            if (i == 0) format.append(".");
            format.append("#");
        }
        mDecimalFormat = new DecimalFormat(format.toString());

        if (mStart) {
            mRefreshRate = 1000 * (mSharedPreferences.getInt("refresh_Rate", 9) + 1);
            if (mTimer != null) {
                mTimer.cancel();
                mTimer.purge();
                mTimer = null;
            }
            mTimer = new Timer();                // Track ISS based on refresh rate
            mTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    trackISS();
                }
            }, 0, mRefreshRate);

            mMapType();
            // When activity is killed or created for first time
        } else {
            mStart = true;
            mTimer = new Timer();
        }

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
        mMapType();

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
            try {
                mMap.clear();
                trackISS();
                mPoly = 0;
                mPolyCounter = 0;
            } catch (Exception e) {
                Toast.makeText(mContext, R.string.polyError, Toast.LENGTH_SHORT).show();
            }
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
    private void mMapType() {
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
                Toast.makeText(MapsActivity.this, getResources().getString(R.string.errorLessFive), Toast.LENGTH_SHORT).show();
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

        final String url = getString(futureTen);

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

            }
        }, e -> {

        });
        mRequestQueue.add(jsonArrayRequest);
    }

    @NonNull
    private static String getString(long[] futureTen) {
        final StringBuilder urlBuilder = new StringBuilder();
        for (long aFutureTen : futureTen) {
            urlBuilder.append(aFutureTen).append(",");
        }
        urlBuilder.setLength(urlBuilder.length() - 1);

        //TODO: As a user, I would like the option of changing the units from metric to imperial
        final String units = "miles";
        return "https://api.wheretheiss.at/v1/satellites/25544/positions?timestamps=" +
                urlBuilder +
                "&units=" +
                units;
    }

    /**
     * Check to see if user has given us the permission to access their location.
     *
     * @return True or false
     */
    private boolean isLocationPermissionGranted() {
        return mContext.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED;
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
            if (mInterstitialAd != null) {
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
     * @noinspection SameParameterValue notifications are disabled for now
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
     * Request for a new interstitial ad
     */
    private void requestNewInterstitial() {
        if (consentInformation == null || !consentInformation.canRequestAds()) {
            return;
        }

        // Initiate the interstitial ad and onAdClosed listener
        InterstitialAd.load(this, getString(R.string.interstitial_ad_unit_id), new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
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

    @Override
    public void onClick(View v) {
        Intent intent;
        final int id = v.getId();

        if (id == R.id.fab_flybys) {
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
        } else if (id == R.id.fab_on_duty) {
            if (mInterstitialAd != null) {
                mInterstitialAd.show(MapsActivity.this);
                mInterstitialAdActivity = 1;
            } else {
                intent = new Intent(mContext, PeopleInSpace.class);
                startActivity(intent);
            }
        } else if (id == R.id.fab_settings) {
            intent = new Intent(mContext, Preferences.class);
            startActivity(intent);
        } else if (id == R.id.fab_help) {
            intent = new Intent(mContext, AboutActivity.class);
            startActivity(intent);
        } else if (id == R.id.fab_live) {
            String videoId = "DIgkvm2nmHc";

            Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + videoId));
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + videoId));
            try {
                startActivity(appIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                startActivity(webIntent);
            }
        }
    }
}
