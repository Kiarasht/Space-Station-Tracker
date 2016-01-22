package com.restart.spacestationtracker;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
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
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import za.co.riggaroo.materialhelptutorial.TutorialItem;
import za.co.riggaroo.materialhelptutorial.tutorial.MaterialTutorialActivity;

/**
 * Contains the google map and uses various API and JSON objects to display
 * the position of the ISS picture on the map and update it occasionally.
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = ".MapsActivity";
    private static final int REQUEST_CODE = 1234;
    private static int refreshrate;
    private boolean start = false;
    private GoogleMap mMap;
    private Timer timer;
    protected SharedPreferences sharedPref;
    protected SharedPreferences.Editor editor;

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
        boolean firsttime = sharedPref.getBoolean(getString(R.string.firsttime), true);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (firsttime) {
            editor = sharedPref.edit();
            editor.putBoolean(getString(R.string.firsttime), false);
            editor.apply();
            loadTutorial();
        }

        AdView adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
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
            Log.i(TAG, "MapsActivity onResume 1 " + refreshrate);
        } else {
            start = true;
            timer = new Timer();
            Log.i(TAG, "MapsActivity onResume 2 " + refreshrate);
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
        Log.i(TAG, "MapsActivity onMapReady 1 " + refreshrate);
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
        menu.add("Settings").setIntent(new Intent(this, Settings.class));
        menu.add("Location").setIntent(new Intent(this, Locations.class));
        return true;
    }

    /**
     * Tutorial for first time users opening the applications
     */
    public void loadTutorial() {
        Intent mainAct = new Intent(this, MaterialTutorialActivity.class);
        mainAct.putParcelableArrayListExtra(
                MaterialTutorialActivity.MATERIAL_TUTORIAL_ARG_TUTORIAL_ITEMS, getTutorialItems(this));
        startActivityForResult(mainAct, REQUEST_CODE);
    }

    /**
     * 4 TutorialItems each representing a page with their own text and picture.
     *
     * @param context getApplicationContext();
     * @return The array list of tutorial pages
     */
    private ArrayList<TutorialItem> getTutorialItems(Context context) {
        TutorialItem tutorialItem1 = new TutorialItem(context.getString(R.string.tutorial_title1),
                context.getString(R.string.tutorial_descr1),
                R.color.tutorial_color1, R.drawable.tutorial_picture1);

        TutorialItem tutorialItem2 = new TutorialItem(context.getString(R.string.tutorial_title2),
                context.getString(R.string.tutorial_descr2),
                R.color.tutorial_color2, R.drawable.tutorial_picture2);

        TutorialItem tutorialItem3 = new TutorialItem(context.getString(R.string.tutorial_title3),
                context.getString(R.string.tutorial_descr3),
                R.color.tutorial_color3, R.drawable.tutorial_picture3);

        TutorialItem tutorialItem4 = new TutorialItem(context.getString(R.string.tutorial_title4),
                context.getString(R.string.tutorial_descr4),
                R.color.tutorial_color4, R.drawable.tutorial_picture4);

        ArrayList<TutorialItem> tutorialItems = new ArrayList<>();
        tutorialItems.add(tutorialItem1);
        tutorialItems.add(tutorialItem2);
        tutorialItems.add(tutorialItem3);
        tutorialItems.add(tutorialItem4);

        return tutorialItems;
    }

    /**
     * Results received from the tutorial actions
     *
     * @param requestCode Should be 1234
     * @param resultCode  Should be -1
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
            Log.i(TAG, "Tutorial finished successfully.");
        }
    }
}
