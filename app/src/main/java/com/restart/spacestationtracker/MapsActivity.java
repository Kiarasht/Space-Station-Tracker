package com.restart.spacestationtracker;


import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

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
import java.text.DecimalFormat;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.nightonke.boommenu.BoomMenuButton;
import com.nightonke.boommenu.Types.BoomType;
import com.nightonke.boommenu.Types.ButtonType;
import com.nightonke.boommenu.Types.PlaceType;
import com.nightonke.boommenu.Util;


/**
 * Contains the google map and uses various API and JSON objects to display
 * the position of the ISS picture on the map and update it occasionally.
 */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        BoomMenuButton.OnSubButtonClickListener,
        BoomMenuButton.AnimatorListener,
        View.OnClickListener {

    private static final String TAG = ".MapsActivity";
    protected SharedPreferences.Editor editor;
    protected SharedPreferences sharedPref;
    private static int refreshrate;
    private boolean start = false;
    private TextView lanlog;
    private GoogleMap mMap;
    private Timer timer;
    private View mCustomView;
    private BoomMenuButton boomMenuButton;
    private BoomMenuButton boomMenuButtonInActionBar;

    /**
     * When the application begins try to read from SharedPreferences
     *
     * @param savedInstanceState on create method
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        ActionBar mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(false);
        LayoutInflater mInflater = LayoutInflater.from(this);

        mCustomView = mInflater.inflate(R.layout.custom_actionbar, null);
        TextView mTitleTextView = (TextView) mCustomView.findViewById(R.id.title_text);
        mTitleTextView.setText(R.string.app_name);


        boomMenuButtonInActionBar = (BoomMenuButton) mCustomView.findViewById(R.id.boom);
        boomMenuButtonInActionBar.setOnSubButtonClickListener(this);
        boomMenuButtonInActionBar.setAnimatorListener(this);

        mActionBar.setCustomView(mCustomView);
        mActionBar.setDisplayShowCustomEnabled(true);

        ((Toolbar) mCustomView.getParent()).setContentInsetsAbsolute(0,0);

        lanlog = ((TextView) findViewById(R.id.textView));
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
        });
    }

     /**
     * Create more activity option accessible in the top right corner of the screen. The
     * activities will also need to be declared in the AndroidManifest
     *
     * @param menu Object we need to manipulate to add our activities.
     * @return Return results
     */
/*    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add("Flybys").setIntent(new Intent(this, Locations.class));
        menu.add("Who's in space?").setIntent(new Intent(this, PeopleinSpace.class));
        menu.add("Settings").setIntent(new Intent(this, Settings.class));
        menu.add("Help").setIntent(new Intent(this, Help.class));
        return true;
    }*/

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        int number = 4;

        Drawable[] drawables = new Drawable[number];
        int[] drawablesResource = new int[]{
                R.drawable.ic_media_pause,
                R.drawable.ic_media_pause,
                R.drawable.ic_media_pause,
                R.drawable.ic_media_pause,
        };

        for (int i = 0; i < number; i++)
            drawables[i] = ContextCompat.getDrawable(this, drawablesResource[i]);

        String[] STRINGS = new String[]{
                "Mark",
                "Refresh",
                "Copy",
                "Heart",
        };
        String[] strings = new String[number];
        System.arraycopy(STRINGS, 0, strings, 0, number);

        int[][] colors = new int[number][2];
        for (int i = 0; i < number; i++) {
            colors[i][1] = GetRandomColor();
            colors[i][0] = Util.getInstance().getPressedColor(colors[i][1]);
        }

        boomMenuButtonInActionBar.init(
                drawables, // The drawables of images of sub buttons. Can not be null.
                strings,     // The texts of sub buttons, ok to be null.
                colors,    // The colors of sub buttons, including pressed-state and normal-state.
                ButtonType.HAM,     // The button type.
                BoomType.LINE,  // The boom type.
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

    public int GetRandomColor() {
        Random random = new Random();
        int r = random.nextInt(255);
        int g = random.nextInt(255);
        int b = random.nextInt(255);
        return Color.rgb(r,g,b);
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

    @Override
    public void onClick(int buttonIndex) {
        switch (buttonIndex) {
            case 1:
                setIntent(new Intent(this, Locations.class));
                break;
            case 2:
                setIntent(new Intent(this, PeopleinSpace.class));
                break;
            case 3:
                setIntent(new Intent(this, Settings.class));
                break;
            case 4:
                setIntent(new Intent(this, Help.class));
                break;
        }
    }
}
