package com.restart.spacestationtracker;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PeopleinSpace extends AppCompatActivity {

    private final String TAG = ".PeopleinSpace";
    private RequestQueue requestQueue;
    private SharedPreferences sharedPref;
    private Firebase myFirebaseRef;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);
        myFirebaseRef = new Firebase("https://project-5182046725513325760.firebaseio.com/");
        setContentView(R.layout.layout_locations);
        startAnimation();
        sharedPref = getSharedPreferences("savefile", MODE_PRIVATE);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        requestQueue = Volley.newRequestQueue(this);
        display_people(false, null);

        // Show an ad, or hide it if its disabled
        if (!sharedPreferences.getBoolean("advertisement", false)) {
            AdView adView = (AdView) findViewById(R.id.adView);
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
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }

    /**
     * Finds a string that holds the values of people in space.
     *
     * @param intent             Is the function getting called from a service or from the activity? If its a
     *                           service then we don't to update objects such as textboxes.
     * @param applicationContext Used if coming from a service
     * @return Return a string variable holding the astro people.
     */
    public String display_people(final Boolean intent, Context applicationContext) {
        final String url = "http://api.open-notify.org/astros.json";
        final StringBuilder astro_detail = new StringBuilder();     // For Alert service

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray results = response.getJSONArray("people");
                    int numbers = response.getInt("number");
                    final String[] astro = new String[numbers];
                    final Astronaut[] astronauts = new Astronaut[numbers];

                    for (int i = 0; i < numbers; i += 1) {
                        JSONObject result = results.getJSONObject(i);
                        astro[i] = String.valueOf(result.getString("name"));
                        astro_detail.append(String.valueOf(result.getString("name")));
                    }

                    // If false, then PeopleinSpace.java called. So we will call UiThread
                    if (!intent) {
                        myFirebaseRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                int i = 0;
                                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                                    astronauts[i++] = postSnapshot.getValue(Astronaut.class);
                                }
                                Astronaut.commanderFirst(astronauts);
                                final CustomList astroAdapter = new CustomList(PeopleinSpace.this, astro, astronauts);
                                final ListView astroListView = (ListView) findViewById(R.id.listView);
                                PeopleinSpace.this.runOnUiThread(new Runnable() {
                                    public void run() {
                                        astroListView.setAdapter(astroAdapter);
                                        astroListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                                            @Override
                                            public void onItemClick(AdapterView<?> parent, View view,
                                                                    final int position, long id) {
                                                if (astronauts[position] != null) {
                                                    startActivity(new Intent(getApplicationContext(), Info.class)
                                                            .putExtra("url", astronauts[position].getWiki())
                                                            .putExtra("astro", astronauts[position].getName()));
                                                } else {
                                                    startActivity(new Intent(getApplicationContext(), Info.class)
                                                            .putExtra("url", "https://www.bing.com/search?q=" + astro[position])
                                                            .putExtra("astro", astro[position]));
                                                }
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
                        });
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                if (!intent) {
                    Toast.makeText(PeopleinSpace.this, "Either you have no connection or server is overloaded.", Toast.LENGTH_LONG).show();
                    endAnimation();
                }
            }
        });

        // If AlertPeople.java called us
        if (requestQueue == null) {
            RequestQueue requestQueue = Volley.newRequestQueue(applicationContext);
            requestQueue.add(jsonObjectRequest);
        } else {
            jsonObjectRequest.setTag(TAG);
            requestQueue.add(jsonObjectRequest);
        }

        // Save the most recent grab of astro's on duty.
        if (!intent) {
            sharedPref.edit().putString(getString(R.string.astro_detail), astro_detail.toString()).apply();
        }
        return astro_detail.toString();
    }

    void startAnimation() {
        View view = findViewById(R.id.avloadingIndicatorView);

        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }

    void endAnimation() {
        View view = findViewById(R.id.avloadingIndicatorView);

        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }
}
