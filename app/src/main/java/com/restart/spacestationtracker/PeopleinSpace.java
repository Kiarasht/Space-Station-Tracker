package com.restart.spacestationtracker;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.restart.spacestationtracker.data.Astronaut;
import com.restart.spacestationtracker.view.CustomList;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PeopleinSpace extends AppCompatActivity {

    private RequestQueue requestQueue;
    private AdView adView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_locations);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        requestQueue = Volley.newRequestQueue(this);
        display_people();

        // Show an ad, or hide it if its disabled
        if (!sharedPreferences.getBoolean("advertisement", false)) {
            adView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().addTestDevice(getString(R.string.test_device)).build();
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

    /**
     * Displays a list of astronauts in a ListView using Firebase.
     */
    private void display_people() {
        final String url = "http://www.howmanypeopleareinspacerightnow.com/peopleinspace.json";
        final List<Astronaut> peopleInSpace = new ArrayList<>();
        final List<String> astronautNames = new ArrayList<>();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray astronauts = response.getJSONArray("people");

                    for (int i = 0; i < astronauts.length(); ++i) {
                        JSONObject anAstronaut = astronauts.getJSONObject(i);

                        final String name = anAstronaut.getString("name");
                        final String image = anAstronaut.getString("biophoto");
                        final String country = anAstronaut.getString("country");
                        final String countryLink = anAstronaut.getString("countryflag");
                        final String launchDate = anAstronaut.getString("launchdate");
                        final String role = anAstronaut.getString("title");
                        final String location = anAstronaut.getString("location");
                        final String bio = anAstronaut.getString("bio");
                        final String wiki = anAstronaut.getString("biolink");
                        final String twitter = anAstronaut.getString("twitter");
                        final int careerDays = anAstronaut.getInt("careerdays");

                        astronautNames.add(name);
                        Astronaut storeAnAstronaut = new Astronaut(name, image, country, countryLink, launchDate, role, location, bio, wiki, twitter, careerDays);
                        peopleInSpace.add(storeAnAstronaut);
                    }

                    Collections.sort(peopleInSpace);
                    final CustomList astroAdapter = new CustomList(PeopleinSpace.this, peopleInSpace, astronautNames);
                    final ListView astroListView = (ListView) findViewById(R.id.listView);
                    astroListView.setAdapter(astroAdapter);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
            }
        });

        requestQueue.add(jsonObjectRequest);
    }
}
