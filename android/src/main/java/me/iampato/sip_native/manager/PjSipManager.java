package me.iampato.sip_native.manager;

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AuthCredInfo;
import org.pjsip.pjsua2.AuthCredInfoVector;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.IpChangeParam;
import org.pjsip.pjsua2.TransportConfig;
import org.pjsip.pjsua2.UaConfig;
import org.pjsip.pjsua2.pjsip_transport_type_e;


public class PjSipManager {
    public static Endpoint mEndPoint;
    public static MyAppObserver observer;
    private static volatile PjSipManager mInstance;
    private AccountConfig mAccountConfig;
    private MyAccount mAccount;

    private PjSipManager() {

    }

    public static PjSipManager getInstance() {
        if (mInstance == null)
            synchronized (PjSipManager.class) {
                if (mInstance == null)
                    mInstance = new PjSipManager();
            }
        return mInstance;
    }

    public void init(MyAppObserver obs) {
        init(obs, false);
    }

    public void init(MyAppObserver obs, boolean own_worker_thread) {
        observer = obs;

        /* Create endpoint */
        try {
            if (mEndPoint == null)
                mEndPoint = new Endpoint();
            mEndPoint.libCreate();
        } catch (Exception e) {
            return;
        }

        EpConfig epConfig = new EpConfig();

        UaConfig ua_cfg = epConfig.getUaConfig();
        ua_cfg.setUserAgent("Pjsua2 Android " + mEndPoint.libVersion().getFull());

        /* STUN server. */
        //StringVector stun_servers = new StringVector();
        //stun_servers.add("stun.pjsip.org");
        //ua_cfg.setStunServer(stun_servers);

        /* No worker thread */
        if (own_worker_thread) {
            ua_cfg.setThreadCnt(0);
            ua_cfg.setMainThreadOnly(true);
        }

        try {
            mEndPoint.libInit(epConfig);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        TransportConfig sipTpConfig = new TransportConfig();
        int SIP_PORT = 5060;

        sipTpConfig.setPort(SIP_PORT);

        // 创建一个或多个传输
        try {
            mEndPoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, sipTpConfig);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
//    try
//    {
//      mEndPoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TCP, sipTpConfig);
//    } catch (Exception e)
//    {
//      e.printStackTrace();
//    }
//
//    try
//    {
//      sipTpConfig.setPort(SIP_PORT + 1);
//      mEndPoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TLS, sipTpConfig);
//    } catch (Exception e)
//    {
//      e.printStackTrace();
//    }

        /* Start. */
        try {
            mEndPoint.libStart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleNetworkChange() {
        try {
            System.out.println("Network change detected");
            IpChangeParam changeParam = new IpChangeParam();
            mEndPoint.handleIpChange(changeParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deinit() {
        /* Try force GC to avoid late destroy of PJ objects as they should be
         * deleted before lib is destroyed.
         */
        Runtime.getRuntime().gc();

        /* Shutdown pjsua. Note that Endpoint destructor will also invoke
         * libDestroy(), so this will be a test of double libDestroy().
         */
        try {
            mEndPoint.libDestroy();
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* Force delete Endpoint here, to avoid deletion from a non-
         * registered thread (by GC?).
         */
        mEndPoint.delete();
        mEndPoint = null;
    }

    public void login(String username, String password, String ip, String port) {
        mAccountConfig = new AccountConfig();
        mAccountConfig.getNatConfig().setIceEnabled(true);
        mAccountConfig.getVideoConfig().setAutoTransmitOutgoing(false);
        mAccountConfig.getVideoConfig().setAutoShowIncoming(false);
        mAccountConfig.setIdUri("sip:" + username + "@" + ip + ":" + port);
        mAccountConfig.getRegConfig().setRegistrarUri("sip:" + ip + ":" + port);
        AuthCredInfoVector creds = mAccountConfig.getSipConfig().getAuthCreds();
        if (creds != null) {
            creds.clear();
            if (username != null && username.length() != 0)
                creds.add(new AuthCredInfo("Digest", "*", username, 0, password));
        }

        mAccount = new MyAccount(mAccountConfig);
        try {
            mAccount.create(mAccountConfig);
        } catch (Exception e) {
            e.printStackTrace();
            mAccount = null;
        }
    }

    public MyCall call(String username, String ip, String port) {
        MyCall call = new MyCall(mAccount, -1);
        CallOpParam prm = new CallOpParam(true);
        String uri = "sip:" + username + "@" + ip + ":" + port;
        try {
            call.makeCall(uri, prm);
        } catch (Exception e) {
            call.delete();
            return null;
        }
        return call;
    }

    public void logout() {
        mAccountConfig.delete();
        mAccount.delete();
    }
}
