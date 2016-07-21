package com.restart.spacestationtracker;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

public class AdPreference extends Preference {

    public AdPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public AdPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AdPreference(Context context) {
        super(context);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);

        Activity activity = (Activity) getContext();

        AdView adView = new AdView(activity);
        adView.setAdSize(AdSize.SMART_BANNER);
        adView.setAdUnitId(activity.getString(R.string.banner_ad_unit_id));

        ((LinearLayout) view).addView(adView);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean advertisement = sharedPreferences.getBoolean("advertisement", false);

        if (!advertisement) {
            AdRequest adRequest = new AdRequest.Builder().addTestDevice("998B51E0DA18B35E1A4C4E6D78084ABB").build();
            adView.loadAd(adRequest);
        }

        return view;
    }
}
