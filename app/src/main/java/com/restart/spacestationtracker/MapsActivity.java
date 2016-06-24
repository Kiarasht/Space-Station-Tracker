package com.restart.spacestationtracker;


import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.nightonke.boommenu.BoomMenuButton;
import com.nightonke.boommenu.Types.BoomType;
import com.nightonke.boommenu.Types.ButtonType;
import com.nightonke.boommenu.Types.PlaceType;
import com.nightonke.boommenu.Util;
import com.restart.spacestationtracker.services.Alert;
import com.restart.spacestationtracker.services.AlertPeople;
import com.squareup.leakcanary.LeakCanary;
import com.wooplr.spotlight.SpotlightView;
import com.wooplr.spotlight.utils.SpotlightListener;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;


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
    private SharedPreferences sharedPref;               // Managing app's control flow
    private SharedPreferences sharedPreferences;        // Managing options from Settings
    private RequestQueue requestQueue;
    private GoogleMap mMap;
    private Timer timer;                                // Updates map based on refreshrate
    private Context context;
    private AdView adView;
    private TextView latlong;                           // latlong of ISS
    private int refreshrate;                            // Millisecond between each timer repeat
    private int success;                                // Tracks # times server failed to respond
    private boolean first_time;                         // Ask your for location permission once
    private boolean first_time2;                        // Menu Tutorial
    private boolean start = false;                      // Opened app or returned to activity?


    /**
     * When the application begins try to read from SharedPreferences to get ready
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // TODO Remove this after checking for more leak
        LeakCanary.install(getApplication());

        ActionBar mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(false);
        LayoutInflater mInflater = LayoutInflater.from(this);

        View mCustomView = mInflater.inflate(R.layout.custom_actionbar, null);
        TextView mTitleTextView = (TextView) mCustomView.findViewById(R.id.title_text);
        mTitleTextView.setText("Map");

        boomMenuButtonInActionBar = (BoomMenuButton) mCustomView.findViewById(R.id.boom);
        boomMenuButtonInActionBar.setOnSubButtonClickListener(this);
        boomMenuButtonInActionBar.setAnimatorListener(this);

        mActionBar.setCustomView(mCustomView);
        mActionBar.setDisplayShowCustomEnabled(true);

        ((Toolbar) mCustomView.getParent()).setContentInsetsAbsolute(0, 0);

        adView = null;
        context = getApplicationContext();
        requestQueue = Volley.newRequestQueue(this);
        latlong = ((TextView) findViewById(R.id.textView));
        sharedPref = getSharedPreferences("savefile", MODE_PRIVATE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        refreshrate = 1000 * sharedPreferences.getInt("refresh_Rate", 15);
        Log.d(TAG, "Refreshrate = " + refreshrate);
        first_time = sharedPref.getBoolean(getString(R.string.first_time), true);
        first_time2 = sharedPref.getBoolean(getString(R.string.first_time2), true);
        final View view = findViewById(R.id.imageView1);
        final Activity activity = this;

        //  When view is shown start our animated animations for first time users
        view.post(new Runnable() {
            @Override
            public void run() {
                startAnimation(view, findViewById(R.id.textView), activity);
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (!sharedPreferences.getBoolean("advertisement", false)) {
            adView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().addTestDevice("998B51E0DA18B35E1A4C4E6D78084ABB").build();
            adView.loadAd(adRequest);
        }

    }

    /**
     * Starts a three step animation process. One gets executed after another.
     *
     * @param view     A view to ISS icon
     * @param view2    A view to the top text widget
     * @param activity MapsActivity.java
     */
    public void startAnimation(final View view, final View view2, final Activity activity) {
        if (first_time2) {
            new SpotlightView.Builder(activity)
                    .introAnimationDuration(400)
                    .enableRevalAnimation(true)
                    .performClick(true)
                    .fadeinTextDuration(400)
                    .headingTvColor(Color.parseColor("#6441A5"))
                    .headingTvSize(32)
                    .headingTvText("Hi There!")
                    .subHeadingTvColor(Color.parseColor("#ffffff"))
                    .subHeadingTvSize(16)
                    .subHeadingTvText("Let's quickly go over some of the features. This page is a map showing ISS's current location.")
                    .maskColor(Color.parseColor("#dc000000"))
                    .target(view2)
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
                                    .subHeadingTvText("Clicking this icon takes you to features such as flybys, settings, etc...")
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
                                                    .subHeadingTvText("Clicking this ISS icon will take you to a live stream." +
                                                            " Basically what astronauts see right now.")
                                                    .maskColor(Color.parseColor("#dc000000"))
                                                    .target(view)
                                                    .lineAnimDuration(400)
                                                    .lineAndArcColor(Color.parseColor("#6441A5"))
                                                    .dismissOnTouch(true)
                                                    .usageId("3")
                                                    .show();
                                        }
                                    })
                                    .show();
                        }
                    })
                    .show();
        }
    }

    /**
     * On resume, for when the user might have visited the setting activity and came back.
     * Reread the refreshrate.
     * onResume gets called right after onCreate for example when the application
     * gets opened for the first time, or might get called solely if user switches to this
     * activity. If/else for both situations
     * http://i.stack.imgur.com/1byIg.png
     */
    protected void onResume() {
        super.onResume();
        if (start) {                            // When activity was just paused
            refreshrate = 1000 * sharedPreferences.getInt("refresh_Rate", 15);
            Log.d(TAG, "Refreshrate = " + refreshrate);
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
        } else {                                // When activity is killed or created for first time
            start = true;
            timer = new Timer();
        }

        // Provide optional advertisements that is visible/hidden by a checkbox in Preferences
        if (sharedPreferences.getBoolean("advertisement", false) && adView != null) {
            adView.setVisibility(View.INVISIBLE);       // User disabled ads
        } else if (!sharedPreferences.getBoolean("advertisement", false)) {
            if (adView == null) {                       // User wants ads but instance is null
                adView = (AdView) findViewById(R.id.adView);
                AdRequest adRequest = new AdRequest.Builder().addTestDevice("998B51E0DA18B35E1A4C4E6D78084ABB").build();
                adView.loadAd(adRequest);
            } else {                                    // User wants ads, instance already got one
                adView.setVisibility(View.VISIBLE);
            }
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
        timer = null;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mMap.getUiSettings().setScrollGesturesEnabled(false);

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                trackISS();
            }
        }, 0, refreshrate);
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

                    DecimalFormat decimalFormat = new DecimalFormat("0.000");
                    String LAT = decimalFormat.format(latParameter);
                    String LNG = decimalFormat.format(lngParameter);

                    final String position = LAT + "° N, " + LNG + "° E";

                    MapsActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            LatLng ISS = new LatLng(latParameter, lngParameter);
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(ISS));
                            latlong.setText(position);
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
                    final String message = e.getMessage();
                    final String reason = message + " Error: " + error;
                    Toast.makeText(MapsActivity.this, reason + ".", Toast.LENGTH_LONG).show();

                    return;
                }

                // Server did not respond. Got 5 chances before stop trying.
                if (++success <= 4) {
                    Toast.makeText(MapsActivity.this,
                            "Request to server failed. " + success + " out of 5. Trying in " +
                                    refreshrate / 1000 + " seconds.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MapsActivity.this,
                            "Failed all 5 times. Either you have no connection or server is overloaded.",
                            Toast.LENGTH_LONG).show();
                    timer.cancel();
                    timer.purge();
                    timer = null;
                }
            }
        });
        jsonObjectRequest.setTag(TAG);
        requestQueue.add(jsonObjectRequest);
    }

    /**
     * Method responsible of creating and designing the popup drawer. (BoomMenuButton)
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        int number = 4;

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
     * needs to make sure Location permission is allowed by the user.
     *
     * @param buttonIndex Index representing which button was clicked.
     */
    @Override
    public void onClick(int buttonIndex) {
        Intent intent;

        switch (buttonIndex) {
            case 0:
                if (first_time) {
                    askPermission();
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
                sharedPreferences.edit().putBoolean("notification_ISS", isMyServiceRunning(Alert.class)).apply();
                sharedPreferences.edit().putBoolean("notification_Astro", isMyServiceRunning(AlertPeople.class)).apply();
                intent = new Intent(context, Preferences.class);
                startActivity(intent);
                break;
            case 3:
                intent = new Intent(context, Help.class);
                startActivity(intent);
                break;
        }
    }

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
     * Make sure the user knows why we need access to their location, specially for Marshmallow
     * users.
     */
    private void askPermission() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapsActivity.this);
        alertDialog.setTitle("Location Permission");
        alertDialog.setMessage("I need your location to find your ISS flybys. Do make sure your " +
                "location is turned on and that you can find your location on an app such as " +
                "Google maps. If you are on Marshmallow 6.0 or above \"Allow\" the permission and " +
                "click on the \"Flybys\" options again. \nI do not store any information from you.");
        alertDialog.setIcon(R.drawable.ic_report_problem);
        alertDialog.setCancelable(true);
        alertDialog.setPositiveButton("Ok", null);
        alertDialog.setNegativeButton("Cancel", null);

        LinearLayout layout = new LinearLayout(MapsActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        alertDialog.setView(layout);

        final AlertDialog final_dialog = alertDialog.create();
        final_dialog.show();

        final_dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                first_time = false;
                sharedPref.edit().putBoolean(getString(R.string.first_time), false).apply();
                final_dialog.dismiss();
                Intent intent = new Intent(context, Locations.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
            }
        });

        final_dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final_dialog.dismiss();
            }
        });
    }

    public void onISS(View view) {
        Intent intent = new Intent(context, LiveStream.class);
        startActivity(intent);
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
