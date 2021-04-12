package me.iampato.sip_native;

import io.flutter.plugin.common.EventChannel;

public class CallsEventChannel implements EventChannel.StreamHandler {
    final SipDataManager sipDataManager;
    final SipCallState sipCallState;
    
    public CallsEventChannel(SipDataManager sipDataManager, SipCallState sipCallState) {
        this.sipDataManager = sipDataManager;
        this.sipCallState = sipCallState;
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        try {
            sipDataManager.setupCallStateStream(events, sipCallState);
        } catch (Exception e) {
            e.printStackTrace();
            events.error(null, e.toString(), null);
        }
    }

    @Override
    public void onCancel(Object arguments) {

    }
}
