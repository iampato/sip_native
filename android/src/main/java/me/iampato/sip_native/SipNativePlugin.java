package me.iampato.sip_native;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
enum SipRegistrationState {UNKNOWN, ONREGISTERING, ONREGISTRATIONDONE, ONREGISTRATIONFAILED}

public class SipNativePlugin implements FlutterPlugin, MethodCallHandler, StreamHandler, ActivityAware {
    /// To use the SIP API, your application must create a SipManager object.
    /// The SipManager takes care of the following in your application:
    ///  1. Initiating SIP sessions.
    ///  2. Initiating and receiving calls.
    ///  3. Registering and unregistering with a SIP provider.
    ///  4. Verifying session connectivity.
    public SipManager sipManager;
    /// A SipProfile defines a SIP profile, including a SIP account, and domain
    /// and server information.
    public SipProfile sipProfile = null;
    // initialize the registrationState to unknown then mutate it later after setting up the
    // sipManager.setRegistrationListener
    SipRegistrationState registrationState = SipRegistrationState.UNKNOWN;
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel methodChannel;
    private EventChannel eventChannel;
    private FlutterPluginBinding pluginBinding = null;
    private ActivityPluginBinding activityPluginBinding;
    private SipRegistrationListener listener;
    private Handler uiThreadHandler = new Handler(Looper.getMainLooper());


    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        pluginBinding = flutterPluginBinding;
        // initialize sip manager

