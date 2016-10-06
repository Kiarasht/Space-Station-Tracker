package com.restart.spacestationtracker;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.restart.spacestationtracker.data.Astronaut;
import com.restart.spacestationtracker.view.CustomList;

public class PeopleinSpace extends AppCompatActivity {

    private ProgressDialog mProgressDialog;
    private Context mContext;
    private AdView adView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        Firebase.setAndroidContext(this);
        setContentView(R.layout.layout_locations);
        startAnimation();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        display_people();

        // Show an ad, or hide it if its disabled
        if (!sharedPreferences.getBoolean("advertisement", false)) {
            adView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().addTestDevice("998B51E0DA18B35E1A4C4E6D78084ABB").build();
            if (adView != null) {
                adView.loadAd(adRequest);
            }
        } else {
            findViewById(R.id.adView).setVisibility(View.GONE);
        }
    }

    /**
     * Cancel any request on Volley after user goes to another activity.
     */
    protected void onPause() {
        super.onPause();
        if (adView != null) {
            adView.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    public void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        AdView adView = (AdView) findViewById(R.id.adView);
        RelativeLayout parent = (RelativeLayout) adView.getParent();
        ViewGroup.LayoutParams adViewParams = adView.getLayoutParams();

        parent.removeView(adView);
        AdView newAdView = new AdView(mContext);
        newAdView.setAdSize(AdSize.SMART_BANNER);
        newAdView.setAdUnitId(getString(R.string.banner_ad_unit_id));
        newAdView.setId(R.id.adView);

        parent.addView(newAdView, adViewParams);
        newAdView.loadAd(new AdRequest.Builder().addTestDevice("998B51E0DA18B35E1A4C4E6D78084ABB").build());
    }

    /**
     * Displays a list of astronauts in a ListView using Firebase.
     */
    public void display_people() {
        Firebase firebase = new Firebase("https://project-5182046725513325760.firebaseio.com/");
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Astronaut[] astronauts = new Astronaut[6];
                final String[] names;

                int i = 0;
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    astronauts[i++] = postSnapshot.getValue(Astronaut.class);
                }

                Astronaut.commanderFirst(astronauts);
                final Astronaut[] onDutyAstronauts = Astronaut.offDuty(astronauts);
                names = Astronaut.getNames(onDutyAstronauts);
                final CustomList astroAdapter = new CustomList(PeopleinSpace.this, onDutyAstronauts, names);
                final ListView astroListView = (ListView) findViewById(R.id.listView);
                PeopleinSpace.this.runOnUiThread(new Runnable() {
                    public void run() {
                        astroListView.setAdapter(astroAdapter);
                        astroListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                            @Override
                            public void onItemClick(AdapterView<?> parent, View view,
                                                    final int position, long id) {
                                startActivity(new Intent(getApplicationContext(), Info.class)
                                        .putExtra("url", onDutyAstronauts[position].getWiki())
                                        .putExtra("astro", onDutyAstronauts[position].getName()));
                            }
                        });
                        endAnimation();
                    }
                });
            }

            @Override
            public void onCancelled(FirebaseError error) {
                Toast.makeText(getApplicationContext(), "Unable to connect to database", Toast.LENGTH_SHORT).show();
                endAnimation();
            }
        };
        firebase.addValueEventListener(valueEventListener);
    }

    void startAnimation() {
        mProgressDialog = new ProgressDialog(PeopleinSpace.this);
        mProgressDialog.setTitle("Just a moment...");
        mProgressDialog.setMessage("Getting astronauts on duty");
    }

    void endAnimation() {
        mProgressDialog.hide();
    }
}
