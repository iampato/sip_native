package me.iampato.sip_native;

import android.net.sip.SipAudioCall;
import android.net.sip.SipProfile;
import android.os.Handler;
import android.util.Log;

import io.flutter.plugin.common.EventChannel;

public class MySipCallListener extends SipAudioCall.Listener {
    final Handler uiThreadHandler;
    final EventChannel.EventSink events;
    final private String TAG = "SIP Native";
    SipCallState sipCallState;

    public MySipCallListener(Handler uiThreadHandler, EventChannel.EventSink events, SipCallState sipCallState) {
        this.uiThreadHandler = uiThreadHandler;
        this.events = events;
        this.sipCallState = sipCallState;
    }

    @Override
    public void onCalling(SipAudioCall call) {
        sipCallState = SipCallState.ONCALLING;
        uiThreadHandler.post(
                () -> {
                    events.success(sipCallState.toString());
                }
        );
    }

    @Override
    public void onRinging(SipAudioCall call, SipProfile caller) {
        sipCallState = SipCallState.ONRINGING;
        uiThreadHandler.post(
                () -> {
                    events.success(sipCallState.toString());
                }
        );
    }

    @Override
    public void onRingingBack(SipAudioCall call) {
        sipCallState = SipCallState.ONRINGINGBACK;
        uiThreadHandler.post(
                () -> {
                    events.success(sipCallState.toString());
                }
        );
    }

    @Override
    public void onReadyToCall(SipAudioCall call) {
        sipCallState = SipCallState.ONREADYTOCALL;
        uiThreadHandler.post(
                () -> {
                    events.success(sipCallState.toString());
                }
        );
    }

    // Much of the client's interaction with the SIP Stack will
    // happen via listeners.  Even making an outgoing call, don't
    // forget to set up a listener to set things up once the call is established.
    @Override
    public void onCallEstablished(SipAudioCall call) {
        sipCallState = SipCallState.ONCALLESTABLISHED;
        uiThreadHandler.post(
                () -> {
                    events.success(sipCallState.toString());
                }
        );
        call.startAudio();
        call.setSpeakerMode(false);
        call.toggleMute();
    }

    @Override
    public void onCallEnded(SipAudioCall call) {
        sipCallState = SipCallState.ONCALLENDED;
        uiThreadHandler.post(
                () -> {
                    events.success(sipCallState.toString());
                }
        );
    }

    @Override
    public void onCallBusy(SipAudioCall call) {
        sipCallState = SipCallState.ONCALLBUSY;
        uiThreadHandler.post(
                () -> {
                    events.success(sipCallState.toString());
                }
        );

    }

    @Override
    public void onError(SipAudioCall call, int errorCode,
                        String errorMessage) {
        sipCallState = SipCallState.ONERROR;
        Log.d(TAG, "call onError" + "\nError code:" + String.valueOf(errorCode) + "\nError message:" + errorMessage);
        uiThreadHandler.post(
                () -> {
                    events.success(sipCallState.toString());
                }
        );
        uiThreadHandler.post(
                () -> {
                    events.error(null,"call onError" + "\nError code:" + String.valueOf(errorCode) + "\nError message:" + errorMessage,null);
                }
        );
    }
}
