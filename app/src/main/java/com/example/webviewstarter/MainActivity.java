package com.example.webviewstarter;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
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

    private FrameLayout rootLayout;
    private WebView webView;
    private ProgressBar progressBar;

    // Fullscreen (HTML5 video) state
    private View fullscreenView;
    private FrameLayout fullscreenContainer;
    private WebChromeClient.CustomViewCallback fullscreenCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rootLayout = new FrameLayout(this);

        webView = new WebView(this);

        progressBar = new ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
        );

        progressBar.setMax(100);

        rootLayout.addView(
                webView,
                new FrameLayout.LayoutParams(-1, -1)
        );

        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(-1, 5);

        rootLayout.addView(progressBar, params);

        setContentView(rootLayout);

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

        // Never allow popup windows — window.open() calls are
        // cancelled in onCreateWindow() below.
        settings.setSupportMultipleWindows(false);

        // Zoom support
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        settings.setAllowFileAccess(false);

        // Progress bar + fullscreen video support + popup blocking
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

            // Block ALL window.open() popup windows
            @Override
            public boolean onCreateWindow(
                    WebView view,
                    boolean isDialog,
                    boolean isUserGesture,
                    Message resultMsg) {

                return false;
            }

            // Enter fullscreen video
            @Override
            public void onShowCustomView(
                    View view,
                    CustomViewCallback callback) {

                if (fullscreenView != null) {
                    callback.onCustomViewHidden();
                    return;
                }

                fullscreenView = view;
                fullscreenCallback = callback;

                fullscreenContainer = new FrameLayout(
                        MainActivity.this
                );
                fullscreenContainer.setBackgroundColor(Color.BLACK);
                fullscreenContainer.addView(
                        view,
                        new FrameLayout.LayoutParams(-1, -1)
                );

                rootLayout.addView(
                        fullscreenContainer,
                        new FrameLayout.LayoutParams(-1, -1)
                );

                hideSystemUi();
            }

            // Exit fullscreen video
            @Override
            public void onHideCustomView() {

                if (fullscreenView == null) {
                    return;
                }

                showSystemUi();

                rootLayout.removeView(fullscreenContainer);
                fullscreenContainer = null;
                fullscreenView = null;

                if (fullscreenCallback != null) {
                    fullscreenCallback.onCustomViewHidden();
                    fullscreenCallback = null;
                }
            }
        });

        // Navigation handling — block ads/popups, keep everything
        // else inside this WebView (never external browser)
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request) {

                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                String host = uri.getHost();

                if ("http".equalsIgnoreCase(scheme)
                        || "https".equalsIgnoreCase(scheme)) {

                    if (host != null) {

                        String lowerHost = host.toLowerCase();

                        // 1. Blocklisted ad/popup domains
                        for (String blocked : BLOCKED_DOMAINS) {

                            if (lowerHost.equals(blocked)
                                    || lowerHost.endsWith(
                                            "." + blocked)) {

                                return true; // cancel
                            }
                        }

                        // 2. Block automatic redirects to a
                        // DIFFERENT host with no user gesture —
                        // this is how popup chains escape.
                        if (!request.hasGesture()) {

                            String currentHost = null;

                            if (view.getUrl() != null) {
                                currentHost = Uri.parse(
                                        view.getUrl()
                                ).getHost();
                            }

                            if (currentHost != null
                                    && !lowerHost.equalsIgnoreCase(
                                            currentHost)) {

                                return true; // cancel popup redirect
                            }
                        }
                    }

                    // Load everything else inside the app
                    return false;
                }

                // Block intent:// URLs from opening external apps.
                // Extract the browser_fallback_url if present and
                // load it inside the WebView instead.
                if ("intent".equalsIgnoreCase(scheme)) {

                    try {
                        String fallback = uri.getQueryParameter(
                                "browser_fallback_url"
                        );

                        if (fallback != null) {

                            Uri fallbackUri = Uri.parse(fallback);

                            String fbScheme = fallbackUri.getScheme();

                            if ("http".equalsIgnoreCase(fbScheme)
                                    || "https".equalsIgnoreCase(
                                            fbScheme)) {

                                view.loadUrl(fallback);
                            }
                        }

                    } catch (Exception ignored) {
                    }

                    return true; // never hand off to external app
                }

                // All other non-web schemes — do nothing
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

    private void hideSystemUi() {

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }

    private void showSystemUi() {

        getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_VISIBLE
        );
    }

    // Android back button
    @Override
    public void onBackPressed() {

        // Exit fullscreen video first
        if (fullscreenView != null) {

            webView.getWebChromeClient().onHideCustomView();
            return;
        }

        if (webView != null && webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
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
