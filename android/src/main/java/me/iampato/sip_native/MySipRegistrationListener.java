package me.iampato.sip_native;

import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.os.Handler;
import android.util.Log;

import io.flutter.plugin.common.EventChannel;

public class MySipRegistrationListener implements SipRegistrationListener {
    final Handler uiThreadHandler;
    final EventChannel.EventSink events;
    SipRegistrationState registrationState;

    public MySipRegistrationListener(Handler uiThreadHandler, EventChannel.EventSink events, SipRegistrationState registrationState) {
        this.uiThreadHandler = uiThreadHandler;
        this.events = events;
        this.registrationState = registrationState;
    }

    @Override
    public void onRegistering(String localProfileUri) {
        registrationState = SipRegistrationState.ONREGISTERING;
        uiThreadHandler.post(
                () -> {
                    events.success(registrationState.toString());
                }
        );
    }

    @Override
    public void onRegistrationDone(String localProfileUri, long expiryTime) {
        registrationState = SipRegistrationState.ONREGISTRATIONDONE;
        uiThreadHandler.post(
                () -> {
                    events.success(registrationState.toString());
                }
        );
    }

    @Override
    public void onRegistrationFailed(String localProfileUri, int errorCode, String errorMessage) {
        registrationState = SipRegistrationState.ONREGISTRATIONFAILED;
        Log.d("SIP_NATIVE",errorMessage);
        Log.d("SIP_NATIVE","Error code "+String.valueOf(errorCode));
        uiThreadHandler.post(
                () -> {
                    events.success(registrationState.toString());
                }
        );
        uiThreadHandler.post(
                () -> {
                    events.error(null,"Registration onError" + "\nError code:" + String.valueOf(errorCode) + "\nError message:" + errorMessage,null);
                }
        );
    }
}
