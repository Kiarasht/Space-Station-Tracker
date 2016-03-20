package com.restart.spacestationtracker;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.Menu;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Contains the google map and uses various API and JSON objects to display
 * the position of the ISS picture on the map and update it occasionally.
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = ".MapsActivity";
    protected SharedPreferences.Editor editor;
    protected SharedPreferences sharedPref;
    private static int refreshrate;
    private boolean start = false;
    private GoogleMap mMap;
    private Timer timer;

    /**
     * When the application begins try to read from SharedPreferences
     *
     * @param savedInstanceState on create method
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        sharedPref = getSharedPreferences("savefile", MODE_PRIVATE);
        refreshrate = sharedPref.getInt(getString(R.string.freshsave), 2500);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        AdView adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().addTestDevice(getString(R.string.deviceid)).build();
        adView.loadAd(adRequest);
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
        AsyncTask.execute(new Runnable() {
            public void run() {
                String strContent = "";

                try {
                    URL urlHandle = new URL("http://api.open-notify.org/iss-now.json");
                    URLConnection urlconnectionHandle = urlHandle.openConnection();
                    InputStream inputstreamHandle = urlconnectionHandle.getInputStream();

                    try {
                        int intRead;
                        byte[] byteBuffer = new byte[1024];

                        do {
                            intRead = inputstreamHandle.read(byteBuffer);

                            if (intRead == 0) {
                                break;

                            } else if (intRead == -1) {
                                break;
                            }

                            strContent += new String(byteBuffer, 0, intRead, "UTF-8");
                        } while (true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    inputstreamHandle.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    JSONObject results = new JSONObject(strContent).getJSONObject("iss_position");

                    final double latParameter = Double.parseDouble(results.getString("latitude"));
                    final double lngParameter = Double.parseDouble(results.getString("longitude"));

                    MapsActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            LatLng ISS = new LatLng(latParameter, lngParameter);
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(ISS));
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create more activity option accessible in the top right corner of the screen. The
     * activities will also need to be declared in the AndroidManifest
     *
     * @param menu Object we need to manipulate to add our activities.
     * @return Return results
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add("Flybys").setIntent(new Intent(this, Locations.class));
        menu.add("Who's in space?").setIntent(new Intent(this, PeopleinSpace.class));
        menu.add("Settings").setIntent(new Intent(this, Settings.class));
        menu.add("Help").setIntent(new Intent(this, Help.class));
        return true;
    }
}