        if (this.sipManager == null) {
            Log.d("SIP NATIVE", "onAttachedToEngine: Sip manager init was called");
            try {
                this.sipManager = SipManager.newInstance(pluginBinding.getApplicationContext());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.d("SIP NATIVE", "onAttachedToEngine: Sip manager is crazy");
        }
        // setup method channels
        methodChannel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "sip_native/method");
        methodChannel.setMethodCallHandler(this);
        // setup event channels
        eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "sip_native/event");
        eventChannel.setStreamHandler(this);
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
                        boolean isSuccess = checkPermissions(permissionArrays);
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
                boolean isVoipSupported = false;
                boolean isSipManagerSupported = false;

                // check if Sip manager is supported
                if (SipManager.isVoipSupported(this.pluginBinding.getApplicationContext())) {
                    Log.d("VOIP:", "Supported!");
                    isVoipSupported = true;
                } else {
                    Log.d("VOIP:", "Not Supported");
                }
                if (SipManager.isApiSupported(this.pluginBinding.getApplicationContext())) {
                    Log.d("API:", "Supported!");
                    isSipManagerSupported = true;
                } else {
                    Log.d("API:", "NotSupported!");
                }
                HashMap<String, Object> arguments = new HashMap<>();
                arguments.put("voip", isVoipSupported);
                arguments.put("api", isSipManagerSupported);

                result.success(arguments);

                break;
            case "discSip":
                try {
                    closeLocalProfile();
                } catch (Exception e) {
                    e.printStackTrace();
                    result.error(null, e.toString(), null);
                }
                break;
            case "initSip":
                String username = "254717008247";//call.argument("username");
                String password = "475bbd248835981240e0fab16cdeb5af"; //call.argument("password");
                String domain = "138.68.167.56";//call.argument("domain");

                //
                try {
                    SipProfile.Builder builder = new SipProfile.Builder(username, domain);
                    builder.setPassword(password);
                    builder.setPort(5060);
                    builder.setProtocol("UDP");
                    builder.setAutoRegistration(true);
//                    builder.setSendKeepAlive(true);
                    this.sipProfile = builder.build();
                    Intent intent = new Intent();
                    intent.setAction("android.SIPNative.INCOMING_CALL");
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(this.pluginBinding.getApplicationContext(), 0, intent, Intent.FILL_IN_DATA);
                    sipManager.open(sipProfile);
                    TimeUnit.SECONDS.sleep(10);
//                    sipManager.setRegistrationListener(sipProfile.getUriString(),new SipRegistrationListener() {
//                        @Override
//                        public void onRegistering(String localProfileUri) {
//                            Log.d("SIP plugin", "sip registering");
//                        }
//
//                        @Override
//                        public void onRegistrationDone(String localProfileUri, long expiryTime) {
//                            Log.d("SIP plugin", "sip onRegistrationDone");
//                        }
//
//                        @Override
//                        public void onRegistrationFailed(String localProfileUri, int errorCode, String errorMessage) {
//                            Log.d("SIP plugin", "sip onRegistrationFailed\n"+"uri: "+localProfileUri+"\nError code:"+String.valueOf(errorCode)+"\nError message:"+errorMessage);
//                        }
//                    });
//                    sipManager.open(sipProfile, pendingIntent, new SipRegistrationListener() {
//                        @Override
//                        public void onRegistering(String localProfileUri) {
//                            Log.d("SIP plugin", "sip registering");
//                        }
//
//                        @Override
//                        public void onRegistrationDone(String localProfileUri, long expiryTime) {
//                            Log.d("SIP plugin", "sip onRegistrationDone");
//                        }
//
//                        @Override
//                        public void onRegistrationFailed(String localProfileUri, int errorCode, String errorMessage) {
//                            Log.d("SIP plugin", "sip onRegistrationFailed\n"+"uri: "+localProfileUri+"\nError code:"+String.valueOf(errorCode)+"\nError message:"+errorMessage);
//                        }
//                    });
//                    sipManager.register(sipProfile, 3600, new SipRegistrationListener() {
//                        @Override
//                        public void onRegistering(String localProfileUri) {
//                            Log.d("SIP plugin", "sip registering");
//                        }
//
//                        @Override
//                        public void onRegistrationDone(String localProfileUri, long expiryTime) {
//                            Log.d("SIP plugin", "sip onRegistrationDone");
//                        }
//
//                        @Override
//                        public void onRegistrationFailed(String localProfileUri, int errorCode, String errorMessage) {
//                            Log.d("SIP plugin", "sip onRegistrationFailed");
//                        }
//                    });
                    result.success(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    result.success(false);
                }
                //
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public void onListen(Object arguments, final EventChannel.EventSink events) {

        if (this.sipManager != null && this.sipProfile != null) {
            try {
                events.success(registrationState.toString());
                sipManager.register(sipProfile, 3600, new SipRegistrationListener() {
                    @Override
                    public void onRegistering(String localProfileUri) {
                        Log.d("SIP plugin", "sip registering");
                        registrationState = SipRegistrationState.ONREGISTERING;
                        uiThreadHandler.post(
                                () -> {
                                    events.success(registrationState.toString());
                                }
                        );
                    }

                    @Override
                    public void onRegistrationDone(String localProfileUri, long expiryTime) {
                        Log.d("SIP plugin", "sip onRegistrationDone");
                        registrationState = SipRegistrationState.ONREGISTRATIONDONE;
                        uiThreadHandler.post(
                                () -> {
                                    events.success(registrationState.toString());
                                }
                        );
                    }

                    @Override
                    public void onRegistrationFailed(String localProfileUri, int errorCode, String errorMessage) {
                        Log.d("SIP plugin", "sip onRegistrationFailed\n" + "uri: " + localProfileUri + "\nError code:" + String.valueOf(errorCode) + "\nError message:" + errorMessage);
                        registrationState = SipRegistrationState.ONREGISTRATIONFAILED;
                        uiThreadHandler.post(
                                () -> {
                                    events.success(registrationState.toString());
                                }
                        );
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Log.d("SIP", sipProfile.getProtocol());
                Log.d("SIP", sipProfile.getUserName());
                Log.d("SIP", sipProfile.getSipDomain());
                Log.d("SIP", sipProfile.getUriString());
                Log.d("SIP", String.valueOf(sipProfile.getPort()));
            }

        } else {
            events.error(null, "Sip manager || user profile is null", null);
        }
    }

    @Override
    public void onCancel(Object arguments) {
//        try {
////            sipManager.unregister();
//            // TODO remove the listener;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        this.pluginBinding = null;
        this.activityPluginBinding = null;
        closeLocalProfile();
        methodChannel.setMethodCallHandler(null);
    }

    public void closeLocalProfile() {
        if (sipManager == null) {
            return;
        }
        try {
            if (sipProfile != null) {
                sipManager.close(sipProfile.getUriString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activityPluginBinding = binding;
        if (SipManager.isVoipSupported(binding.getActivity().getApplicationContext())) {
            Log.d("VOIP:", "Supported!");
        } else {
            Log.d("VOIP:", "Not Supported");
        }
        if (SipManager.isApiSupported(binding.getActivity().getApplicationContext())) {
            Log.d("API:", "Supported!");
        } else {
            Log.d("API:", "NotSupported!");
        }
        if (this.sipManager == null) {
            Log.d("SIP NATIVE", "onAttachedToActivity: Sip manager init was called");
            try {
                this.sipManager = SipManager.newInstance(binding.getActivity().getApplicationContext());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.d("SIP NATIVE", "onAttachedToActivity: Sip manager is crazy");
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
        closeLocalProfile();
    }

    private boolean checkPermissions(String[] permissions) {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this.pluginBinding.getApplicationContext(), p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this.activityPluginBinding.getActivity(), listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 1111);
            return false;
        }
        return true;
    }

}
