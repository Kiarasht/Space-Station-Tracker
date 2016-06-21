package com.restart.spacestationtracker;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
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

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PeopleinSpace extends AppCompatActivity {

    private RequestQueue requestQueue;
    private SharedPreferences sharedPref;
    private TextView people_number;
    private TextView people_detail;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_locations);
        startAnimation();
        sharedPref = getSharedPreferences("savefile", MODE_PRIVATE);
        people_number = (TextView) findViewById(R.id.textView2);
        people_detail = (TextView) findViewById(R.id.textView3);
        requestQueue = Volley.newRequestQueue(this);
        display_people(false, getApplicationContext());

        if (!sharedPref.getBoolean(getString(R.string.notificationcheck3), false)) {
            AdView adView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            if (adView != null) {
                adView.loadAd(adRequest);
            }
        }
    }

    /**
     * Finds a string that holds the values of people in space.
     *
     * @param intent Is the function getting called from a service or from the activity? If its a
     *               service then we don't to update objects such as textboxes.
     * @param applicationContext Used if coming from a service
     * @return Return a string variable holding the astro people.
     */
    public String display_people(final Boolean intent, Context applicationContext) {
        String url = "http://api.open-notify.org/astros.json";
        final StringBuilder astro_detail = new StringBuilder();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray results = response.getJSONArray("people");
                    int numbers = response.getInt("number");
                    final String astro_number = "Currently " + numbers + " People In Space";

                    for (int i = 0; i < results.length(); i += 1) {
                        JSONObject result = results.getJSONObject(i);
                        astro_detail.append(i + 1)
                                .append(". ")
                                .append(String.valueOf(result.getString("name")))
                                .append(" at ")
                                .append(String.valueOf(result.getString("craft")))
                                .append(".\n\n");
                    }

                    if (!intent) {
                        PeopleinSpace.this.runOnUiThread(new Runnable() {
                            public void run() {
                                people_number.setText(astro_number);
                                people_detail.setVisibility(View.VISIBLE);
                                people_detail.setText(astro_detail);
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
                e.printStackTrace();
                NetworkResponse networkResponse = e.networkResponse;

                if (networkResponse != null && networkResponse.statusCode == HttpStatus.SC_UNAUTHORIZED) {
                    int error = networkResponse.statusCode;
                    String message = e.getMessage();
                    String reason = message + " Error: " + error;
                    Toast.makeText(PeopleinSpace.this, reason + ".", Toast.LENGTH_LONG).show();

                    return;
                }

                Toast.makeText(PeopleinSpace.this, "Either you have no connection or server is overloaded.", Toast.LENGTH_LONG).show();
                endAnimation();
            }
        });

        if (requestQueue == null) {
            RequestQueue requestQueue = Volley.newRequestQueue(applicationContext);
            requestQueue.add(jsonObjectRequest);
        } else {
            requestQueue.add(jsonObjectRequest);
        }

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
