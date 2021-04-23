package me.iampato.sip_native;

public enum PjSipManagerState {

    STATE_UNDEFINED(0),
    STATE_INITED(1),
    STATE_LOGINED(2),
    STATE_CALLING(3),
    STATE_INCOMING(4),
    STATE_CONFIRMED(5),
    STATE_DISCONNECTED(6);

    private int code;

    private PjSipManagerState(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
