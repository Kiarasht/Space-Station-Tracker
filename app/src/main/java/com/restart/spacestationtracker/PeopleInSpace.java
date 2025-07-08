package com.restart.spacestationtracker;

import static com.android.volley.toolbox.HttpHeaderParser.parseCharset;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.restart.spacestationtracker.adapter.PeopleInSpaceAdapter;
import com.restart.spacestationtracker.data.Astronaut;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class PeopleInSpace extends AppCompatActivity {
    private final String PEOPLE_URL = "https://corquaid.github.io/international-space-station-APIs/JSON/people-in-space.json";
    private final String BIO_URL = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=";
    private PeopleInSpaceAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private RequestQueue mRequestQueue;
    private LottieAnimationView animation;
    private TextView errors;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.people_in_space_layout);

        mRecyclerView = findViewById(R.id.recycler);
        mRequestQueue = Volley.newRequestQueue(this);
        display_people();

        animation = findViewById(R.id.animation_view);
        errors = findViewById(R.id.errors);
    }

    private void onSuccessResult(List<Astronaut> peopleInSpace) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mAdapter = new PeopleInSpaceAdapter(this, peopleInSpace);
        mRecyclerView.setNestedScrollingEnabled(true);
        mAdapter.setDataSet(peopleInSpace);
        mRecyclerView.setAdapter(mAdapter);
        animation.setVisibility(View.GONE);
        errors.setVisibility(View.GONE);
    }

    private void onFailure(List<Astronaut> peopleInSpace) {
        if (peopleInSpace.isEmpty()) {
            animation.setVisibility(View.VISIBLE);
            errors.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Displays a list of astronauts in a RecyclerView
     */
    private void display_people() {
        final List<Astronaut> peopleInSpace = new ArrayList<>();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, PEOPLE_URL, null, astronautResponse -> {
            onSuccessResult(peopleInSpace);
            requestWiki(peopleInSpace);
        }, e -> {
            onFailure(peopleInSpace);
        }) {
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                try {
                    String jsonString = new String(response.data, parseCharset(response.headers, PROTOCOL_CHARSET));
                    JSONObject astronautJson = new JSONObject(jsonString);

                    JSONArray astronauts = astronautJson.getJSONArray("people");

                    for (int i = 0; i < astronauts.length(); ++i) {
                        JSONObject anAstronaut = astronauts.getJSONObject(i);

                        final String name = anAstronaut.getString("name");
                        final String image = anAstronaut.getString("image");
                        final boolean isIss = anAstronaut.getBoolean("iss");
                        final String flagCode = anAstronaut.getString("flag_code").toUpperCase();
                        final int launchDate = anAstronaut.getInt("launched");
                        final String role = anAstronaut.getString("position");
                        final String location = anAstronaut.getString("spacecraft");
                        final String twitter = anAstronaut.getString("twitter");
                        final String facebook = anAstronaut.getString("facebook");
                        final String instagram = anAstronaut.getString("instagram");
                        final String wiki = anAstronaut.getString("url");

                        Astronaut storeAnAstronaut = new Astronaut(name, image, isIss, flagCode, launchDate, role, location, "", wiki, twitter, facebook, instagram);
                        peopleInSpace.add(storeAnAstronaut);
                    }

                    return Response.success(new JSONObject(jsonString), HttpHeaderParser.parseCacheHeaders(response));
                } catch (UnsupportedEncodingException | JSONException e) {
                    return Response.error(new ParseError(e));
                }
            }
        };

        mRequestQueue.add(jsonObjectRequest);
    }

    private void requestWiki(List<Astronaut> astronauts) {
        for (int i = 0; i < astronauts.size(); ++i) {
            final String wiki = astronauts.get(i).getWiki();
            final String[] wikiPage = wiki.split("/");
            final String bioUrl = BIO_URL + wikiPage[wikiPage.length - 1];

            JsonObjectRequest wikiRequest = new JsonObjectRequest(Request.Method.GET, bioUrl, null, wikiResponse -> {
                int index = 0;
                for (Astronaut astronaut: astronauts) {
                    if (astronaut.getWiki().equals(wiki)) {
                        try {
                            JSONObject pages = wikiResponse.getJSONObject("query").getJSONObject("pages");
                            JSONObject innerObject = pages.getJSONObject(pages.keys().next());
                            String bio = innerObject.getString("extract");
                            while (bio.endsWith("\n"))
                                bio = bio.substring(0, bio.length() - 2);
                            astronaut.setBio(bio);
                            mAdapter.notifyItemChanged(index);
                        } catch (JSONException e) {
                            astronaut.setBio("");
                        }
                    }
                    ++index;
                }
            }, e -> {
            }) {
                @Override
                protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                    try {
                        String bioJson = new String(response.data, parseCharset(response.headers, PROTOCOL_CHARSET));
                        return Response.success(new JSONObject(bioJson), HttpHeaderParser.parseCacheHeaders(response));
                    } catch (UnsupportedEncodingException | JSONException e) {
                        return Response.error(new ParseError(e));
                    }
                }
            };

            mRequestQueue.add(wikiRequest);
        }
    }
}
