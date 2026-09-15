package com.cscaprep.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String HOME_URL = "https://cscaprep.com/";
    private static final int FILE_CHOOSER_REQUEST = 501;
    private static final int STORAGE_REQUEST = 502;

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout errorView;
    private ValueCallback<Uri[]> fileCallback;
    private Runnable pendingStorageAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CookieManager.getInstance().setAcceptCookie(true);
        buildInterface();
        configureWebView(webView, true);

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(startUrl(getIntent()));
        }
    }

    private void buildInterface() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(244, 249, 253));

        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(3)
        );
        progressParams.gravity = Gravity.TOP;
        root.addView(progressBar, progressParams);

        errorView = new LinearLayout(this);
        errorView.setOrientation(LinearLayout.VERTICAL);
        errorView.setGravity(Gravity.CENTER);
        errorView.setPadding(dp(28), dp(28), dp(28), dp(28));
        errorView.setBackgroundColor(Color.rgb(244, 249, 253));

        TextView mark = new TextView(this);
        mark.setText("C");
        mark.setTextSize(44);
        mark.setTextColor(Color.WHITE);
        mark.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable markBackground = new android.graphics.drawable.GradientDrawable();
        markBackground.setColor(Color.rgb(19, 120, 185));
        markBackground.setCornerRadius(dp(22));
        mark.setBackground(markBackground);
        errorView.addView(mark, new LinearLayout.LayoutParams(dp(78), dp(78)));

        TextView title = new TextView(this);
        title.setText(R.string.connection_error);
        title.setTextSize(24);
        title.setTextColor(Color.rgb(32, 35, 40));
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.topMargin = dp(22);
        errorView.addView(title, titleParams);

        TextView help = new TextView(this);
        help.setText(R.string.connection_help);
        help.setTextSize(15);
        help.setTextColor(Color.rgb(104, 114, 126));
        help.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams helpParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        helpParams.topMargin = dp(9);
        errorView.addView(help, helpParams);

        Button retry = new Button(this);
        retry.setText(R.string.retry);
        retry.setTextColor(Color.WHITE);
        android.graphics.drawable.GradientDrawable buttonBackground = new android.graphics.drawable.GradientDrawable();
        buttonBackground.setColor(Color.rgb(19, 120, 185));
        buttonBackground.setCornerRadius(dp(28));
        retry.setBackground(buttonBackground);
        retry.setOnClickListener(view -> {
            hideError();
            webView.reload();
        });
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(dp(170), dp(52));
        retryParams.topMargin = dp(24);
        errorView.addView(retry, retryParams);

        errorView.setVisibility(View.GONE);
        root.addView(errorView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setContentView(root);
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void configureWebView(WebView target, boolean main) {
        WebSettings settings = target.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUserAgentString(settings.getUserAgentString() + " CSCAPrepAndroid/1.0");

        CookieManager.getInstance().setAcceptThirdPartyCookies(target, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WebView.enableSlowWholeDocumentDraw();
            settings.setSafeBrowsingEnabled(true);
        }

        if (main) target.addJavascriptInterface(new AndroidBridge(), "CSCAPrepAndroid");
        target.setWebViewClient(new CSCAPrepWebViewClient(main));
        target.setWebChromeClient(new CSCAPrepChromeClient(main));
        target.setDownloadListener(new CSCAPrepDownloadListener());
    }

    private class CSCAPrepWebViewClient extends WebViewClient {
        private final boolean main;

        CSCAPrepWebViewClient(boolean main) {
            this.main = main;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return routeUri(view, request.getUrl(), main);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return routeUri(view, Uri.parse(url), main);
        }

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            if (main) hideError();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            CookieManager.getInstance().flush();
            if (main || isCSCAPrepHost(Uri.parse(url).getHost())) injectAndroidFeatures(view);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (main && request.isForMainFrame()) showError();
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
            if (main && request.isForMainFrame() && response.getStatusCode() >= 500) showError();
        }

        @Override
        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            if (main) {
                ((ViewGroup) view.getParent()).removeView(view);
                view.destroy();
                webView = new WebView(MainActivity.this);
                ((ViewGroup) errorView.getParent()).addView(webView, 0, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));
                configureWebView(webView, true);
                webView.loadUrl(HOME_URL);
            }
            return true;
        }
    }

    private class CSCAPrepChromeClient extends WebChromeClient {
        private final boolean main;

        CSCAPrepChromeClient(boolean main) {
            this.main = main;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (!main) return;
            progressBar.setProgress(newProgress);
            progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
        }

        @Override
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
            if (fileCallback != null) fileCallback.onReceiveValue(null);
            fileCallback = callback;
            Intent intent;
            try {
                intent = params.createIntent();
            } catch (Exception exception) {
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
            }
            try {
                startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                return true;
            } catch (Exception exception) {
                fileCallback = null;
                Toast.makeText(MainActivity.this, "No file picker is available.", Toast.LENGTH_LONG).show();
                return false;
            }
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
            Dialog dialog = new Dialog(MainActivity.this, android.R.style.Theme_Material_Light_NoActionBar);
            WebView popup = new WebView(MainActivity.this);
            configureWebView(popup, false);
            popup.setWebChromeClient(new CSCAPrepChromeClient(false) {
                @Override
                public void onCloseWindow(WebView window) {
                    dialog.dismiss();
                }
            });
            dialog.setContentView(popup);
            Window window = dialog.getWindow();
            if (window != null) window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.setOnDismissListener(listener -> popup.destroy());
            dialog.show();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(popup);
            resultMsg.sendToTarget();
            return true;
        }
    }

    private boolean routeUri(WebView view, Uri uri, boolean mainWindow) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.US);
        if ((scheme.equals("https") || scheme.equals("http")) && isCSCAPrepHost(uri.getHost())) {
            return false;
        }
        if (!mainWindow && scheme.equals("https") && isGoogleIdentityHost(uri.getHost())) {
            return false;
        }
        if (scheme.equals("about") || scheme.equals("javascript") || scheme.equals("blob") || scheme.equals("data")) {
            return false;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(intent);
        } catch (Exception exception) {
            Toast.makeText(this, "No app can open this link.", Toast.LENGTH_LONG).show();
        }
        return true;
    }

    private boolean isCSCAPrepHost(String host) {
        if (host == null) return false;
        String normalized = host.toLowerCase(Locale.US);
        return normalized.equals("cscaprep.com") || normalized.equals("www.cscaprep.com");
    }

    private boolean isGoogleIdentityHost(String host) {
        if (host == null) return false;
        String normalized = host.toLowerCase(Locale.US);
        return normalized.equals("accounts.google.com") || normalized.endsWith(".accounts.google.com");
    }

    private class CSCAPrepDownloadListener implements DownloadListener {
        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long length) {
            if (url.startsWith("blob:")) {
                downloadBlob(url, mimeType);
            } else if (url.startsWith("data:")) {
                saveDataUrl(url, "CSCAPrep-download", mimeType);
            } else {
                downloadHttp(url, userAgent, contentDisposition, mimeType);
            }
        }
    }

    private void downloadHttp(String url, String userAgent, String contentDisposition, String mimeType) {
        Runnable action = () -> {
            try {
                String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setTitle(filename);
                request.setDescription("Downloading from CSCAPrep");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                if (mimeType != null) request.setMimeType(mimeType);
                if (userAgent != null) request.addRequestHeader("User-Agent", userAgent);
                String cookies = CookieManager.getInstance().getCookie(url);
                if (cookies != null) request.addRequestHeader("Cookie", cookies);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                manager.enqueue(request);
                Toast.makeText(this, "Download started.", Toast.LENGTH_SHORT).show();
            } catch (Exception exception) {
                openExternally(Uri.parse(url));
            }
        };
        withStoragePermission(action);
    }

    private void downloadBlob(String blobUrl, String mimeType) {
        String quotedUrl = org.json.JSONObject.quote(blobUrl);
        String quotedMime = org.json.JSONObject.quote(mimeType == null ? "application/octet-stream" : mimeType);
        String script = "(async function(){try{" +
                "const r=await fetch(" + quotedUrl + ");const b=await r.arrayBuffer();const u=new Uint8Array(b);let s='';" +
                "for(let i=0;i<u.length;i+=8192){s+=String.fromCharCode.apply(null,u.subarray(i,i+8192));}" +
                "CSCAPrepAndroid.saveBase64('CSCAPrep-download'," + quotedMime + ",btoa(s));" +
                "}catch(e){CSCAPrepAndroid.notify('The file could not be downloaded.');}})();";
        webView.evaluateJavascript(script, null);
    }

    private void saveDataUrl(String dataUrl, String fallbackName, String fallbackMime) {
        int comma = dataUrl.indexOf(',');
        if (comma < 0) return;
        String header = dataUrl.substring(0, comma);
        String encoded = dataUrl.substring(comma + 1);
        String mime = header.startsWith("data:") ? header.substring(5).split(";")[0] : fallbackMime;
        new AndroidBridge().saveBase64(fallbackName, mime, encoded);
    }

    private class AndroidBridge {
        @JavascriptInterface
        public void share(String title, String text, String url) {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, title == null ? "CSCAPrep" : title);
                String body = (text == null ? "" : text) + (url == null || url.isEmpty() ? "" : "\n" + url);
                intent.putExtra(Intent.EXTRA_TEXT, body.trim());
                startActivity(Intent.createChooser(intent, "Share with"));
            });
        }

        @JavascriptInterface
        public void saveBase64(String filename, String mimeType, String encoded) {
            runOnUiThread(() -> withStoragePermission(() -> saveDecodedFile(filename, mimeType, encoded)));
        }

        @JavascriptInterface
        public void notify(String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());
        }
    }

    private void saveDecodedFile(String requestedName, String mimeType, String encoded) {
        try {
            byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (extension == null || extension.isEmpty()) extension = "bin";
            String cleanName = requestedName == null ? "CSCAPrep-download" : requestedName.replaceAll("[^A-Za-z0-9._-]", "-");
            if (!cleanName.toLowerCase(Locale.US).endsWith("." + extension)) cleanName += "." + extension;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, cleanName);
                values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri destination = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (destination == null) throw new IllegalStateException("No download destination");
                try (OutputStream output = getContentResolver().openOutputStream(destination)) {
                    if (output == null) throw new IllegalStateException("No output stream");
                    output.write(bytes);
                }
            } else {
                File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!directory.exists() && !directory.mkdirs()) throw new IllegalStateException("Downloads folder unavailable");
                try (FileOutputStream output = new FileOutputStream(new File(directory, cleanName))) {
                    output.write(bytes);
                }
            }
            runOnUiThread(() -> Toast.makeText(this, "Saved to Downloads.", Toast.LENGTH_LONG).show());
        } catch (Exception exception) {
            runOnUiThread(() -> Toast.makeText(this, "The file could not be saved.", Toast.LENGTH_LONG).show());
        }
    }

    private void injectAndroidFeatures(WebView view) {
        String script = "(function(){" +
                "document.documentElement.classList.add('cscaprep-android-app');" +
                "if(window.CSCAPrepAndroid&&!window.__cscaprepAndroidShare){" +
                "window.__cscaprepAndroidShare=true;" +
                "if(!navigator.share){navigator.share=function(d){d=d||{};CSCAPrepAndroid.share(d.title||'CSCAPrep',d.text||'',d.url||'');return Promise.resolve();};}" +
                "}" +
                "})();";
        view.evaluateJavascript(script, null);
    }

    private void withStoragePermission(Runnable action) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingStorageAction = action;
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST);
            return;
        }
        action.run();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_REQUEST) {
            Runnable action = pendingStorageAction;
            pendingStorageAction = null;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && action != null) {
                action.run();
            } else {
                Toast.makeText(this, "Storage permission is required to save this file.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openExternally(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception exception) {
            Toast.makeText(this, "The download could not be opened.", Toast.LENGTH_LONG).show();
        }
    }

    private void showError() {
        errorView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    private void hideError() {
        errorView.setVisibility(View.GONE);
    }

    private String startUrl(Intent intent) {
        Uri data = intent == null ? null : intent.getData();
        return data != null && isCSCAPrepHost(data.getHost()) ? data.toString() : HOME_URL;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        webView.loadUrl(startUrl(intent));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        CookieManager.getInstance().flush();
        webView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
        }
        super.onDestroy();
    }

    @Override
    @Deprecated
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && fileCallback != null) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            fileCallback.onReceiveValue(results);
            fileCallback = null;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
