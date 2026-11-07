
package com.example.webviewstarter;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity {

    // Use a website you own or are authorized to package.
    private static final String START_URL = "https://netmirror.plus/";

    private WebView webView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout layout = new FrameLayout(this);

        webView = new WebView(this);

        progressBar = new ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
        );

        progressBar.setMax(100);

        layout.addView(
                webView,
                new FrameLayout.LayoutParams(-1, -1)
        );

        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(-1, 5);

        layout.addView(progressBar, params);

        setContentView(layout);

        // Cookie settings
        CookieManager cookieManager =
                CookieManager.getInstance();

        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(
                webView, true
        );

        // Browser settings
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(false);
        settings.setAllowFileAccess(false);

        // Progress bar
        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(
                    WebView view, int progress) {

                progressBar.setProgress(progress);

                progressBar.setVisibility(
                        progress >= 100
                                ? View.GONE
                                : View.VISIBLE
                );
            }
        });

        // Navigation handling
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request) {

                Uri uri = request.getUrl();

                String scheme = uri.getScheme();

                if ("http".equalsIgnoreCase(scheme)
                        || "https".equalsIgnoreCase(scheme)) {

                    String host = uri.getHost();

                    // Keep same website navigation in the app.
                    if (host != null &&
                            host.equalsIgnoreCase(
                                    Uri.parse(START_URL).getHost())) {
                        return false;
                    }

                    // Open other websites externally.
                    try {
                        Intent intent = new Intent(
                                Intent.ACTION_VIEW, uri
                        );

                        startActivity(intent);

                    } catch (Exception e) {

                        Toast.makeText(
                                MainActivity.this,
                                "No supported browser found",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    return true;
                }

                return true;
            }

            @Override
            public void onPageStarted(
                    WebView view,
                    String url,
                    Bitmap favicon) {

                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(
                    WebView view,
                    String url) {

                progressBar.setVisibility(View.GONE);
                CookieManager.getInstance().flush();
            }
        });

        // Load website
        if (savedInstanceState == null) {

            webView.loadUrl(START_URL);

        } else {

            webView.restoreState(savedInstanceState);
        }
    }

    // Android back button
    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }

    // Save page state
    @Override
    protected void onSaveInstanceState(Bundle outState) {

        if (webView != null) {
            webView.saveState(outState);
        }

        super.onSaveInstanceState(outState);
    }

    // Cleanup
    @Override
    protected void onDestroy() {

        if (webView != null) {

            webView.stopLoading();
            webView.destroy();
        }

        super.onDestroy();
    }
}
