package com.restart.spacestationtracker;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
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

    private final String TAG = ".MapsActivity";         // Used for volley and occasional Log

    private BoomMenuButton boomMenuButtonInActionBar;   // Drawer manager
    private SharedPreferences sharedPreferences;        // Managing options from Settings
    private MarkerOptions markerOptions;                // Marker options, uses ISS drawable
    private DecimalFormat decimalFormat;                // For number decimal places from zero to five
    private RequestQueue requestQueue;                  // Volley for JSONObject/JSONArray requests
    private Polyline[] polyArray;                       // An array of polyline. Need 200 for a nice curve.
    private TextView description;                       // A description of additional ISS info. Optional
    private Polyline polyLine;                          // A single poly to help compare any changes from settings
    private TextView latLong;                           // lat lon of ISS
    private Context context;                            // Application context
    private Timer polyTimer;                            // Updates the poly lines
    private GoogleMap mMap;                             // Google maps
    private Marker marker;                              // Marker representing ISS that moves alone the prediction line
    private AdView adView;                              // Optional ads
    private LatLng last;                                // Used for connecting polyline.
    private Timer timer;                                // Updates map based on refresh rate

    private int polyCounter;                            // Counts how many polyline there are
    private int refreshrate;                            // Millisecond between each timer repeat of updating ISS's location
    private int success;                                // Tracks # times server failed to respond
    private int poly;                                   // Used for adding on to a timestamp and getting future locations

    private boolean threadManager;                      // Checks if polyLines are already being created.
    private boolean firstTime;                          // Menu Tutorial
    private boolean start;                              // Opened app or returned to activity?
    private boolean once;                               // Move map to ISS's location on start

    /**
     * When the application begins try to read from SharedPreferences to get ready. Run first
     * time setups if needed.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

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

        poly = 0;
        once = true;
        adView = null;
        start = false;
        polyCounter = 0;
        polyArray = new Polyline[200];
        context = getApplicationContext();
        requestQueue = Volley.newRequestQueue(this);
        latLong = ((TextView) findViewById(R.id.textView));
        description = (TextView) findViewById(R.id.textView2);
        PreferenceManager.setDefaultValues(this, R.xml.app_preferences, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        refreshrate = 1000 * sharedPreferences.getInt("refresh_Rate", 1);
        firstTime = sharedPreferences.getBoolean(getString(R.string.firstTime), true);
        markerOptions = new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.iss_2011));
        markerOptions.anchor(0.5f, 0.5f);

        final Activity activity = this;

        //  When view is shown, start our animations for first time users
        latLong.post(new Runnable() {
            @Override
            public void run() {
                if (firstTime) {
                    startAnimation(latLong, mTitleTextView, activity);
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

        Log.e(TAG + " Decimal", sharedPreferences.getString("decimalType", "0.000"));
        decimalFormat = new DecimalFormat(sharedPreferences.getString("decimalType", "0.000"));

        if (start) {                            // When activity was just paused
            refreshrate = 1000 * sharedPreferences.getInt("refresh_Rate", 25);
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
            adView.setVisibility(View.GONE);            // User disabled ads
        } else if (!sharedPreferences.getBoolean("advertisement", false)) {
            if (adView == null) {                       // User wants ads but instance is null
                adView = (AdView) findViewById(R.id.adView);
                AdRequest adRequest = new AdRequest.Builder().addTestDevice("998B51E0DA18B35E1A4C4E6D78084ABB").build();
                adView.loadAd(adRequest);
            } else {                                    // User wants ads, instance already got one
                adView.setVisibility(View.VISIBLE);
            }
        }

        int currentColor = Integer.parseInt(sharedPreferences.getString("colorType", "-256"));
        int currentWidth = Integer.parseInt(sharedPreferences.getString("sizeType", "5"));

        if (polyLine != null &&
                (polyLine.getColor() != currentColor ||
                        polyLine.getWidth() != currentWidth)) {
            if (mMap != null) {
                for (int i = 0; polyArray[i] != null && i < polyArray.length - 1; ++i) {
                    polyArray[i].setColor(currentColor);
                    polyArray[i].setWidth(currentWidth);
                }
            } else {
                Log.e(TAG, "Can't reset the map");
            }
        }

        if (polyTimer == null) {
            polyTimer = new Timer();
            TimerTask hourlyTask = new TimerTask() {
                @Override
                public void run() {
                    asyncTaskPolyline();
                }
            };
            polyTimer.schedule(hourlyTask, 0L, 5400000); // 90 minutes

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

        if (polyTimer != null) {
            polyTimer.cancel();
            polyTimer.purge();
        }
        polyTimer = null;
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

        if (timer == null) {
            timer = new Timer();
        }

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                trackISS();
            }
        }, 0, refreshrate);
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
                poly = 0;
                polyCounter = 0;
            }
        });

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    threadManager = true;
                    Date currentDate = new Date();
                    for (int i = 0; i < 20; ++i) {
                        Log.e(TAG, "asyncTaskPolyline:" + i);
                        updatePolyline(currentDate);
                        Thread.sleep(1000);
                    }
                    threadManager = false;
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
        int current = Integer.parseInt(sharedPreferences.getString("mapType", "2"));

        if (mMap.getMapType() == current) {
            return;
        }

        mMap.setMapType(current);
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
                    final String LAT;
                    final String LNG;

                    if (latParameter < 0) {
                        LAT = decimalFormat.format(latParameter) + "° S";
                    } else {
                        LAT = decimalFormat.format(latParameter) + "° N";
                    }

                    if (lngParameter < 0) {
                        LNG = decimalFormat.format(lngParameter) + "° W";
                    } else {
                        LNG = decimalFormat.format(lngParameter) + "° E";
                    }

                    final String position = LAT + ", " + LNG;
                    final String visibility = response.getString("visibility");

                    final StringBuilder moreInfo = new StringBuilder();
                    moreInfo.append("Altitude: ").append(decimalFormat.format(Double.parseDouble(response.getString("altitude")))).append(" km").append("\n")
                            .append("Velocity: ").append(decimalFormat.format(Double.parseDouble(response.getString("velocity")))).append(" kph").append("\n")
                            .append("Footprint: ").append(decimalFormat.format(Double.parseDouble(response.getString("footprint")))).append(" km").append("\n")
                            .append("Solar LAT: ").append(decimalFormat.format(Double.parseDouble(response.getString("solar_lat")))).append("°").append("\n")
                            .append("Solar LON: ").append(decimalFormat.format(Double.parseDouble(response.getString("solar_lon")))).append("°").append("\n")
                            .append("Visibility: ").append(visibility.substring(0, 1).toUpperCase()).append(visibility.substring(1)).append("\n");

                    MapsActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            if (once) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(ISS));
                                if (mMap.getUiSettings().isScrollGesturesEnabled()) {
                                    mMap.getUiSettings().setScrollGesturesEnabled(false);
                                }
                                once = false;
                            } else if (sharedPreferences.getBoolean("lock_ISS", false)) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(ISS));
                                if (mMap.getUiSettings().isScrollGesturesEnabled()) {
                                    mMap.getUiSettings().setScrollGesturesEnabled(false);
                                }
                            } else if (!sharedPreferences.getBoolean("lock_ISS", false)) {
                                mMap.getUiSettings().setScrollGesturesEnabled(true);
                            }
                            if (marker != null) {
                                marker.remove();
                            }

                            if (description.getVisibility() == View.VISIBLE && !sharedPreferences.getBoolean("info_ISS", false)) {
                                description.setVisibility(View.GONE);
                            } else if (description.getVisibility() == View.GONE && sharedPreferences.getBoolean("info_ISS", false)) {
                                if (!String.valueOf(description.getCurrentTextColor()).equals(sharedPreferences.getString("colorText", "-256"))) {
                                    description.setTextColor(Integer.parseInt(sharedPreferences.getString("colorText", "-256")));
                                }
                                description.setVisibility(View.VISIBLE);
                                description.setText(moreInfo.toString());
                            } else if (description.getVisibility() == View.VISIBLE) {
                                if (!String.valueOf(description.getCurrentTextColor()).equals(sharedPreferences.getString("colorText", "-256"))) {
                                    description.setTextColor(Integer.parseInt(sharedPreferences.getString("colorText", "-256")));
                                }
                                description.setText(moreInfo.toString());
                            }

                            markerOptions.position(ISS);
                            marker = mMap.addMarker(markerOptions);

                            if (!String.valueOf(latLong.getCurrentTextColor()).equals(sharedPreferences.getString("colorText", "-256"))) {
                                latLong.setTextColor(Integer.parseInt(sharedPreferences.getString("colorText", "-256")));
                            }

                            Log.d(TAG, "shared: " + sharedPreferences.getString("colorText", "-256"));
                            Log.d(TAG, "latLong: " + String.valueOf(latLong.getCurrentTextColor()));
                            Log.d(TAG, "description: " + String.valueOf(description.getCurrentTextColor()));
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
    private void updatePolyline(Date currentDate) {
        long currentLong = currentDate.getTime() / 1000;
        final long[] futureTen = new long[10];

        for (int i = 0; i < futureTen.length; ++i) {
            futureTen[i] = currentLong + (30 * poly++);
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

        final int finalStart = poly;
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
                            Log.d(TAG, sharedPreferences.getString("colorType", "-256"));
                            Log.d(TAG, sharedPreferences.getString("sizeType", "5"));

                            if (finalStart == 10) {
                                for (int i = 0; i < futureTen.length - 1; ++i) {
                                    polyLine = mMap.addPolyline(new PolylineOptions()
                                            .add(latLngs[i], latLngs[i + 1])
                                            .width(Integer.parseInt(sharedPreferences.getString("sizeType", "5")))
                                            .color(Integer.parseInt(sharedPreferences.getString("colorType", "-256"))));
                                    polyArray[polyCounter++] = polyLine;
                                }
                                last = latLngs[latLngs.length - 1];
                            } else {
                                polyArray[polyCounter++] = mMap.addPolyline(new PolylineOptions()
                                        .add(last, latLngs[0])
                                        .width(Integer.parseInt(sharedPreferences.getString("sizeType", "5")))
                                        .color(Integer.parseInt(sharedPreferences.getString("colorType", "-256"))));
                                for (int i = 0; i < futureTen.length - 1; ++i) {
                                    polyArray[polyCounter++] = mMap.addPolyline(new PolylineOptions()
                                            .add(latLngs[i], latLngs[i + 1])
                                            .width(Integer.parseInt(sharedPreferences.getString("sizeType", "5")))
                                            .color(Integer.parseInt(sharedPreferences.getString("colorType", "-256"))));
                                }
                                last = latLngs[latLngs.length - 1];
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
     */
    public void onISS() {
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

    /**
     * Starts a three step animation process. One gets executed after another.
     *
     * @param latlng         A view to ISS text position
     * @param mTitleTextView A view to the top text widget
     * @param activity       MapsActivity.java
     */
    public void startAnimation(final View latlng, final View mTitleTextView, final Activity activity) {
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
                .subHeadingTvText("Map shows ISS's current location with a prediction line of where it will be.")
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
                if (!threadManager) {
                    asyncTaskPolyline();
                } else {
                    Toast.makeText(context, "Already creating prediction lines", Toast.LENGTH_SHORT).show();
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
        inflater.inflate(R.menu.reset, menu);
        return true;
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
