import android.content.Intent;
package com.example.webviewstarter;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.net.Uri;
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

    // Replace with a website you own or are authorized to package.
    private static final String START_URL = "https://netmirror.plus/";

    private WebView webView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout layout = new FrameLayout(this);

        webView = new WebView(this);
        progressBar = new ProgressBar(
                this, null,
                android.R.attr.progressBarStyleHorizontal
        );

        progressBar.setMax(100);

        layout.addView(webView,
                new FrameLayout.LayoutParams(-1, -1));

        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(-1, 5);

        layout.addView(progressBar, params);

        setContentView(layout);

        // Cookie support
        CookieManager cookieManager =
                CookieManager.getInstance();

        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // WebView settings
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setSupportZoom(true);
        settings.setAllowFileAccess(false);

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(
                    WebView view, int progress) {

                progressBar.setProgress(progress);

                progressBar.setVisibility(
                        progress >= 100 ? View.GONE : View.VISIBLE
                );
            }

            // Handle links requesting a new window.
            @Override
            public boolean onCreateWindow(
                    WebView view,
                    boolean isDialog,
                    boolean isUserGesture,
                    android.os.Message resultMsg) {

                if (!isUserGesture) {
                    return false;
                }

                WebView popupWebView = new WebView(MainActivity.this);

                popupWebView.getSettings().setJavaScriptEnabled(true);
                popupWebView.getSettings().setDomStorageEnabled(true);

                popupWebView.setWebViewClient(new WebViewClient() {

                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            WebResourceRequest request) {
                    
                        Uri uri = request.getUrl();
                    
                        if ("https".equalsIgnoreCase(uri.getScheme()) ||
                            "http".equalsIgnoreCase(uri.getScheme())) {
                    
                            try {
                                Intent intent = new Intent(
                                    Intent.ACTION_VIEW,
                                    uri
                                );
                                startActivity(intent);
                            } catch (Exception e) {
                                Toast.makeText(
                                    MainActivity.this,
                                    "No browser available",
                                    Toast.LENGTH_SHORT
                                ).show();
                            }
                    
                            return true;
                        }
                    
                        return true;
                    });

                WebView.WebViewTransport transport =
                        (WebView.WebViewTransport) resultMsg.obj;

                transport.setWebView(popupWebView);
                resultMsg.sendToTarget();

                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request) {

                String scheme = request.getUrl().getScheme();

                return !("https".equalsIgnoreCase(scheme)
                        || "http".equalsIgnoreCase(scheme));
            }

            @Override
            public void onPageStarted(
                    WebView view, String url, Bitmap favicon) {

                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(
                    WebView view, String url) {

                progressBar.setVisibility(View.GONE);
                CookieManager.getInstance().flush();
            }
        });

        if (savedInstanceState == null) {
            webView.loadUrl(START_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private void popupWebViewCleanup(WebView view) {
        view.stopLoading();
        view.destroy();
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
    protected void onSaveInstanceState(Bundle outState) {

        if (webView != null) {
            webView.saveState(outState);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {

        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }

        super.onDestroy();
    }
}
