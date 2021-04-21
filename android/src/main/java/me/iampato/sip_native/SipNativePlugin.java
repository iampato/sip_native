package me.iampato.sip_native;

import android.Manifest;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashMap;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * SipNativePlugin
 */

public class SipNativePlugin implements FlutterPlugin, MethodCallHandler, StreamHandler, ActivityAware {
    // global variables
    final private String TAG = "SIP Native";
    private final Handler uiThreadHandler = new Handler(Looper.getMainLooper());
    // initialize the registrationState to unknown then mutate it later after setting up the
    // sipManager.setRegistrationListener
    SipRegistrationState registrationState = SipRegistrationState.UNKNOWN;
    SipCallState sipCallState = SipCallState.UNKNOWN;
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel methodChannel;
    private FlutterPluginBinding pluginBinding = null;
    private ActivityPluginBinding activityPluginBinding;
    private SipDataManager sipDataManager;
    private Utils utils;

    public SipNativePlugin() {
        try {
            System.loadLibrary("openh264");
            // Ticket #1937: libyuv is now included as static lib
            //System.loadLibrary("yuv");
        } catch (UnsatisfiedLinkError e) {
            Log.d(TAG, "UnsatisfiedLinkError: " + e.getMessage());
            Log.d(TAG, "This could be safely ignored if you don't need video.");
        }
        try {
            System.loadLibrary("pjsua2");
            Log.d(TAG, "Library loaded");
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error error) {
            error.printStackTrace();
        }
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        pluginBinding = flutterPluginBinding;

        // initialize sip manager
        if (sipDataManager == null) {
            sipDataManager = new SipDataManager(flutterPluginBinding.getApplicationContext(), uiThreadHandler);
            try {
                sipDataManager.initialize(flutterPluginBinding.getApplicationContext());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // init utils
        if (utils == null) {
            utils = new Utils();
        }
        // setup method channels
        methodChannel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "sip_native/method");
        methodChannel.setMethodCallHandler(this);
        // setup event channels
        EventChannel eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "sip_native/register_events");
        eventChannel.setStreamHandler(this);
        CallsEventChannel callsEventChannel = new CallsEventChannel(sipDataManager, sipCallState);
        EventChannel callsEvent = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "sip_native/calls_events");
        callsEvent.setStreamHandler(callsEventChannel);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "request_permissions":
                try {
                    String[] permissionArrays = new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.USE_SIP,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.MODIFY_AUDIO_SETTINGS,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.CHANGE_NETWORK_STATE,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                    };

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        boolean isSuccess = utils.checkPermissions(permissionArrays, this.activityPluginBinding);
                        if (isSuccess) {
                            result.success(true);
                        } else {
                            result.success(false);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    result.error(null, e.toString(), null);
                }
                break;
            case "prepSip":
                boolean isVoipSupported = sipDataManager.isVoipSupported(this.pluginBinding.getApplicationContext());
                boolean isSipManagerSupported = sipDataManager.isSipManagerSupported(this.pluginBinding.getApplicationContext());
                HashMap<String, Object> arguments = new HashMap<>();
                arguments.put("voip", isVoipSupported);
                arguments.put("api", isSipManagerSupported);

                result.success(arguments);
                break;

            case "initSip":
                try {
                    String username = call.argument("username");
                    String password = call.argument("password");
                    String domain = call.argument("domain");
                    int port = call.argument("port");
                    String protocol = call.argument("protocol");

                    sipDataManager.initializeProfile(username, password, domain, port, protocol);
                    result.success(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    result.success(false);
                }
                break;
            case "discSip":
                try {
                    boolean res = sipDataManager.dispose();
                    result.success(res);
                } catch (Exception e) {
                    e.printStackTrace();
                    result.error(null, e.toString(), null);
                }
                break;
            case "initCall":
                String username = call.argument("uri");
                sipDataManager.makeCall(result, username);

                break;
            case "endCall":
                sipDataManager.endCall(result);

                break;
            case "holdCall":
                sipDataManager.holdCall(result);

                break;
            case "muteCall":
                sipDataManager.muteCall(result);

                break;
            case "speakerMode":
                boolean mode = call.argument("mode");
                sipDataManager.callSpeakerMode(result, mode);

                break;
            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public void onListen(Object arguments, final EventChannel.EventSink events) {
        try {
            sipDataManager.setupRegistrationStream(events, registrationState);
        } catch (Exception e) {
            e.printStackTrace();
            events.error(null, e.toString(), null);
        }
    }

    @Override
    public void onCancel(Object arguments) {
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        this.pluginBinding = null;
        this.activityPluginBinding = null;
        sipDataManager.dispose();
        methodChannel.setMethodCallHandler(null);
    }


    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activityPluginBinding = binding;
        // initialize sip manager
        if (sipDataManager == null) {
            sipDataManager = new SipDataManager(binding.getActivity().getApplicationContext(), uiThreadHandler);
            try {
                sipDataManager.initialize(binding.getActivity().getApplicationContext());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // init utils
        if (utils == null) {
            utils = new Utils();
        }
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        activityPluginBinding = binding;
    }

    @Override
    public void onDetachedFromActivity() {
        sipDataManager.dispose();
    }

}
