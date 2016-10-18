package com.restart.spacestationtracker;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class LiveStream extends Activity {

    private WebView mWebView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_layout);

        mWebView = (WebView) findViewById(R.id.webView2);

        if (mWebView != null) {
            mWebView.loadUrl("http://www.ustream.tv/channel/iss-hdev-payload/pop-out");
            mWebView.getSettings().setJavaScriptEnabled(true);
            mWebView.setVerticalScrollBarEnabled(false);

            if (Build.VERSION.SDK_INT <= 23) {
                mWebView.setWebViewClient(new MyWebViewClient());
            } else {
                mWebView.setWebViewClient(new MyWebViewClientNougat());
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) { // Auto play if above Jelly
                mWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
            }
        } else {
            Toast.makeText(getApplicationContext(), "Unable to load page", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads WebView inside the app instead of starting phone's default browser
     */
    @SuppressWarnings("deprecation")
    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    /**
     * MyWebViewClient for Android Nougat
     */
    @TargetApi(Build.VERSION_CODES.N)
    private class MyWebViewClientNougat extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            view.loadUrl(request.getUrl().toString());
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
}

