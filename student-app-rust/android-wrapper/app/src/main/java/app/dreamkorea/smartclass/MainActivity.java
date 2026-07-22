package app.dreamkorea.smartclass;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.view.WindowManager;
import android.graphics.Color;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen immersive
        getWindow().setStatusBarColor(Color.parseColor("#0B1120"));
        getWindow().setNavigationBarColor(Color.parseColor("#0B1120"));

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setAllowFileAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Keep links inside the app (no external browser)
        webView.setWebViewClient(new WebViewClient());
        webView.setBackgroundColor(Color.parseColor("#0B1120"));

        // Load the DreamKorea SmartClass web app
        webView.loadUrl("http://eduplatform-alb-606377009.ap-south-1.elb.amazonaws.com/");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
