package com.restart.spacestationtracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ObservableWebView;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class Info extends AppCompatActivity implements ObservableScrollViewCallbacks {
    private ObservableWebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_layout);

        mWebView = (ObservableWebView) findViewById(R.id.webView2);
        mWebView.setScrollViewCallbacks(this);

        if (mWebView != null) {
            Intent intent = getIntent();
            if (intent.getStringExtra("astro") != null) {
                setTitle(intent.getStringExtra("astro"));
            }
            mWebView.loadUrl(intent.getStringExtra("url"));
            mWebView.getSettings().setJavaScriptEnabled(true);
            mWebView.setVerticalScrollBarEnabled(false);
            mWebView.setWebViewClient(new MyWebViewClient());
        } else {
            Toast.makeText(getApplicationContext(), "Unable to load page", Toast.LENGTH_SHORT).show();
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
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
     * Loads WebView inside the app instead of starting phone's default browser
     */
    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    /**
     * Navigates back through the WebView history
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (mWebView.canGoBack()) {
                        mWebView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {

    }

    @Override
    public void onDownMotionEvent() {

    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {
        ActionBar ab = getSupportActionBar();
        if (ab == null) {
            return;
        }
        if (scrollState == ScrollState.UP) {
            if (ab.isShowing()) {
                ab.hide();
            }
        } else if (scrollState == ScrollState.DOWN) {
            if (!ab.isShowing()) {
                ab.show();
            }
        }
    }
}
