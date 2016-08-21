package com.restart.spacestationtracker;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.nightonke.boommenu.BoomMenuButton;
import com.nightonke.boommenu.Types.BoomType;
import com.nightonke.boommenu.Types.ButtonType;
import com.nightonke.boommenu.Types.PlaceType;
import com.nightonke.boommenu.Util;
import com.restart.spacestationtracker.services.Alert;
import com.restart.spacestationtracker.view.ViewDialog;
import com.wooplr.spotlight.SpotlightView;
import com.wooplr.spotlight.utils.SpotlightListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import hotchemi.android.rate.AppRate;
import hotchemi.android.rate.OnClickButtonListener;

/**
 * Contains the google map and uses volley to grab JSON objects and display
 * the position of the ISS picture on the map and update it occasionally.
 */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        BoomMenuButton.OnSubButtonClickListener,
        BoomMenuButton.AnimatorListener,
        View.OnClickListener {

    private final String TAG = ".MapsActivity";
    private BoomMenuButton boomMenuButtonInActionBar;
    private SharedPreferences sharedPreferences;        // Managing options from Settings
    private RequestQueue requestQueue;                  // Volley
    private TextView latLong;                           // latLong of ISS
    private Context context;
    private GoogleMap mMap;
    private AdView adView;
    private Timer timer;                                // Updates map based on refresh rate
    private int refreshrate;                            // Millisecond between each timer repeat
    private int success;                                // Tracks # times server failed to respond
    private boolean firstTime;                          // Menu Tutorial
    private boolean start = false;                      // Opened app or returned to activity?

    /**
     * When the application begins try to read from SharedPreferences to get ready. Run first
     * time setups if needed.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // TODO Remove this after checking for more leak
        //LeakCanary.install(getApplication());

        Log.e(TAG, "onCreate");

        ActionBar mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(false);
        LayoutInflater mInflater = LayoutInflater.from(this);

        View mCustomView = mInflater.inflate(R.layout.custom_actionbar, null);
        final TextView mTitleTextView = (TextView) mCustomView.findViewById(R.id.title_text);
        mTitleTextView.setText("Map");

        boomMenuButtonInActionBar = (BoomMenuButton) mCustomView.findViewById(R.id.boom);
        boomMenuButtonInActionBar.setOnSubButtonClickListener(this);
        boomMenuButtonInActionBar.setAnimatorListener(this);
        boomMenuButtonInActionBar.setDuration(700);
        boomMenuButtonInActionBar.setDelay(100);

        mActionBar.setCustomView(mCustomView);
        mActionBar.setDisplayShowCustomEnabled(true);
        ((Toolbar) mCustomView.getParent()).setContentInsetsAbsolute(0, 0);

        AppRate.with(this)
                .setInstallDays(10)
                .setLaunchTimes(10)
                .setRemindInterval(3)
                .setShowLaterButton(true)
                .setDebug(false)
                .setOnClickButtonListener(new OnClickButtonListener() {
                    @Override
                    public void onClickButton(int which) {
                        Log.d(MapsActivity.class.getName(), Integer.toString(which));
                    }
                })
                .monitor();

        AppRate.showRateDialogIfMeetsConditions(this);

        adView = null;
        context = getApplicationContext();
        requestQueue = Volley.newRequestQueue(this);
        latLong = ((TextView) findViewById(R.id.textView));
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        refreshrate = 1000 * sharedPreferences.getInt("refresh_Rate", 15);
        firstTime = sharedPreferences.getBoolean(getString(R.string.firstTime), true);

        final Activity activity = this;
        final View issPicture = findViewById(R.id.issPicture);

        //  When view is shown, start our animations for first time users
        issPicture.post(new Runnable() {
            @Override
            public void run() {
                if (firstTime) {
                    startAnimation(issPicture, mTitleTextView, activity);
                }
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (!sharedPreferences.getBoolean("advertisement", false)) {
            adView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().addTestDevice("998B51E0DA18B35E1A4C4E6D78084ABB").build();
            adView.loadAd(adRequest);
        } else if (adView == null) {
            findViewById(R.id.adView).setVisibility(View.GONE);
        }
    }

    /**
     * Reread the refreshrate and update layout if needed.
     */
    protected void onResume() {
        super.onResume();

        Log.e(TAG, "onResume");

        if (start) {                            // When activity was just paused
            refreshrate = 1000 * sharedPreferences.getInt("refresh_Rate", 15);
            if (timer != null) {
                timer.cancel();
                timer.purge();
                timer = null;
            }
            timer = new Timer();                // Track ISS based on refreshrate
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    trackISS();
                }
            }, 0, refreshrate);

            mMaptype();
        } else {                                // When activity is killed or created for first time
            start = true;
            timer = new Timer();
        }

        // Provide optional advertisements that is visible/hidden by a checkbox in Preferences
        if (sharedPreferences.getBoolean("advertisement", false) && adView != null) {
            adView.setVisibility(View.GONE);       // User disabled ads
        } else if (!sharedPreferences.getBoolean("advertisement", false)) {
            if (adView == null) {                       // User wants ads but instance is null
                adView = (AdView) findViewById(R.id.adView);
                AdRequest adRequest = new AdRequest.Builder().addTestDevice("998B51E0DA18B35E1A4C4E6D78084ABB").build();
                adView.loadAd(adRequest);
            } else {                                    // User wants ads, instance already got one
                adView.setVisibility(View.VISIBLE);
            }
        }

        if (adView != null) {
            adView.resume();
        }
        success = 0;
    }

    /**
     * Cancel any request on Volley after user goes to another activity.
     */
    protected void onPause() {
        super.onPause();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }

        if (timer != null) {
            timer.cancel();
            timer.purge();
        }

        if (adView != null) {
            adView.pause();
        }
        timer = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adView != null) {
            adView.destroy();
        }
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

        mMap.getUiSettings().setScrollGesturesEnabled(false);

        if (timer == null) {
            timer = new Timer();
        }

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                trackISS();
            }
        }, 0, refreshrate);

        updatePolyline();
    }

    /**
     * Changes the map type to match the one from settings. If it's the same just return.
     */
    private void mMaptype() {
        int current = Integer.parseInt(sharedPreferences.getString("mapType", "2"));

        if (mMap.getMapType() == current) {
            return;
        }

        switch (current) {
            case 0:
                mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
                break;
            case 1:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            case 2:
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            case 3:
                mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                break;
            case 4:
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
        }
    }

    /**
     * Starts a three step animation process. One gets executed after another.
     *
     * @param issPicture     A view to ISS icon
     * @param mTitleTextView A view to the top text widget
     * @param activity       MapsActivity.java
     */
    public void startAnimation(final View issPicture, final View mTitleTextView, final Activity activity) {
        sharedPreferences.edit().putBoolean(getString(R.string.firstTime), false).apply();
        firstTime = false;
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
                .subHeadingTvText("Map shows ISS's current location. You can only zoom in and out. Otherwise it follows ISS by itself.")
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
                                .subHeadingTvText("Drawer takes you to other features such as flybys, settings, etc...")
                                .maskColor(Color.parseColor("#dc000000"))
                                .target(boomMenuButtonInActionBar)
                                .lineAnimDuration(400)
                                .lineAndArcColor(Color.parseColor("#6441A5"))
                                .dismissOnTouch(true)
                                .usageId("2")
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
                                                .headingTvText("Live Stream")
                                                .subHeadingTvColor(Color.parseColor("#ffffff"))
                                                .subHeadingTvSize(16)
                                                .subHeadingTvText("Clicking the ISS icon will take you to a live stream." +
                                                        " Basically what astronauts see right now.")
                                                .maskColor(Color.parseColor("#dc000000"))
                                                .target(issPicture)
                                                .lineAnimDuration(400)
                                                .lineAndArcColor(Color.parseColor("#6441A5"))
                                                .dismissOnTouch(true)
                                                .usageId("3")
                                                .show();
                                    }
                                }).show();
                    }
                }).show();
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
                    success = 0;        // Server responded successfully
                    final double latParameter = Double.parseDouble(response.getString("latitude"));
                    final double lngParameter = Double.parseDouble(response.getString("longitude"));
                    final LatLng ISS = new LatLng(latParameter, lngParameter);
                    final DecimalFormat decimalFormat = new DecimalFormat("0.000");
                    final String LAT = decimalFormat.format(latParameter);
                    final String LNG = decimalFormat.format(lngParameter);

                    final String position = LAT + "° N, " + LNG + "° E";

                    MapsActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(ISS));
                            latLong.setText(position);
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
                if (++success <= 4) {
                    Toast.makeText(MapsActivity.this,
                            "Request to server failed. " + success + " out of 5. Trying in " +
                                    refreshrate / 1000 + " seconds.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MapsActivity.this,
                            "Failed all 5 times. Either you have no connection or server is overloaded.",
                            Toast.LENGTH_LONG).show();
                    if (timer != null) {
                        timer.cancel();
                        timer.purge();
                    }
                    timer = null;
                }
            }
        });
        jsonObjectRequest.setTag(TAG);
        requestQueue.add(jsonObjectRequest);
    }

    /**
     * Update polyline sets the future predictions of ISS's position for up to 90 minutes.
     */
    private void updatePolyline() {
        Date currentDate = new Date();
        final long[] futureTen = new long[10];

        for (int i = 0; i < futureTen.length; ++i) {
            futureTen[i] = (currentDate.getTime() / 1000) + (552 * i);
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

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(url, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    final LatLng[] latLngs = new LatLng[10];

                    for (int i = 0; i < response.length(); ++i) {
                        latLngs[i] = new LatLng(response.getJSONObject(i).getDouble("latitude"), response.getJSONObject(i).getDouble("longitude"));
                    }

                    MapsActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            for (int i = 0; i < futureTen.length - 1; ++i) {
                                mMap.addPolyline(new PolylineOptions()
                                        .add(latLngs[i], latLngs[i+1])
                                        .width(5)
                                        .color(Color.RED));
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
                Log.e(TAG, "Couldn't setup Poly");
            }
        });
        jsonArrayRequest.setTag(TAG);
        requestQueue.add(jsonArrayRequest);
    }

    /**
     * Method responsible of creating and designing the popup drawer. (BoomMenuButton)
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        final int number = 4;

        Drawable[] drawables = new Drawable[number];
        int[] drawablesResource = new int[]{
                R.drawable.iss,
                R.drawable.astronaut,
                R.drawable.ic_settings,
                R.drawable.ic_help_outline,
        };

        for (int i = 0; i < number; i++)
            drawables[i] = ContextCompat.getDrawable(this, drawablesResource[i]);

        String[] STRINGS = new String[]{
                "Flybys",
                "Who's in Space?",
                "Settings",
                "Help",
        };

        String[] strings = new String[number];
        System.arraycopy(STRINGS, 0, strings, 0, number);

        int[][] colors = new int[number][2];
        for (int i = 0; i < number; i++) {
            colors[i][1] = GetColor(i);
            colors[i][0] = Util.getInstance().getPressedColor(colors[i][1]);
        }

        boomMenuButtonInActionBar.init(
                drawables,          // The drawables of images of sub buttons. Can not be null.
                strings,            // The texts of sub buttons, ok to be null.
                colors,             // The colors of sub buttons, including pressed-state and normal-state.
                ButtonType.HAM,     // The button type.
                BoomType.LINE,      // The boom type.
                PlaceType.HAM_4_1,  // The place type.
                null,               // Ease type to move the sub buttons when showing.
                null,               // Ease type to scale the sub buttons when showing.
                null,               // Ease type to rotate the sub buttons when showing.
                null,               // Ease type to move the sub buttons when dismissing.
                null,               // Ease type to scale the sub buttons when dismissing.
                null,               // Ease type to rotate the sub buttons when dismissing.
                null                // Rotation degree.
        );
    }

    /**
     * Navigate out of boom menu first on back button
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (boomMenuButtonInActionBar.isOpen()) {
                        boomMenuButtonInActionBar.dismiss();
                    } else {
                        finish();
                    }

                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Manually picked colors that best matched the over theme of the application.
     *
     * @param iteration The i iteration of previous method.
     * @return Returns a color hex which also includes some transparency.
     */
    public int GetColor(int iteration) {
        int colors;
        switch (iteration) {
            case 0:
                colors = Color.parseColor("#807E57C2");
                break;
            case 1:
                colors = Color.parseColor("#80EF5350");
                break;
            case 2:
                colors = Color.parseColor("#8066BB6A");
                break;
            case 3:
                colors = Color.parseColor("#8029B6F6");
                break;
            default:
                colors = Color.parseColor("#805C6BC0");
                break;
        }
        return colors;
    }

    /**
     * onClick method for each drawer popup. Most just start another activity but location
     * needs to make sure Location permission is allowed by the user. Settings needs to compare
     * service state with the boolean on hand to see if they ever got shutdown.
     *
     * @param buttonIndex Index representing which button was clicked.
     */
    @Override
    public void onClick(int buttonIndex) {
        Intent intent;
        switch (buttonIndex) {
            case 0:
                if (Build.VERSION.SDK_INT >= 23 && (sharedPreferences.getBoolean(getString(R.string.askPermission), true) || !isLocationPermissionGranted())) {
                    ViewDialog alert = new ViewDialog(MapsActivity.this, "To show your flybys, " +
                            "I first need access to your location.", sharedPreferences, this);
                    alert.showDialog();
                } else {
                    intent = new Intent(context, Locations.class);
                    startActivity(intent);
                }
                break;
            case 1:
                intent = new Intent(context, PeopleinSpace.class);
                startActivity(intent);
                break;
            case 2:
                /* Update the check boxes representing the app's services. If for example the service
                 exited not by the app, it shouldn't be checked. */
                boolean Alert = isMyServiceRunning(Alert.class);

                if (Alert != sharedPreferences.getBoolean("notification_ISS", false)) {
                    Toast.makeText(MapsActivity.this, "ISS Notification wasn't stopped through the app.", Toast.LENGTH_LONG).show();
                    sharedPreferences.edit().putBoolean("notification_ISS", Alert).apply();
                }

                intent = new Intent(context, Preferences.class);
                startActivity(intent);
                break;
            case 3:
                intent = new Intent(context, Help.class);
                startActivity(intent);
                break;
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
     * Start the LiveStream activity
     *
     * @param view N/A
     */
    public void onISS(View view) {
        Intent intent = new Intent(context, LiveStream.class);
        startActivity(intent);
    }

    /**
     * Check to see if user has given us the permission to access their location.
     *
     * @return True or false
     */
    public boolean isLocationPermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) { // Marshmallow or above
            return context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(android.Manifest.permission.INTERNET)
                            == PackageManager.PERMISSION_GRANTED;
        } else { // Permission is automatically granted on sdk < 23 upon installation
            return true;
        }
    }

    @Override
    public void toShow() {
    }

    @Override
    public void showing(float fraction) {
    }

    @Override
    public void showed() {
    }

    @Override
    public void toHide() {
    }

    @Override
    public void hiding(float fraction) {
    }

    @Override
    public void hided() {
    }

    @Override
    public void onClick(View v) {
    }
}
