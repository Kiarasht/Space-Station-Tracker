package com.restart.spacestationtracker;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.restart.spacestationtracker.adapter.PeopleInSpaceAdapter;
import com.restart.spacestationtracker.data.Astronaut;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PeopleInSpace extends AppCompatActivity {

    private PeopleInSpaceAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private RequestQueue mRequestQueue;
    private AdView adView;
    private Activity mActivity;
    private boolean mPaddingOnce;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.people_in_space_layout);

        mRecyclerView = findViewById(R.id.recycler);
        mActivity = this;
        mRequestQueue = Volley.newRequestQueue(this);
        display_people();

        adView = findViewById(R.id.adView);
        adView.loadAd(new AdRequest.Builder().build());
        adView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (!mPaddingOnce) {
                mPaddingOnce = true;
                mRecyclerView.setPadding(mRecyclerView.getPaddingLeft(), mRecyclerView.getPaddingTop(), mRecyclerView.getPaddingRight(), mRecyclerView.getPaddingBottom() + adView.getHeight());
            }
        });
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
        final String url = "https://www.howmanypeopleareinspacerightnow.com/peopleinspace.json";
        final List<Astronaut> peopleInSpace = new ArrayList<>();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,
                null, response -> {
            try {
                LinearLayoutManager layoutManager = new LinearLayoutManager(mActivity, LinearLayoutManager.VERTICAL, false);
                mRecyclerView.setLayoutManager(layoutManager);
                mAdapter = new PeopleInSpaceAdapter(mActivity, peopleInSpace);
                mRecyclerView.setNestedScrollingEnabled(true);
                mAdapter.setDataSet(peopleInSpace);
                mRecyclerView.setAdapter(mAdapter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, e -> {
            if (e != null) {
                Log.e("PeopleInSpace Volley", e.getMessage());
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

                            if (!role.isEmpty())
                                role = role.substring(0, 1).toUpperCase() + role.substring(1);
                            Astronaut storeAnAstronaut = new Astronaut(name, image, countryLink, launchDate, role, location, bio, wiki, twitter);
                            peopleInSpace.add(storeAnAstronaut);
                        }

                        Collections.sort(peopleInSpace);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return Response.success(new JSONObject(jsonString), HttpHeaderParser.parseCacheHeaders(response));
                } catch (UnsupportedEncodingException | JSONException e) {
                    return Response.error(new ParseError(e));
                }
            }
        };

        mRequestQueue.add(jsonObjectRequest);
    }
}
