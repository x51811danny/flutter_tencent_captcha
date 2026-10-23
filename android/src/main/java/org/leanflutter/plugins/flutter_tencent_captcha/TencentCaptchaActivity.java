package org.leanflutter.plugins.flutter_tencent_captcha;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.tencent.captcha.sdk.RetCode;
import com.tencent.captcha.sdk.TencentCaptcha;
import com.tencent.captcha.sdk.TencentCaptchaCallback;
import com.tencent.captcha.sdk.TencentCaptchaParam;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class TencentCaptchaActivity extends Activity {
    private static final String TAG = "TencentCaptchaActivity";

    private WebView webView;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean finishedVerify = false;

    @SuppressLint({"SetJavaScriptEnabled"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#99000000"));
        }

        this.webView = new WebView(this);
        this.webView.setBackgroundColor(Color.TRANSPARENT);
        this.webView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        this.setContentView(this.webView);

        Intent intent = getIntent();
        String fallbackAppId = intent.getStringExtra("appId");
        String configJsonString = intent.getStringExtra("configJsonString");

        startTencentCaptcha(fallbackAppId, configJsonString);
    }

    private void startTencentCaptcha(String fallbackAppId, String configJsonString) {
        JSONObject configJson = new JSONObject();
        if (configJsonString != null && !configJsonString.isEmpty()) {
            try {
                configJson = new JSONObject(configJsonString);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse configJsonString", e);
            }
        }

        String captchaAppId = configJson.optString("appId", "");
        if (captchaAppId.isEmpty() && fallbackAppId != null) {
            captchaAppId = fallbackAppId;
        }

        TencentCaptchaParam.Builder paramBuilder = new TencentCaptchaParam.Builder()
                .setWebView(this.webView)
                .setCaptchaAppid(captchaAppId);

        if (configJson.has("bizState") && !configJson.isNull("bizState")) {
            paramBuilder.setBizState(configJson.opt("bizState"));
        }

        if (configJson.has("enableDarkMode") && !configJson.isNull("enableDarkMode")) {
            Object darkModeObj = configJson.opt("enableDarkMode");
            if (darkModeObj instanceof Boolean) {
                paramBuilder.setEnableDarkMode((Boolean) darkModeObj);
            } else if (darkModeObj instanceof String) {
                paramBuilder.setEnableDarkMode((String) darkModeObj);
            }
        }

        if (configJson.has("needFeedBack") && !configJson.isNull("needFeedBack")) {
            Object feedBackObj = configJson.opt("needFeedBack");
            if (feedBackObj instanceof Boolean) {
                paramBuilder.setNeedFeedBack((Boolean) feedBackObj);
            } else if (feedBackObj instanceof String) {
                paramBuilder.setNeedFeedBack((String) feedBackObj);
            }
        }

        if (configJson.has("userLanguage") && !configJson.isNull("userLanguage")) {
            paramBuilder.setUserLanguage(configJson.optString("userLanguage"));
        }

        if (configJson.has("enableAged") && !configJson.isNull("enableAged")) {
            paramBuilder.setEnableAged(configJson.optBoolean("enableAged"));
        }

        if (configJson.has("type") && !configJson.isNull("type")) {
            paramBuilder.setType(configJson.optString("type"));
        }

        if (configJson.has("loading") && !configJson.isNull("loading")) {
            paramBuilder.setLoading(configJson.optBoolean("loading"));
        }

        if (configJson.has("aidEncrypted") && !configJson.isNull("aidEncrypted")) {
            paramBuilder.setAidEncrypted(configJson.optString("aidEncrypted"));
        }

        if (configJson.has("captchaPageHost") && !configJson.isNull("captchaPageHost")) {
            paramBuilder.setCaptchaPageHost(configJson.optString("captchaPageHost"));
        }

        if (configJson.has("captchaTimeoutMillis") && !configJson.isNull("captchaTimeoutMillis")) {
            paramBuilder.setCaptchaTimeoutMillis(configJson.optInt("captchaTimeoutMillis"));
        }

        if (configJson.has("enableAutoCheck") && !configJson.isNull("enableAutoCheck")) {
            paramBuilder.setExtractParameter("enableAutoCheck", configJson.optBoolean("enableAutoCheck"));
        }

        if (configJson.has("extractParameters") && !configJson.isNull("extractParameters")) {
            JSONObject extractParams = configJson.optJSONObject("extractParameters");
            if (extractParams != null) {
                Iterator<String> keys = extractParams.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    paramBuilder.setExtractParameter(key, extractParams.opt(key));
                }
            }
        }

        paramBuilder.setOptionsCallback(new TencentCaptchaParam.OptionsCallback() {
            @Override
            public void onCallback(final String optFuncName, final String data) {
                if ("ready".equals(optFuncName)) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            TencentCaptchaSender.getInstance().onLoaded(data != null ? data : "{}");
                        }
                    });
                }
            }
        });

        TencentCaptchaCallback callback = new TencentCaptchaCallback() {
            @Override
            public void finish(final RetCode ret, final JSONObject resultObject) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        finishedVerify = true;
                        if (ret == RetCode.OK && resultObject != null) {
                            int verifyRet = resultObject.optInt("ret", -1);
                            if (verifyRet == 0) {
                                TencentCaptchaSender.getInstance().onSuccess(resultObject.toString());
                            } else {
                                TencentCaptchaSender.getInstance().onFail(resultObject.toString());
                            }
                        } else {
                            JSONObject errJson = resultObject != null ? resultObject : new JSONObject();
                            try {
                                if (ret != null) {
                                    errJson.put("retCode", ret.getCode());
                                    errJson.put("retMsg", ret.getMsg());
                                    if (!errJson.has("errorCode")) {
                                        errJson.put("errorCode", ret.getCode());
                                    }
                                    if (!errJson.has("errorMessage")) {
                                        errJson.put("errorMessage", ret.getMsg());
                                    }
                                }
                            } catch (JSONException ignored) {
                            }
                            TencentCaptchaSender.getInstance().onFail(errJson.toString());
                        }
                        closeActivity();
                    }
                });
            }

            @Override
            public void exception(final Throwable t) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        finishedVerify = true;
                        JSONObject errJson = new JSONObject();
                        try {
                            errJson.put("retCode", RetCode.CAPTCHA_EXCEPTION.getCode());
                            errJson.put("errorMessage", t != null ? t.getMessage() : "Unknown exception");
                        } catch (JSONException ignored) {
                        }
                        TencentCaptchaSender.getInstance().onFail(errJson.toString());
                        closeActivity();
                    }
                });
            }
        };

        RetCode startRet = TencentCaptcha.start(callback, paramBuilder.build());
        if (startRet != RetCode.OK) {
            finishedVerify = true;
            JSONObject errJson = new JSONObject();
            try {
                errJson.put("retCode", startRet.getCode());
                errJson.put("retMsg", startRet.getMsg());
                errJson.put("errorCode", startRet.getCode());
                errJson.put("errorMessage", startRet.getMsg());
            } catch (JSONException ignored) {
            }
            TencentCaptchaSender.getInstance().onFail(errJson.toString());
            closeActivity();
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
        TencentCaptcha.stop();
    }

    @Override
    protected void onDestroy() {
        if (!finishedVerify) {
            TencentCaptcha.stop();
        }
        if (this.webView != null) {
            this.webView.destroy();
            this.webView = null;
        }
        super.onDestroy();
    }
}
