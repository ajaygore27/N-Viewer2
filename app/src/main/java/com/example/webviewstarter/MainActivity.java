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

import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {

    // Use a website you own or are authorized to package.
    private static final String START_URL = "https://netmirror.plus/";

    // Popup / ad domains to block.
    // Add new popup domains here as they appear.
    private static final List<String> BLOCKED_DOMAINS = Arrays.asList(
            "ay267.com",
            "doubleclick.net",
            "googlesyndication.com",
            "adservice.google.com",
            "amazon-adsystem.com",
            "adnxs.com",
            "popads.net",
            "popcash.net",
            "onclickads.net",
            "redirect-ads.com"
    );

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

        // Spoof a regular Chrome browser User-Agent
        // so the site doesn't detect WebView and block it.
        // No "; wv" token and no "Version/4.0" token.
        settings.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Mobile Safari/537.36"
        );

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);

        // Zoom support — pinch to zoom works
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

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

                    // Block popup / ad domains
                    String host = uri.getHost();

                    if (host != null) {

                        String lowerHost = host.toLowerCase();

                        for (String blocked : BLOCKED_DOMAINS) {

                            if (lowerHost.equals(blocked)
                                    || lowerHost.endsWith(
                                            "." + blocked)) {

                                // Cancel — don't load the ad
                                return true;
                            }
                        }
                    }

                    // Load everything else inside this WebView —
                    // never open external browser.
                    return false;
                }

                // Non-web schemes (intent:, mailto:, tel:, etc.)
                try {
                    Intent intent = new Intent(
                            Intent.ACTION_VIEW, uri
                    );

                    startActivity(intent);

                } catch (Exception e) {

                    Toast.makeText(
                            MainActivity.this,
                            "No supported app found",
                            Toast.LENGTH_SHORT
                    ).show();
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
