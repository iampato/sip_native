//package me.iampato.sip_native;
//
//import android.annotation.TargetApi;
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//import android.net.sip.SipAudioCall;
//import android.net.sip.SipManager;
//import android.net.sip.SipProfile;
//import android.net.sip.SipRegistrationListener;
//import android.net.sip.SipSession;
//import android.os.Build;
//import android.os.Handler;
//import android.util.Log;
//
//import org.pjsip.pjsua2.AccountConfig;
//
//import io.flutter.plugin.common.EventChannel;
//import io.flutter.plugin.common.MethodChannel;
//
//public class SipDataManager {
//    final private String TAG = "SIP Native";
//    private final Handler uiThreadHandler;
//    /// To use the SIP API, your application must create a SipManager object.
//    /// The SipManager takes care of the following in your application:
//    ///  1. Initiating SIP sessions.
//    ///  2. Initiating and receiving calls.
//    ///  3. Registering and unregistering with a SIP provider.
//    ///  4. Verifying session connectivity.
//    public SipManager sipManager;
//    /// A SipProfile defines a SIP profile, including a SIP account, and domain
//    /// and server information.
//    public SipProfile sipProfile;
//    private SipSession session;
//    private Context context;
//    private SipAudioCall call;
//
//    public SipDataManager(Context context, Handler uiThreadHandler) {
//        this.context = context;
//        this.uiThreadHandler = uiThreadHandler;
//    }
//
//    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
//    public void initialize(Context context) throws Exception {
//        Log.d(TAG, "initialize manager");
//        if(mAccountConfig ==null){
//            mAccountConfig = new AccountConfig();
//
//        }
//        if (sipManager == null) {
//            Log.d(TAG, "sip manager is not null");
//            sipManager = SipManager.newInstance(context);
//            if (sipManager == null) {
//                throw new Exception("Sip not supported");
//            }
//        }
//    }
//
//    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
//    public boolean isVoipSupported(Context context) {
//        boolean isVoipSupported = false;
//        // check if Sip manager is supported
//        if (SipManager.isVoipSupported(context)) {
//            Log.d(TAG, "Supported!");
//            isVoipSupported = true;
//        } else {
//            Log.d(TAG, "Not Supported");
//        }
//        return isVoipSupported;
//    }
//
//    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
//    public boolean isSipManagerSupported(Context context) {
//        boolean isSipManagerSupported = false;
//        if (SipManager.isApiSupported(context)) {
//            Log.d(TAG, "Supported!");
//            isSipManagerSupported = true;
//        } else {
//            Log.d(TAG, "NotSupported!");
//        }
//        return isSipManagerSupported;
//    }
//
//    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
//    public void initializeProfile(String username, String password, String domain, int port, String protocol) throws Exception {
//        if (sipManager == null) {
//            Log.d(TAG, "Sip manager cannot be null");
//        }
//        SipProfile.Builder builder = new SipProfile.Builder(username, domain);
//        builder.setPassword(password);
//        builder.setPort(port);
//        builder.setProtocol(protocol);
//        builder.setAutoRegistration(true);
//        sipProfile = builder.build();
////        sipManager.open(sipProfile);
//        Intent intent = new Intent();
//        intent.setAction("android.SIPNative.INCOMING_CALL");
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.context, 0, intent, Intent.FILL_IN_DATA);
//        SipRegistrationListener registrationListener = new SipRegistrationListener() {
//            @Override
//            public void onRegistering(String localProfileUri) {
//                Log.d(TAG, "sip registering");
//            }
//
//            @Override
//            public void onRegistrationDone(String localProfileUri, long expiryTime) {
//                Log.d(TAG, "sip onRegistrationDone");
//            }
//            @Override
//            public void onRegistrationFailed(String localProfileUri, int errorCode, String errorMessage) {
//                Log.d(TAG, "sip onRegistrationFailed\n" + "uri: " + localProfileUri + "\nError code:" + String.valueOf(errorCode) + "\nError message:" + errorMessage);
//            }
//        };
//        sipManager.open(sipProfile, pendingIntent, registrationListener);
//
//    }
//
//    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
//    public void setupRegistrationStream(final EventChannel.EventSink events, SipRegistrationState registrationState) throws Exception {
//        if (sipManager != null && this.sipProfile != null) {
//            // emit unknown
//            uiThreadHandler.post(
//                    () -> {
//                        events.success(registrationState.toString());
//                    }
//            );
//            SipRegistrationListener listener = new MySipRegistrationListener(uiThreadHandler, events, registrationState);
//            sipManager.setRegistrationListener(sipProfile.getUriString(), listener);
////
////                Log.d("Called", "called this");
////                Log.d("Called", sipProfile.getSipDomain());
////                Log.d("Called", sipProfile.getUserName());
////                Log.d("Called", String.valueOf(sipProfile.getPort()));
////                Log.d("Called", sipProfile.getProtocol());
////                Log.d("Called", sipProfile.getPassword());
////                Log.d("Called", String.valueOf(sipManager.isOpened(sipProfile.getUriString())));
//
//        } else {
//            if (sipManager == null) {
//                throw new Exception("sip manager cannot be null");
//            } else {
//                throw new Exception("sip profile cannot be null");
//            }
//        }
//    }
//
//    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
//    public void makeCall(MethodChannel.Result result, String username) {
//        String address = username + "@" + sipProfile.getSipDomain();
//        Log.d(TAG,"Sip address: "+address);
//        Log.d(TAG,"Sip address: "+address);
//
//        try {
//            Log.d(TAG,"Sip manager open?: "+sipManager.isOpened(sipProfile.getUriString()));
////            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
////                @Override
////                public void onCalling(SipAudioCall call) {
////                    Log.d(TAG,"call onCalling");
////                }
////
////                @Override
////                public void onRinging(SipAudioCall call, SipProfile caller) {
////                    Log.d(TAG,"call onRinging");
////                }
////
////                @Override
////                public void onRingingBack(SipAudioCall call) {
////                    Log.d(TAG,"call onRingingBack");
////                }
////
////                @Override
////                public void onReadyToCall(SipAudioCall call) {
////                    Log.d(TAG,"call onReadyToCall");
////                }
////                // Much of the client's interaction with the SIP Stack will
////                // happen via listeners.  Even making an outgoing call, don't
////                // forget to set up a listener to set things up once the call is established.
////                @Override
////                public void onCallEstablished(SipAudioCall call) {
////                    Log.d(TAG,"call onCallEstablished");
////                    call.startAudio();
////                    call.setSpeakerMode(true);
////                    call.toggleMute();
////                }
////
////                @Override
////                public void onCallEnded(SipAudioCall call) {
////                    Log.d(TAG,"call onCallEnded");
////                }
////                @Override
////                public void onCallBusy(SipAudioCall call) {
////                    Log.d(TAG,"call onCallBusy");
////
////                }
////                @Override
////                public void onError(SipAudioCall call, int errorCode,
////                                    String errorMessage) {
////                    Log.d(TAG,"call onError"+ "\nError code:" + String.valueOf(errorCode) + "\nError message:" + errorMessage);
////                }
////            };
//            call = sipManager.makeAudioCall(sipProfile.getUriString(), address, null, 0);
//            if (call != null) {
//                result.success(true);
//            } else {
//                result.success(false);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            result.error(null, e.toString(), null);
//        }
//    }
//
//    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
//    public void setupCallStateStream(final EventChannel.EventSink events, SipCallState sipCallState) throws Exception {
//        if (call != null) {
//            call.setListener(new MySipCallListener(uiThreadHandler, events, sipCallState));
//        } else {
//            throw new Exception("sip manager cannot be null");
//        }
//    }
//
//    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
//    public void endCall(MethodChannel.Result result) {
//        try {
//            if (call != null) {
//                call.endCall();
//                result.success(true);
//            } else {
//                throw new Exception("No call is in progress");
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            result.error(null, e.toString(), null);
//        }
//    }
//
//    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
//    public void holdCall(MethodChannel.Result result) {
//        try {
//            if (call != null) {
//                call.holdCall(0);
//                result.success(true);
//            } else {
//                throw new Exception("No call is in progress");
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            result.error(null, e.toString(), null);
//        }
//    }
//
//    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
//    public void callSpeakerMode(MethodChannel.Result result, boolean value) {
//        try {
//            if (call != null) {
//                call.setSpeakerMode(value);
//                result.success(true);
//            } else {
//                throw new Exception("No call is in progress");
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            result.error(null, e.toString(), null);
//        }
//    }
//
//    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
//    public void muteCall(MethodChannel.Result result) {
//        try {
//            if (call != null) {
//                call.toggleMute();
//                result.success(true);
//            } else {
//                throw new Exception("No call is in progress");
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            result.error(null, e.toString(), null);
//        }
//    }
//
//    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
//    public boolean dispose() {
//        if (sipManager == null || call == null) {
//            return true;
//        } else {
//            try {
//                if (sipProfile != null) {
//                    sipManager.close(sipProfile.getUriString());
//                }
//                if (call != null) {
//                    call.close();
//                }
//                return true;
//            } catch (Exception e) {
//                e.printStackTrace();
//                return false;
//            }
//        }
//    }
//
//
//    public SipManager getSipManager() {
//        return sipManager;
//    }
//
//    public void setSipManager(SipManager sipManager) {
//        this.sipManager = sipManager;
//    }
//
//    public SipAudioCall getCall() {
//        return call;
//    }
//
//    public Context getContext() {
//        return context;
//    }
//}
////getters and setters