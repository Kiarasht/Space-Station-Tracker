package com.restart.spacestationtracker;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class LiveStream extends Activity {
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_layout);

        findViewById(R.id.adView).setVisibility(View.GONE);
        mWebView = (WebView) findViewById(R.id.webView2);

        if (mWebView != null) {
            mWebView.loadUrl("http://www.ustream.tv/channel/iss-hdev-payload/pop-out");
            mWebView.getSettings().setJavaScriptEnabled(true);
            mWebView.setVerticalScrollBarEnabled(false);
            mWebView.setWebViewClient(new MyWebViewClient());
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
}

