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

        mWebView = findViewById(R.id.webView);

        if (mWebView != null) {
            mWebView.loadUrl("https://www.youtube.com/embed/DIgkvm2nmHc");
            mWebView.getSettings().setJavaScriptEnabled(true);
            mWebView.setVerticalScrollBarEnabled(false);

            if (Build.VERSION.SDK_INT <= 23) {
                mWebView.setWebViewClient(new MyWebViewClient());
            } else {
                mWebView.setWebViewClient(new MyWebViewClientNougat());
            }

            // Auto play if above Jelly
            mWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        } else {
            Toast.makeText(getApplicationContext(), R.string.errorWebView, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads WebView inside the app instead of starting phone's default browser
     */
    private static class MyWebViewClient extends WebViewClient {
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
    private static class MyWebViewClientNougat extends WebViewClient {
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
            if (keyCode == KeyEvent.KEYCODE_BACK) {
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

