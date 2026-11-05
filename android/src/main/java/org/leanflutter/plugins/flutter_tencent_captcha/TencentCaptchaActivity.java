package org.leanflutter.plugins.flutter_tencent_captcha;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

public class TencentCaptchaActivity extends Activity {
    private static WeakReference<TencentCaptchaActivity> current = new WeakReference<>(null);

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    // 是否已回傳結果；未回傳就被關閉（系統返回鍵等）時，onDestroy 補送 onFail，避免呼叫端永遠等不到結果
    private boolean resultSent = false;
    private WebView webView;
    private String configJsonString = "{}";

    static void finishCurrent() {
        TencentCaptchaActivity activity = current.get();
        if (activity != null) {
            activity.closeActivity();
        }
    }

    // captcha.html 通过 window.messageHandlers[method](json) 回调
    private class JsInterface {
        @JavascriptInterface
        public void onLoaded(final String data) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    TencentCaptchaSender.getInstance().onLoaded(data);
                }
            });
        }

        @JavascriptInterface
        public void onSuccess(final String data) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    resultSent = true;
                    TencentCaptchaSender.getInstance().onSuccess(data);
                    closeActivity();
                }
            });
        }

        @JavascriptInterface
        public void onFail(final String data) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    resultSent = true;
                    TencentCaptchaSender.getInstance().onFail(data);
                    closeActivity();
                }
            });
        }
    }

    private final WebViewClient webViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            view.evaluateJavascript("window._verify(" + JSONObject.quote(configJsonString) + ");", null);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String scheme = uri.getScheme();
            // 验证码内的帮助/反馈链接交给外部浏览器打开
            if ("http".equals(scheme) || "https".equals(scheme)) {
                view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }
            return false;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.cancel();
        }
    };

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        current = new WeakReference<>(this);

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#99000000"));

        Intent intent = getIntent();
        String config = intent.getStringExtra("configJsonString");
        if (config != null) {
            this.configJsonString = config;
        }

        this.webView = new WebView(this);
        this.webView.setBackgroundColor(Color.TRANSPARENT);
        this.webView.setWebViewClient(this.webViewClient);
        this.webView.addJavascriptInterface(new JsInterface(), "messageHandlers");

        WebSettings webSettings = this.webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        this.setContentView(this.webView);
        this.webView.requestFocus();

        String captchaHtmlPath = intent.getStringExtra("captchaHtmlPath");
        if (captchaHtmlPath != null) {
            this.webView.loadUrl("file:///android_asset/" + captchaHtmlPath);
        }
    }

    private void closeActivity() {
        if (!isFinishing()) {
            finish();
            overridePendingTransition(0, 0);
        }
    }

    @Override
    protected void onPause() {
        overridePendingTransition(0, 0);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // isFinishing 排除設定變更造成的重建
        if (!resultSent && isFinishing()) {
            resultSent = true;
            // 與 JS 端使用者關閉（ret=2）同形
            TencentCaptchaSender.getInstance().onFail("{\"ret\":2}");
        }
        if (current.get() == this) {
            current.clear();
        }
        if (this.webView != null) {
            this.webView.destroy();
            this.webView = null;
        }
        super.onDestroy();
    }
}
