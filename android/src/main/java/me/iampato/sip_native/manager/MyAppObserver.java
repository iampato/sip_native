package me.iampato.sip_native.manager;

import org.pjsip.pjsua2.pjsip_status_code;

public interface MyAppObserver {
    void notifyRegState(pjsip_status_code code, String reason, int expiration);

    void notifyIncomingCall(MyCall call);

    void notifyCallState(MyCall call);

    void notifyCallMediaState(MyCall call);

    void notifyBuddyState(MyBuddy buddy);

    void notifyChangeNetwork();
}