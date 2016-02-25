package com.restart.spacestationtracker;


import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class PeopleinSpace extends MapsActivity {

    private static final String TAG = ".Locations";
    private TextView people_number;
    private TextView people_detail;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_locations);
        startAnimation();
        people_number = (TextView) findViewById(R.id.textView2);
        people_detail = (TextView) findViewById(R.id.textView3);
        display_people();

        AdView adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().addTestDevice(getString(R.string.deviceid)).build();
        adView.loadAd(adRequest);
    }

    public String display_people() {
        final StringBuilder astro_detail = new StringBuilder();

        AsyncTask.execute(new Runnable() {
            public void run() {
                String strContent = "";

                try {
                    URL urlHandle = new URL("http://api.open-notify.org/astros.json");
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
                    JSONArray results = new JSONObject(strContent).getJSONArray("people");
                    int numbers = new JSONObject(strContent).getInt("number");
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

                    PeopleinSpace.this.runOnUiThread(new Runnable() {
                        public void run() {
                            people_number.setText(astro_number);
                            people_detail.setVisibility(View.VISIBLE);
                            people_detail.setText(astro_detail);
                            endAnimation();
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        sharedPref.edit().putString(getString(R.string.astro_detail), astro_detail.toString()).apply();
        return astro_detail.toString();
    }

    void startAnimation() {
        findViewById(R.id.avloadingIndicatorView).setVisibility(View.VISIBLE);
    }

    void endAnimation() {
        findViewById(R.id.avloadingIndicatorView).setVisibility(View.GONE);
    }
}
