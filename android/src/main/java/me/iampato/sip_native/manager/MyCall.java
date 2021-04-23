package me.iampato.sip_native.manager;

import org.pjsip.pjsua2.AudioMedia;
import org.pjsip.pjsua2.Call;
import org.pjsip.pjsua2.CallInfo;
import org.pjsip.pjsua2.CallMediaInfo;
import org.pjsip.pjsua2.CallMediaInfoVector;
import org.pjsip.pjsua2.Media;
import org.pjsip.pjsua2.OnCallMediaStateParam;
import org.pjsip.pjsua2.OnCallStateParam;
import org.pjsip.pjsua2.pjmedia_type;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsua_call_media_status;

public class MyCall extends Call {

    public MyCall(MyAccount acc, int call_id) {
        super(acc, call_id);

    }

    @Override
    public void onCallState(OnCallStateParam prm) {
        try {
            CallInfo ci = getInfo();
            if (ci.getState() == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED) {
                PjSipManager.mEndPoint.utilLogWrite(3, "MyCall", this.dump(true, ""));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Should not delete this call instance (self) in this context,
        // so the observer should manage this call instance deletion
        // out of this callback context.
        PjSipManager.observer.notifyCallState(this);
    }

    @Override
    public void onCallMediaState(OnCallMediaStateParam prm) {
        CallInfo callInfo;
        try {
            callInfo = getInfo();
        } catch (Exception e) {
            return;
        }

        CallMediaInfoVector mediaInfoVector = callInfo.getMedia();

        for (int i = 0; i < mediaInfoVector.size(); i++) {
            CallMediaInfo mediaInfo = mediaInfoVector.get(i);
            pjmedia_type type = mediaInfo.getType();
            pjsua_call_media_status status = mediaInfo.getStatus();

            if (type == pjmedia_type.PJMEDIA_TYPE_AUDIO
                    && (status == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE
                    || status == pjsua_call_media_status.PJSUA_CALL_MEDIA_REMOTE_HOLD)) {
                try {

                    Media media = getMedia(i);
                    AudioMedia am = AudioMedia.typecastFromMedia(media);
                    // connect ports

                    PjSipManager.mEndPoint.audDevManager().getCaptureDevMedia().startTransmit(am);
                    am.startTransmit(PjSipManager.mEndPoint.audDevManager().getPlaybackDevMedia());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


        }

        PjSipManager.observer.notifyCallMediaState(this);
    }
}
