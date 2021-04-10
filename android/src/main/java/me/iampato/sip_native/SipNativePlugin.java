package me.iampato.sip_native;

import android.Manifest;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

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

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        pluginBinding = flutterPluginBinding;

        // initialize sip manager
        if (this.sipDataManager == null) {
            sipDataManager = new SipDataManager(flutterPluginBinding.getApplicationContext(), uiThreadHandler);
            try {
                sipDataManager.initialize();
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
        EventChannel callsEventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "sip_native/calls_events");
        callsEventChannel.setStreamHandler(this);
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
                    UserProfile userProfile = new UserProfile(username, password, domain, port, protocol);
                    sipDataManager.setUserProfile(userProfile);
                    sipDataManager.initializeProfile();
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
                try {
                    String profileUri = call.argument("uri");
                    sipDataManager.makeCall(result, profileUri);
                } catch (Exception e) {
                    e.printStackTrace();
                    result.error(null, e.toString(), null);
                }
                break;
            case "endCall":
                try {
                    sipDataManager.endCall(result);
                } catch (Exception e) {
                    e.printStackTrace();
                    result.error(null, e.toString(), null);
                }
                break;
            case "holdCall":
                try {
                    sipDataManager.holdCall(result);
                } catch (Exception e) {
                    e.printStackTrace();
                    result.error(null, e.toString(), null);
                }
                break;
            case "muteCall":
                try {
                    sipDataManager.muteCall(result);
                } catch (Exception e) {
                    e.printStackTrace();
                    result.error(null, e.toString(), null);
                }
                break;
            case "speakerMode":
                try {
                    boolean mode = call.argument("mode");
                    sipDataManager.callSpeakerMode(result, mode);
                } catch (Exception e) {
                    e.printStackTrace();
                    result.error(null, e.toString(), null);
                }
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
        if (this.sipDataManager == null) {
            sipDataManager = new SipDataManager(binding.getActivity().getApplicationContext(), uiThreadHandler);
            try {
                sipDataManager.initialize();
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
