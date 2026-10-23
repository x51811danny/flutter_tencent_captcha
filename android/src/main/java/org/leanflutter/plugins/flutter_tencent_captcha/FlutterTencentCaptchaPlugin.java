package org.leanflutter.plugins.flutter_tencent_captcha;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import com.tencent.captcha.sdk.RetCode;
import com.tencent.captcha.sdk.TencentCaptcha;
import com.tencent.captcha.sdk.TencentCaptchaConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * FlutterTencentCaptchaPlugin
 */
public class FlutterTencentCaptchaPlugin implements FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler, ActivityAware {
    private static final String TAG = "TencentCaptchaPlugin";
    private static final String CHANNEL_NAME = "flutter_tencent_captcha";
    private static final String EVENT_CHANNEL_NAME = "flutter_tencent_captcha/event_channel";

    private MethodChannel channel;
    private EventChannel eventChannel;
    private EventChannel.EventSink eventSink;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Context context;
    private Activity activity;

    private String appId;
    private boolean userAgreement = true;
    private String customAndroidId;
    private boolean sdkInitialized = false;

    private void setupChannel(BinaryMessenger messenger, Context context) {
        this.context = context;

        this.channel = new MethodChannel(messenger, CHANNEL_NAME);
        this.channel.setMethodCallHandler(this);

        this.eventChannel = new EventChannel(messenger, EVENT_CHANNEL_NAME);
        this.eventChannel.setStreamHandler(this);
    }

    private void teardownChannel() {
        this.context = null;

        if (this.channel != null) {
            this.channel.setMethodCallHandler(null);
            this.channel = null;
        }

        if (this.eventChannel != null) {
            this.eventChannel.setStreamHandler(null);
            this.eventChannel = null;
        }
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        this.setupChannel(flutterPluginBinding.getBinaryMessenger(), flutterPluginBinding.getApplicationContext());
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {
        this.teardownChannel();
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding activityPluginBinding) {
        this.activity = activityPluginBinding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        this.activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding activityPluginBinding) {
        this.activity = activityPluginBinding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        this.activity = null;
    }

    @Override
    public void onListen(Object args, EventChannel.EventSink eventSink) {
        this.eventSink = eventSink;
    }

    @Override
    public void onCancel(Object args) {
        this.eventSink = null;
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "getSDKVersion":
                handleMethodGetSDKVersion(call, result);
                break;
            case "init":
                handleMethodInit(call, result);
                break;
            case "verify":
                handleMethodVerify(call, result);
                break;
            case "stop":
                handleMethodStop(call, result);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private void handleMethodGetSDKVersion(@NonNull MethodCall call, @NonNull Result result) {
        result.success("2.1.1");
    }

    private synchronized boolean ensureSdkInitialized() {
        if (sdkInitialized) {
            return true;
        }
        if (context == null) {
            return false;
        }

        final boolean agreed = this.userAgreement;
        final String providedAndroidId = this.customAndroidId;
        final Context appContext = context.getApplicationContext();

        TencentCaptchaConfig.Builder configBuilder = new TencentCaptchaConfig.Builder(
                appContext,
                new TencentCaptchaConfig.ITencentCaptchaPrivacyPolicy() {
                    @Override
                    public boolean userAgreement() {
                        return agreed;
                    }
                },
                new TencentCaptchaConfig.ICaptchaDeviceInfoProvider() {
                    @SuppressLint("HardwareIds")
                    @Override
                    public String getAndroidId() {
                        if (providedAndroidId != null && !providedAndroidId.isEmpty()) {
                            return providedAndroidId;
                        }
                        try {
                            return Settings.Secure.getString(appContext.getContentResolver(), Settings.Secure.ANDROID_ID);
                        } catch (Exception e) {
                            return "";
                        }
                    }
                }
        );

        RetCode ret = TencentCaptcha.init(configBuilder.build());
        if (ret == RetCode.OK) {
            sdkInitialized = true;
            return true;
        } else {
            Log.e(TAG, "TencentCaptcha.init failed, ret code: " + ret.getCode() + " msg: " + ret.getMsg());
            return false;
        }
    }

    private void handleMethodInit(@NonNull MethodCall call, @NonNull Result result) {
        if (call.hasArgument("appId")) {
            this.appId = call.argument("appId");
        }
        if (call.hasArgument("userAgreement")) {
            Boolean agreed = call.argument("userAgreement");
            if (agreed != null) {
                this.userAgreement = agreed;
            }
        }
        if (call.hasArgument("androidId")) {
            this.customAndroidId = call.argument("androidId");
        }

        boolean ok = ensureSdkInitialized();
        result.success(ok);
    }

    private void handleMethodVerify(@NonNull MethodCall call, @NonNull final Result result) {
        if (activity == null) {
            result.error("NO_ACTIVITY", "Plugin is not attached to an Activity.", null);
            return;
        }

        ensureSdkInitialized();

        String configJsonString = "{}";
        if (call.hasArgument("config")) {
            String argConfig = call.argument("config");
            if (argConfig != null) {
                configJsonString = argConfig;
            }
        }

        TencentCaptchaSender.getInstance().listene(new TencentCaptchaListener() {
            @Override
            public void onLoaded(final String data) {
                sendEvent("onLoaded", data);
            }

            @Override
            public void onSuccess(final String data) {
                sendEvent("onSuccess", data);
            }

            @Override
            public void onFail(final String msg) {
                sendEvent("onFail", msg);
            }
        });

        Intent intent = new Intent(activity, TencentCaptchaActivity.class);
        if (this.appId != null) {
            intent.putExtra("appId", this.appId);
        }
        intent.putExtra("configJsonString", configJsonString);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);

        result.success(true);
    }

    private void handleMethodStop(@NonNull MethodCall call, @NonNull Result result) {
        TencentCaptcha.stop();
        result.success(null);
    }

    private void sendEvent(final String method, final String jsonString) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (eventSink == null) {
                    return;
                }
                final Map<String, Object> event = new HashMap<>();
                event.put("method", method);
                event.put("data", convertMsgToMap(jsonString));
                eventSink.success(event);
            }
        });
    }

    private static Map<String, Object> convertMsgToMap(String jsonString) {
        Map<String, Object> map = new HashMap<>();
        if (jsonString == null || jsonString.isEmpty()) {
            return map;
        }
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            map = toMap(jsonObject);
        } catch (JSONException ex) {
            map.put("raw", jsonString);
        }
        return map;
    }

    private static Map<String, Object> toMap(JSONObject jsonObject) throws JSONException {
        Map<String, Object> map = new HashMap<>();
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonObject.get(key);
            if (value == JSONObject.NULL) {
                value = null;
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }
}
