package com.restart.spacestationtracker;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import com.squareup.leakcanary.LeakCanary;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Contains the google map and uses various API and JSON objects to display
 * the position of the ISS picture on the map and update it occasionally.
 */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        BoomMenuButton.OnSubButtonClickListener,
        BoomMenuButton.AnimatorListener,
        View.OnClickListener {

    private SharedPreferences sharedPref;
    private RequestQueue requestQueue;
    private int refreshrate;
    private boolean start = false;
    private TextView lanlog;
    private GoogleMap mMap;
    private Timer timer;
    private BoomMenuButton boomMenuButtonInActionBar;
    private Context context;
    private AdView adView;
    private boolean first_time;

    /**
     * When the application begins try to read from SharedPreferences
     *
     * @param savedInstanceState on create method
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        LeakCanary.install(getApplication());

        ActionBar mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(false);
        LayoutInflater mInflater = LayoutInflater.from(this);

        adView = null;
        context = getApplicationContext();
        requestQueue = Volley.newRequestQueue(this);
        View mCustomView = mInflater.inflate(R.layout.custom_actionbar, null);
        TextView mTitleTextView = (TextView) mCustomView.findViewById(R.id.title_text);
        mTitleTextView.setText(this.getClass().getSimpleName());

        String classname = this.getClass().getSimpleName();
        switch (classname) {
            case "MapsActivity":
                mTitleTextView.setText(R.string.label_map);
                break;
            case "Locations":
                mTitleTextView.setText(R.string.label_location);
                break;
            case "PeopleinSpace":
                mTitleTextView.setText(R.string.label_peopleinspace);
                break;
            case "Settings":
                mTitleTextView.setText(R.string.label_setting);
                break;
            case "Help":
                mTitleTextView.setText(R.string.label_help);
                break;
        }

        boomMenuButtonInActionBar = (BoomMenuButton) mCustomView.findViewById(R.id.boom);
        boomMenuButtonInActionBar.setOnSubButtonClickListener(this);
        boomMenuButtonInActionBar.setAnimatorListener(this);

        mActionBar.setCustomView(mCustomView);
        mActionBar.setDisplayShowCustomEnabled(true);

        ((Toolbar) mCustomView.getParent()).setContentInsetsAbsolute(0, 0);

        lanlog = ((TextView) findViewById(R.id.textView));
        sharedPref = getSharedPreferences("savefile", MODE_PRIVATE);
        refreshrate = sharedPref.getInt(getString(R.string.freshsave), 2500);
        first_time = sharedPref.getBoolean(getString(R.string.first_time), true);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (!sharedPref.getBoolean(getString(R.string.notificationcheck3), false)) {
            adView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
    }

    /**
     * On resume, the user might have visited the setting activity. Reread the
     * refreshrate.
     * onResume gets called right after onCreate for example when the application
     * gets opened for the first time, or might get called solely if user switches to this
     * activity. If/else for both situations
     * http://i.stack.imgur.com/1byIg.png
     */
    protected void onResume() {
        super.onResume();
        if (start) {
            refreshrate = sharedPref.getInt(getString(R.string.freshsave), 2500);
            timer.cancel();
            timer.purge();
            timer = null;
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    trackISS();
                }
            }, 0, refreshrate);
        } else {
            start = true;
            timer = new Timer();
        }

        if (sharedPref.getBoolean(getString(R.string.notificationcheck3), false) && adView != null) {
            adView.setVisibility(View.INVISIBLE);
        } else if (!sharedPref.getBoolean(getString(R.string.notificationcheck3), false)) {
            if (adView == null) {
                adView = (AdView) findViewById(R.id.adView);
                AdRequest adRequest = new AdRequest.Builder().build();
                adView.loadAd(adRequest);
            } else {
                adView.setVisibility(View.VISIBLE);
            }
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
        String url = "https://api.wheretheiss.at/v1/satellites/25544";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
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
                            lanlog.setText(position);
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
                    String message = e.getMessage();
                    String reason = message + " Error: " + error;
                    Toast.makeText(MapsActivity.this, reason + ".", Toast.LENGTH_LONG).show();

                    return;
                }

                Toast.makeText(MapsActivity.this, "An unknown error has occurred. Error: 401", Toast.LENGTH_LONG).show();
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

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

    @Override
    public void onClick(int buttonIndex) {
        switch (buttonIndex) {
            case 0:
                if (this.getClass().getSimpleName().equals("Locations")) {
                    Toast.makeText(context, "Already at \"Flybys\"", Toast.LENGTH_SHORT).show();
                } else {
                    if (first_time) {
                        askPermission();
                    } else {
                        Intent intent = new Intent(context, Locations.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        startActivity(intent);
                    }
                }
                break;
            case 1:
                if (this.getClass().getSimpleName().equals("PeopleinSpace")) {
                    Toast.makeText(context, "Already at \"Who's in Space?\"", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(context, PeopleinSpace.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(intent);
                }
                break;
            case 2:
                if (this.getClass().getSimpleName().equals("Settings")) {
                    Toast.makeText(context, "Already at \"Settings\"", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(context, Settings.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(intent);
                }
                break;
            case 3:
                if (this.getClass().getSimpleName().equals("Help")) {
                    Toast.makeText(context, "Already at \"Help\"", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(context, Help.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(intent);
                }
                break;
        }
    }

    private void askPermission() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapsActivity.this);
        alertDialog.setTitle("Location Permission");
        alertDialog.setMessage("I need your location to find your ISS flybys. Do make sure your " +
                "location is turned on and that you can find your location on an app such as " +
                "Google maps. I do not store any information from you.");
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
