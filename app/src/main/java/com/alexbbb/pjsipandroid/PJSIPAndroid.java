package com.alexbbb.pjsipandroid;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;

import org.pjsip.pjsua2.AudDevManager;
import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.TransportConfig;
import org.pjsip.pjsua2.pjsip_transport_type_e;

import java.util.concurrent.ConcurrentHashMap;

/**
 * PJSIP High level wrapper around PJSUA2 for Android.
 * @author alexbbb (Aleksandar Gotev)
 */
public class PJSIPAndroid {

    private static final String LOG_TAG = "PJSIPAndroid";

    private static String mBroadcastActionNamespace;
    private static ConcurrentHashMap<String, PJSIPAndroidAccount> mSipAccounts = new ConcurrentHashMap<>();
    private static Context mContext;
    private static Endpoint mEndpoint;
    private static PowerManager mPowerManager;
    private static boolean mStarted = false;
    private static PowerManager.WakeLock mStackWakeLock;
    private static Ringtone mRingTone;
    private static PJSIPAndroidBroadcastEmitter mBroadcastEmitter;
    private static boolean mDebugEnabled = false;

    // private constructor to avoid instantiation
    private PJSIPAndroid () {}

    /**
     * Initialize the library.
     * @param context application context
     * @param namespace (e.g. com.yourcompany.yourapp )
     */
    public static synchronized void initialize(final Context context, final String namespace) {

        try {
            System.loadLibrary("openh264");
            debugLog(LOG_TAG, "OpenH264 loaded");
        } catch (UnsatisfiedLinkError error) {
            Log.e(LOG_TAG, "Error while loading OpenH264 native library", error);
        }

        try {
            System.loadLibrary("yuv");
            debugLog(LOG_TAG, "libyuv loaded");
        } catch (UnsatisfiedLinkError error) {
            Log.e(LOG_TAG, "Error while loading libyuv native library", error);
        }

        try {
            System.loadLibrary("pjsua2");
            debugLog(LOG_TAG, "PJSUA2 Loaded");
        } catch (UnsatisfiedLinkError error) {
            Log.e(LOG_TAG, "Error while loading PJSUA2 native library", error);
            throw new RuntimeException(error);
        }

        mContext = context.getApplicationContext();
        mBroadcastActionNamespace = namespace;
        mBroadcastEmitter = new PJSIPAndroidBroadcastEmitter(mContext, mBroadcastActionNamespace);
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

        Uri uri = RingtoneManager.getActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE);
        mRingTone = RingtoneManager.getRingtone(mContext, uri);
    }

    private static synchronized void checkInitialization() {
        if (mContext == null || mBroadcastActionNamespace == null
                || mBroadcastActionNamespace.isEmpty()) {
            throw new RuntimeException("Please call init method first to initialize the library!");
        }
    }

    /**
     * Adds a new SIP Account and performs initial registration.
     * @param account SIP account to add
     * @return SIP account ID string (e.g. sip:user@domain.com)
     * @throws Exception if an initialization error occurs
     */
    public static synchronized String add(PJSIPAccountData account) throws Exception {
        start();

        String accountString = account.getIdUri();

        if (!mSipAccounts.containsKey(accountString)) {
            PJSIPAndroidAccount pjSipAndroidAccount = new PJSIPAndroidAccount(account);
            pjSipAndroidAccount.create();
            mSipAccounts.put(accountString, pjSipAndroidAccount);
            debugLog(LOG_TAG, "SIP account " + account.getIdUri() + " successfully added");
        }

        return accountString;
    }

    /**
     * Removes a SIP Account and performs un-registration.
     * @param accountID SIP account ID string (e.g. sip:user@domain.com)
     * @throws Exception if an error occurs while trying to remove the account
     */
    public static synchronized void remove(String accountID) throws Exception {
        checkInitialization();

        checkAccountID(accountID);

        PJSIPAndroidAccount account = mSipAccounts.get(accountID);
        if (account == null) throw new RuntimeException("No account for ID: " + accountID);

        debugLog(LOG_TAG, "Removing SIP account" + accountID);
        account.delete();
        mSipAccounts.remove(accountID);
        debugLog(LOG_TAG, "SIP account " + accountID + " successfully removed");
    }

    private static void checkAccountID(String accountID) {
        if (accountID == null || accountID.isEmpty() || !accountID.startsWith("sip:")) {
            throw new IllegalArgumentException("Invalid accountID! Example: sip:user@domain");
        }
    }

    /**
     * Get an active account instance, given its accountID.
     * @param accountID (e.g. sip:user@domain.com)
     * @return the instance of the account or null if it doesn't exist
     */
    public static synchronized PJSIPAndroidAccount get(String accountID) {
        checkInitialization();

        checkAccountID(accountID);

        return mSipAccounts.get(accountID);
    }

    /**
     * Get an active call instance, given the Account ID and the Call ID.
     * @param accountID (e.g. sip:user@domain.com)
     * @param callID the id of the call
     * @return the instance of the call or null if it doesn't exist
     */
    public static synchronized PJSIPAndroidCall getCall(String accountID, int callID) {
        PJSIPAndroidAccount account = get(accountID);
        if (account == null) return null;

        return account.getCall(callID);
    }

    /**
     * Starts PJSIP Stack
     * @return true if the start is successful, otherwise false.
     */
    public static synchronized boolean start() {
        checkInitialization();

        if (mStarted) return true;

        try {
            mStackWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PJSIPAndroid");
            mStackWakeLock.acquire();

            mEndpoint = new Endpoint();
            mEndpoint.libCreate();

            EpConfig epConfig = new EpConfig();
            mEndpoint.libInit(epConfig);

            TransportConfig udpTransport = new TransportConfig();
            TransportConfig tcpTransport = new TransportConfig();

            mEndpoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, udpTransport);
            mEndpoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TCP, tcpTransport);
            mEndpoint.libStart();

            debugLog(LOG_TAG, "PJSIP started");
            mStarted = true;
            return true;

        } catch (Exception exc) {
            mStackWakeLock.release();
            Log.e(LOG_TAG, "Error while starting PJSIP", exc);
            return false;
        }
    }

    /**
     * Shuts down PJSIP Stack
     * @throws Exception if an error occurs while trying to shut down the stack
     */
    public static synchronized void shutdown() throws Exception {
        checkInitialization();

        if (!mStarted) return;

        try {
            if (!mSipAccounts.isEmpty()) {
                for (String accountID : mSipAccounts.keySet()) {
                    remove(accountID);
                }
            }

            // try to force GC to do its job before destroying the library, since it's
            // recommended to do that by PJSUA examples
            Runtime.getRuntime().gc();

            mEndpoint.libDestroy();
            mEndpoint.delete();

            debugLog(LOG_TAG, "PJSIP stopped");

        } finally {
            if (mStackWakeLock != null && mStackWakeLock.isHeld()) {
                mStackWakeLock.release();
            }
            mStarted = false;
            mEndpoint = null;
        }
    }

    protected static synchronized String getBroadcastActionNamespace() {
        checkInitialization();

        return mBroadcastActionNamespace;
    }

    protected static synchronized AudDevManager getAudDevManager() {
        checkInitialization();

        return mEndpoint.audDevManager();
    }

    public static synchronized void startRingTone() {
        checkInitialization();
        debugLog(LOG_TAG, "starting ringtone");
        mRingTone.play();
    }

    public static synchronized void stopRingTone() {
        checkInitialization();
        debugLog(LOG_TAG, "stopping ringtone");
        mRingTone.stop();
    }

    protected static synchronized PJSIPAndroidBroadcastEmitter getBroadcastEmitter() {
        checkInitialization();

        return mBroadcastEmitter;
    }

    /**
     * Enables or disables library debug log.
     * @param value true to enable debug messages, false to disable them
     */
    public static synchronized void setDebugEnabled(boolean value) {
        mDebugEnabled = value;
    }

    protected static synchronized void debugLog(String tag, String message) {
        if (!mDebugEnabled) return;

        Log.d(tag, message);
    }
}
