package me.iampato.sip_native;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import org.pjsip.pjsua2.*;
import org.pjsip.pjsua2.CallInfo;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_status_code;

import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import me.iampato.sip_native.manager.*;
import me.iampato.sip_native.manager.MyAppObserver;
import me.iampato.sip_native.manager.MyBuddy;
import me.iampato.sip_native.manager.MyCall;
import me.iampato.sip_native.manager.PjSipManager;

/**
 * SipNativePlugin
 */

public class SipNativePlugin implements FlutterPlugin, MethodCallHandler, StreamHandler, ActivityAware {
    // global variables
    final private String TAG = "SIP Native";
    private final Handler uiThreadHandler = new Handler(Looper.getMainLooper());
    // initialize the registrationState to unknown then mutate it later after setting up the
    // sipManager.setRegistrationListene
    private PjSipManagerState mPjSipManagerState = PjSipManagerState.STATE_UNDEFINED;
    private MyBroadcastReceiver mReceiver;
    private TelephonyManager mTelephonyManager;
    private SystemPhoneStateListener mSystemPhoneStateListener;
    private SensorManager mSensorManager;
    private Result mResult;
    private String mMethod;
    private MyCall mCurrentCall;
    private int port;
    private String ip;
    private PjSipManager mPjSipManager = PjSipManager.getInstance();
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel methodChannel;
    private FlutterPluginBinding pluginBinding = null;
    private ActivityPluginBinding activityPluginBinding;
//    private SipDataManager sipDataManager;
    private Utils utils;
    private MyAppObserver mAppObserver = new MyAppObserver() {
        @Override
        public void notifyRegState(pjsip_status_code code, String reason, int expiration) {
            if (TextUtils.equals(mMethod, "initSip")) {
                boolean loginResult = code.swigValue() / 100 == 2;
                mMethod = "";

                Message m = Message.obtain(handler, MSG_TYPE.REG_STATE, loginResult);
                m.sendToTarget();
            }
        }

        @Override
        public void notifyIncomingCall(MyCall call) {
            Message m = Message.obtain(handler, MSG_TYPE.INCOMING_CALL, call);
            m.sendToTarget();
        }

        @Override
        public void notifyCallState(MyCall call) {
            if (mCurrentCall == null || call.getId() != mCurrentCall.getId()) return;
            CallInfo info = null;
            try {
                info = call.getInfo();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (info != null) {
                Message m = Message.obtain(handler, MSG_TYPE.CALL_STATE, info);
                m.sendToTarget();
            }
        }

        @Override
        public void notifyCallMediaState(MyCall call) {
            Message m = Message.obtain(handler, MSG_TYPE.CALL_MEDIA_STATE, null);
            m.sendToTarget();
        }

        @Override
        public void notifyBuddyState(MyBuddy buddy) {
            Message m = Message.obtain(handler, MSG_TYPE.BUDDY_STATE, buddy);
            m.sendToTarget();
        }

        @Override
        public void notifyChangeNetwork() {
            Message m = Message.obtain(handler, MSG_TYPE.CHANGE_NETWORK, null);
            m.sendToTarget();
        }

    };
    private SensorEventListener mSensorEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] its = event.values;
            if (its != null && event.sensor.getType() == Sensor.TYPE_PROXIMITY) {

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (mResult == null) return false;
            int what = msg.what;
            switch (what) {
                case MSG_TYPE.REG_STATE:
                    boolean loginResult = (boolean) msg.obj;
                    mPjSipManagerState = PjSipManagerState.STATE_LOGINED;
                    mResult.success(loginResult);
                    break;

                case MSG_TYPE.CALL_STATE:
                    CallInfo callInfo = (CallInfo) msg.obj;
                    if (mCurrentCall == null || callInfo == null || callInfo.getId() != mCurrentCall.getId()) {
                        System.out.println("Call state event received, but call info is invalid");
                        return true;
                    }
                    pjsip_inv_state state = callInfo.getState();
                    if (state == pjsip_inv_state.PJSIP_INV_STATE_CALLING) {

                        mPjSipManagerState = PjSipManagerState.STATE_CALLING;
                    } else if (state == pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED) {
                        registerPhoneState();
                        mPjSipManagerState = PjSipManagerState.STATE_CONFIRMED;
                    } else if (state == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED) {
                        mPjSipManagerState = PjSipManagerState.STATE_DISCONNECTED;
                        mCurrentCall.delete();
                        mCurrentCall = null;
                        unRegisterPhoneState();
                    }

                    if (methodChannel != null) {
                        Log.i(TAG, "FlutterPjsipPlugin接收到状态" + callInfo.getStateText());
                        methodChannel.invokeMethod("method_call_state_changed", buildArguments(callInfo.getStateText(), callInfo.getRemoteUri()));
                    }
                    break;

                case MSG_TYPE.CALL_MEDIA_STATE:
                    // TODO 未实现视频通话，暂不用实现
                    break;

                case MSG_TYPE.INCOMING_CALL:
                    /* Incoming call */
                    MyCall call = (MyCall) msg.obj;
                    CallOpParam prm = new CallOpParam();
                    /* Only one call at anytime */
                    if (mCurrentCall != null) {
                        try {
                            // 设置StatusCode
                            prm.setStatusCode(pjsip_status_code.PJSIP_SC_BUSY_HERE);
                            call.hangup(prm);
                            call.delete();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return true;
                    } else {
                        try {

                            /* Answer with ringing */
                            prm.setStatusCode(pjsip_status_code.PJSIP_SC_RINGING);
                            call.answer(prm);
                            mCurrentCall = call;

                            mPjSipManagerState = PjSipManagerState.STATE_INCOMING;

                            if (methodChannel != null) {
                                methodChannel.invokeMethod("method_call_state_changed", buildArguments("INCOMING", mCurrentCall.getInfo().getRemoteUri()));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;

                case MSG_TYPE.CHANGE_NETWORK:
                    if (mPjSipManager != null)
                        mPjSipManager.handleNetworkChange();
                    break;
            }
            return false;
        }
    });

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
//        if (sipDataManager == null) {
//            sipDataManager = new SipDataManager(flutterPluginBinding.getApplicationContext(), uiThreadHandler);
//            try {
//                sipDataManager.initialize(flutterPluginBinding.getApplicationContext());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
        // init utils
        if (utils == null) {
            utils = new Utils();
        }
        // setup method channels
        methodChannel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "sip_native/method");
        methodChannel.setMethodCallHandler(this);
        // setup event channels
//        EventChannel eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "sip_native/register_events");
//        eventChannel.setStreamHandler(this);
//        CallsEventChannel callsEventChannel = new CallsEventChannel(sipDataManager, sipCallState);
//        EventChannel callsEvent = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "sip_native/calls_events");
//        callsEvent.setStreamHandler(callsEventChannel);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        mResult = result;
        mMethod = call.method;
        try {
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

                    pjsipInit(result);
//                boolean isVoipSupported = sipDataManager.isVoipSupported(this.pluginBinding.getApplicationContext());
//                boolean isSipManagerSupported = sipDataManager.isSipManagerSupported(this.pluginBinding.getApplicationContext());
//                HashMap<String, Object> arguments = new HashMap<>();
//                arguments.put("voip", isVoipSupported);
//                arguments.put("api", isSipManagerSupported);
//
//                result.success(arguments);
                    break;

                case "initSip":
                    String username = call.argument("username");
                    String password = call.argument("password");
                    String domain = call.argument("domain");
                    int port = call.argument("port");
                    String protocol = call.argument("protocol");
                    this.ip = domain;
                    this.port = port;
                    pjsipLogin(username, password, domain,String.valueOf(port));
//                try {
//                    String username = call.argument("username");
//                    String password = call.argument("password");
//                    String domain = call.argument("domain");
//                    int port = call.argument("port");
//                    String protocol = call.argument("protocol");
//
//                    sipDataManager.initializeProfile(username, password, domain, port, protocol);
//                    result.success(true);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    result.success(false);
//                }
                    break;
                case "discSip":
                    try {
//                        boolean res = sipDataManager.dispose();
//                        result.success(res);
                    } catch (Exception e) {
                        e.printStackTrace();
                        result.error(null, e.toString(), null);
                    }
                    break;
                case "initCall":
                    String uri = call.argument("uri");
                    pjsipCall(uri, this.ip,String.valueOf(this.port));
//                    sipDataManager.makeCall(result, uri);

                    break;
                case "endCall":
                    pjsipRefuse();
//                    sipDataManager.endCall(result);

                    break;
                case "holdCall":
//                    sipDataManager.holdCall(result);

                    break;
                case "muteCall":
//                    sipDataManager.muteCall(result);

                    break;
                case "speakerMode":
                    boolean mode = call.argument("mode");
//                    sipDataManager.callSpeakerMode(result, mode);

                    break;
                default:
                    result.notImplemented();
                    break;
            }
        }catch (Exception e) {
            e.printStackTrace();
            result.error(null, e.toString(), null);
        }
    }

    @Override
    public void onListen(Object arguments, final EventChannel.EventSink events) {
//        try {
//            sipDataManager.setupRegistrationStream(events, registrationState);
//        } catch (Exception e) {
//            e.printStackTrace();
//            events.error(null, e.toString(), null);
//        }
    }

    @Override
    public void onCancel(Object arguments) {
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        this.pluginBinding = null;
        this.activityPluginBinding = null;
//        sipDataManager.dispose();
        methodChannel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activityPluginBinding = binding;
        // initialize sip manager
//        if (sipDataManager == null) {
//            sipDataManager = new SipDataManager(binding.getActivity().getApplicationContext(), uiThreadHandler);
//            try {
//                sipDataManager.initialize(binding.getActivity().getApplicationContext());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
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
        // sipDataManager.dispose();
    }

    // pjsip
    private void pjsipInit(@NonNull Result result) {
        if (mPjSipManagerState.getCode() > PjSipManagerState.STATE_UNDEFINED.getCode())
            result.success(false);
        else {
            mPjSipManager.init(mAppObserver);
            if (mReceiver == null) {
                mReceiver = new MyBroadcastReceiver();
                IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
                this.activityPluginBinding.getActivity().getApplication().registerReceiver(mReceiver, intentFilter);
            }
            mPjSipManagerState = PjSipManagerState.STATE_INITED;
            result.success(true);
        }
    }

    private void pjsipLogin(String username, String password, String ip, String port) {
        if (mPjSipManagerState.getCode() == PjSipManagerState.STATE_INITED.getCode())
            mPjSipManager.login(username, password, ip, port);
        else
            mResult.success(false);
    }
    private void pjsipCall(String username, String ip, String port) {
        if (mCurrentCall != null)
            mResult.success(false);
        else {
            MyCall call = null;
            if (mPjSipManagerState.getCode() >= PjSipManagerState.STATE_LOGINED.getCode())
                call = mPjSipManager.call(username, ip, port);
            else
                mResult.success(false);

            if (call == null)
                mResult.success(false);
            else {
                mCurrentCall = call;
                mResult.success(true);
            }
        }
    }
    private void pjsipRefuse() {
        if (mCurrentCall == null)
            mResult.success(false);
        else {
            try {
                CallOpParam prm = new CallOpParam();
                prm.setStatusCode(pjsip_status_code.PJSIP_SC_DECLINE);
                mCurrentCall.hangup(prm);
                mResult.success(true);
            } catch (Exception e) {
                e.printStackTrace();
                mResult.success(false);
            } finally {
                mCurrentCall = null;
                if (methodChannel != null) {
                    methodChannel.invokeMethod("method_call_state_changed", buildArguments("DISCONNECTED", null));
                }
                mPjSipManagerState = PjSipManagerState.STATE_DISCONNECTED;
            }
        }
    }

    private void registerPhoneState() {
        Activity activity = this.activityPluginBinding.getActivity();
        PowerManager powerManager = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);

        // 传感器管理对象,调用距离传感器，控制屏幕
        mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(mSensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);

        mTelephonyManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
        mSystemPhoneStateListener = new SystemPhoneStateListener();
        mTelephonyManager.listen(mSystemPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

    }

    private void unRegisterPhoneState() {
        if (mSystemPhoneStateListener != null && mTelephonyManager != null) {
            mTelephonyManager.listen(mSystemPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            mSystemPhoneStateListener = null;
            mTelephonyManager = null;
        }
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mSensorEventListener);
            mSensorManager = null;
        }

    }

    private Map<String, Object> buildArguments(String status, Object remoteUri) {
        Map<String, Object> result = new HashMap<>();
        result.put("call_state", status);
        result.put("remote_uri", remoteUri != null ? remoteUri : "");
        return result;
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        private String conn_name = "";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (isNetworkChange(context)) {
                Message m = Message.obtain(handler, MSG_TYPE.CHANGE_NETWORK, null);
                m.sendToTarget();
            }
        }

        private boolean isNetworkChange(Context context) {
            boolean network_changed = false;
            ConnectivityManager connectivity_mgr = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
            if (connectivity_mgr != null) {
                NetworkInfo net_info = connectivity_mgr.getActiveNetworkInfo();
                if (net_info != null && conn_name != null) {
                    if (net_info.isConnectedOrConnecting() && !conn_name.equalsIgnoreCase("")) {
                        String new_con = net_info.getExtraInfo();
                        if (new_con != null && !new_con.equalsIgnoreCase(conn_name))
                            network_changed = true;
                        conn_name = (new_con == null) ? "" : new_con;
                    } else {
                        if (conn_name.equalsIgnoreCase(""))
                            conn_name = net_info.getExtraInfo();
                    }
                }
            }
            return network_changed;
        }
    }

    private class MSG_TYPE {
        public final static int REG_STATE = 1;
        public final static int CALL_STATE = 2;
        public final static int CALL_MEDIA_STATE = 3;
        public final static int INCOMING_CALL = 4;
        public final static int BUDDY_STATE = 5;
        public final static int CHANGE_NETWORK = 6;
    }

    private class SystemPhoneStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:

                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    System.out.println();
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:

//                    pjsipRefuse();
                    break;
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    }
}
