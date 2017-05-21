package com.restart.spacestationtracker;


import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.restart.spacestationtracker.data.Astronaut;
import com.restart.spacestationtracker.adapter.PeopleInSpaceAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PeopleinSpace extends AppCompatActivity {

    private PeopleInSpaceAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private RequestQueue mRequestQueue;
    private AdView adView;
    private Activity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycler_layout);

        findViewById(R.id.actionLinear).setVisibility(View.GONE);
        mActivity = this;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mRequestQueue = Volley.newRequestQueue(this);
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
     * Displays a list of astronauts in a RecyclerView
     */
    private void display_people() {
        final String url = "http://www.howmanypeopleareinspacerightnow.com/peopleinspace.json";
        final List<Astronaut> peopleInSpace = new ArrayList<>();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    LinearLayoutManager layoutManager = new LinearLayoutManager(mActivity, LinearLayoutManager.VERTICAL, false);
                    mRecyclerView = (RecyclerView) findViewById(R.id.recycler);
                    mRecyclerView.setLayoutManager(layoutManager);
                    mAdapter = new PeopleInSpaceAdapter(mActivity, peopleInSpace);
                    mRecyclerView.setHasFixedSize(true);
                    mRecyclerView.setNestedScrollingEnabled(true);
                    mAdapter.setDataSet(peopleInSpace);
                    mRecyclerView.setAdapter(mAdapter);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
            }
        }) {
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                try {
                    String jsonString = new String(response.data,
                            HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));

                    JSONObject jsonResponse = new JSONObject(jsonString);
                    try {
                        JSONArray astronauts = jsonResponse.getJSONArray("people");

                        for (int i = 0; i < astronauts.length(); ++i) {
                            JSONObject anAstronaut = astronauts.getJSONObject(i);

                            final String name = anAstronaut.getString("name");
                            final String image = anAstronaut.getString("biophoto");
                            final String countryLink = anAstronaut.getString("countryflag");
                            final String launchDate = anAstronaut.getString("launchdate");
                            String role = anAstronaut.getString("title");
                            final String location = anAstronaut.getString("location");
                            final String bio = anAstronaut.getString("bio");
                            final String wiki = anAstronaut.getString("biolink");
                            final String twitter = anAstronaut.getString("twitter");

                            if (role != null && !role.isEmpty())
                                role = role.substring(0, 1).toUpperCase() + role.substring(1);
                            Astronaut storeAnAstronaut = new Astronaut(name, image, countryLink, launchDate, role, location, bio, wiki, twitter);
                            peopleInSpace.add(storeAnAstronaut);
                        }

                        Collections.sort(peopleInSpace);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return Response.success(new JSONObject(jsonString), HttpHeaderParser.parseCacheHeaders(response));
                } catch (UnsupportedEncodingException e) {
                    return Response.error(new ParseError(e));
                } catch (JSONException je) {
                    return Response.error(new ParseError(je));
                }
            }
        };

        mRequestQueue.add(jsonObjectRequest);
    }
}
