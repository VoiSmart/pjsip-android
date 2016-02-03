package net.gotev.sipservice;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.pjsip.pjsua2.AudDevManager;
import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.TransportConfig;
import org.pjsip.pjsua2.pjsip_transport_type_e;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sip Service.
 * @author gotev (Aleksandar Gotev)
 */
public class SipService extends Service {

    public static String AGENT_NAME = "AndroidSipService/" + BuildConfig.VERSION_CODE;
    private static final String TAG = SipService.class.getSimpleName();
    private static final long[] VIBRATOR_PATTERN = {0, 1000, 1000};

    private static final String PREFS_NAME = TAG + "prefs";
    private static final String PREFS_KEY_ACCOUNTS = "accounts";

    private static final String ACTION_RESTART_SIP_STACK = "restartSipStack";
    private static final String ACTION_SET_ACCOUNT = "setAccount";
    private static final String ACTION_REMOVE_ACCOUNT = "removeAccount";
    private static final String PARAM_ACCOUNT_DATA = "accountData";
    private static final String PARAM_ACCOUNT_ID = "accountID";

    private List<SipAccountData> mConfiguredAccounts = new ArrayList<>();
    private static ConcurrentHashMap<String, SipAccount> mActiveSipAccounts = new ConcurrentHashMap<>();
    private PowerManager.WakeLock mWakeLock;
    private MediaPlayer mRingTone;
    private AudioManager mAudioManager;
    private Vibrator mVibrator;
    private Uri mRingtoneUri;
    private SipServiceBroadcastEmitter mBroadcastEmitter;
    private Endpoint mEndpoint;
    private boolean mStarted;

    /**
     * Adds a new SIP account.
     * @param context application context
     * @param sipAccount sip account data
     * @return sip account ID uri as a string
     */
    public static String setAccount(Context context, SipAccountData sipAccount) {
        if (sipAccount == null) {
            throw new IllegalArgumentException("sipAccount MUST not be null!");
        }

        String accountID = sipAccount.getIdUri();
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_SET_ACCOUNT);
        intent.putExtra(PARAM_ACCOUNT_DATA, sipAccount);
        context.startService(intent);

        return accountID;
    }

    /**
     * Remove a SIP account.
     * @param context application context
     * @param accountID account ID uri
     */
    public static void removeAccount(Context context, String accountID) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_REMOVE_ACCOUNT);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        context.startService(intent);
    }

    /**
     * Starts the SIP service.
     * @param context application context
     */
    public static void start(Context context) {
        context.startService(new Intent(context, SipService.class));
    }

    /**
     * Stops the SIP service.
     * @param context application context
     */
    public static void stop(Context context) {
        context.stopService(new Intent(context, SipService.class));
    }

    /**
     * Restarts the SIP stack without restarting the service.
     * @param context application context
     */
    public static void restartSipStack(Context context) {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_RESTART_SIP_STACK);
        context.startService(intent);
    }

    /**
     * Get an active account instance, given its accountID.
     * @param accountID (e.g. sip:user@domain.com)
     * @return the instance of the account or null if it doesn't exist
     */
    public static synchronized SipAccount get(String accountID) {
        return mActiveSipAccounts.get(accountID);
    }

    /**
     * Get an active call instance, given the Account ID and the Call ID.
     * @param accountID (e.g. sip:user@domain.com)
     * @param callID the id of the call
     * @return the instance of the call or null if it doesn't exist
     */
    public static synchronized SipCall getCall(String accountID, int callID) {
        SipAccount account = get(accountID);
        if (account == null) return null;

        return account.getCall(callID);
    }

    private static void checkAccount(String accountID) {
        if (accountID == null || accountID.isEmpty() || !accountID.startsWith("sip:")) {
            throw new IllegalArgumentException("Invalid accountID! Example: sip:user@domain");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Logger.debug(TAG, "Creating SipService");

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        mWakeLock.acquire();

        try {
            System.loadLibrary("openh264");
            Logger.debug(TAG, "OpenH264 loaded");
        } catch (UnsatisfiedLinkError error) {
            Logger.error(TAG, "Error while loading OpenH264 native library", error);
            throw new RuntimeException(error);
        }

        try {
            System.loadLibrary("yuv");
            Logger.debug(TAG, "libyuv loaded");
        } catch (UnsatisfiedLinkError error) {
            Logger.error(TAG, "Error while loading libyuv native library", error);
            throw new RuntimeException(error);
        }

        try {
            System.loadLibrary("pjsua2");
            Logger.debug(TAG, "PJSIP pjsua2 loaded");
        } catch (UnsatisfiedLinkError error) {
            Logger.error(TAG, "Error while loading PJSIP pjsua2 native library", error);
            throw new RuntimeException(error);
        }

        mRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mBroadcastEmitter = new SipServiceBroadcastEmitter(this);
        loadConfiguredAccounts();
        addAllConfiguredAccounts();

        Logger.debug(TAG, "SipService created!");

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Logger.debug(TAG, "Destroying SipService");
        stopStack();
        Logger.debug(TAG, "SipService destroyed!");
        mWakeLock.release();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (ACTION_SET_ACCOUNT.equals(intent.getAction())) {
                startStack();
                SipAccountData data = intent.getParcelableExtra(PARAM_ACCOUNT_DATA);

                int index = mConfiguredAccounts.indexOf(data);
                if (index == -1) {
                    Logger.debug(TAG, "Adding " + data.getIdUri());

                    try {
                        addAccount(data);
                        mConfiguredAccounts.add(data);
                        persistConfiguredAccounts();
                    } catch (Exception exc) {
                        Logger.error(TAG, "Error while adding " + data.getIdUri(), exc);
                    }
                } else {
                    Logger.debug(TAG, "Reconfiguring " + data.getIdUri());

                    try {
                        removeAccount(data.getIdUri());
                        addAccount(data);
                        mConfiguredAccounts.set(index, data);
                        persistConfiguredAccounts();
                    } catch (Exception exc) {
                        Logger.error(TAG, "Error while reconfiguring " + data.getIdUri(), exc);
                    }
                }

            } else if (ACTION_REMOVE_ACCOUNT.equals(intent.getAction())) {
                String accountIDtoRemove = intent.getStringExtra(PARAM_ACCOUNT_ID);

                Logger.debug(TAG, "Removing " + accountIDtoRemove);

                Iterator<SipAccountData> iterator = mConfiguredAccounts.iterator();

                while (iterator.hasNext()) {
                    SipAccountData data = iterator.next();

                    if (data.getIdUri().equals(accountIDtoRemove)) {
                        try {
                            removeAccount(accountIDtoRemove);
                            iterator.remove();
                            persistConfiguredAccounts();
                        } catch (Exception exc) {
                            Logger.error(TAG, "Error while removing account " + accountIDtoRemove, exc);
                        }
                        break;
                    }
                }
            } else if (ACTION_RESTART_SIP_STACK.equals(intent.getAction())) {
                stopStack();
                addAllConfiguredAccounts();
            }
        }

        if (mConfiguredAccounts.isEmpty()) {
            Logger.debug(TAG, "No more configured accounts. Shutting down service");
            stopSelf();
            return START_NOT_STICKY;
        }

        return START_STICKY;
    }

    /**
     * Starts PJSIP Stack.
     */
    private void startStack() {

        if (mStarted) return;

        try {
            Logger.debug(TAG, "Starting PJSIP");
            mEndpoint = new Endpoint();
            mEndpoint.libCreate();

            EpConfig epConfig = new EpConfig();
            epConfig.getUaConfig().setUserAgent(AGENT_NAME);
            mEndpoint.libInit(epConfig);

            TransportConfig udpTransport = new TransportConfig();
            TransportConfig tcpTransport = new TransportConfig();

            mEndpoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, udpTransport);
            mEndpoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TCP, tcpTransport);
            mEndpoint.libStart();
            mEndpoint.codecSetPriority("G729/8000", (short) 255);

            Logger.debug(TAG, "PJSIP started!");
            mStarted = true;

        } catch (Exception exc) {
            Logger.error(TAG, "Error while starting PJSIP", exc);
            mStarted = false;
        }
    }

    /**
     * Shuts down PJSIP Stack
     * @throws Exception if an error occurs while trying to shut down the stack
     */
    private void stopStack() {

        if (!mStarted) return;

        try {
            Logger.debug(TAG, "Stopping PJSIP");

            removeAllActiveAccounts();

            // try to force GC to do its job before destroying the library, since it's
            // recommended to do that by PJSUA examples
            Runtime.getRuntime().gc();

            mEndpoint.libDestroy();
            mEndpoint.delete();

            Logger.debug(TAG, "PJSIP stopped");

        } catch (Exception exc) {
            Logger.error(TAG, "Error while stopping PJSIP", exc);

        } finally {
            mStarted = false;
            mEndpoint = null;
        }
    }

    private void removeAllActiveAccounts() {
        if (!mActiveSipAccounts.isEmpty()) {
            for (String accountID : mActiveSipAccounts.keySet()) {
                try {
                    removeAccount(accountID);
                } catch (Exception exc) {
                    Logger.error(TAG, "Error while removing " + accountID);
                }
            }
        }
    }

    private void addAllConfiguredAccounts() {
        if (!mConfiguredAccounts.isEmpty()) {
            for (SipAccountData accountData : mConfiguredAccounts) {
                try {
                    addAccount(accountData);
                } catch (Exception exc) {
                    Logger.error(TAG, "Error while adding " + accountData.getIdUri());
                }
            }
        }
    }

    /**
     * Adds a new SIP Account and performs initial registration.
     * @param account SIP account to add
     */
    private void addAccount(SipAccountData account) throws Exception {
        String accountString = account.getIdUri();

        if (!mActiveSipAccounts.containsKey(accountString)) {
            startStack();
            SipAccount pjSipAndroidAccount = new SipAccount(this, account);
            pjSipAndroidAccount.create();
            mActiveSipAccounts.put(accountString, pjSipAndroidAccount);
            Logger.debug(TAG, "SIP account " + account.getIdUri() + " successfully added");
        }
    }

    /**
     * Removes a SIP Account and performs un-registration.
     */
    private void removeAccount(String accountID) throws Exception {
        SipAccount account = mActiveSipAccounts.get(accountID);

        if (account == null) {
            Logger.error(TAG, "No account for ID: " + accountID);
            return;
        }

        Logger.debug(TAG, "Removing SIP account" + accountID);
        account.delete();
        mActiveSipAccounts.remove(accountID);
        Logger.debug(TAG, "SIP account " + accountID + " successfully removed");
    }

    private void persistConfiguredAccounts() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(PREFS_KEY_ACCOUNTS, new Gson().toJson(mConfiguredAccounts)).apply();
    }

    private void loadConfiguredAccounts() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        String accounts = prefs.getString(PREFS_KEY_ACCOUNTS, "");

        if (accounts.isEmpty()) {
            mConfiguredAccounts = new ArrayList<>();
        } else {
            Type listType = new TypeToken<ArrayList<SipAccountData>>(){}.getType();
            mConfiguredAccounts = new Gson().fromJson(accounts, listType);
        }
    }

    protected synchronized void startRingtone() {
        mVibrator.vibrate(VIBRATOR_PATTERN, 0);

        try {
            mRingTone = MediaPlayer.create(this, mRingtoneUri);
            mRingTone.setLooping(true);

            int volume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
            mRingTone.setVolume(volume, volume);

            mRingTone.start();
        } catch (Exception exc) {
            Logger.error(TAG, "Error while trying to play ringtone!", exc);
        }
    }

    protected synchronized void stopRingtone() {
        mVibrator.cancel();

        if (mRingTone != null) {
            try {
                if (mRingTone.isPlaying())
                    mRingTone.stop();
            } catch (Exception ignored) { }

            try {
                mRingTone.reset();
                mRingTone.release();
            } catch (Exception ignored) { }
        }
    }

    protected synchronized AudDevManager getAudDevManager() {
        return mEndpoint.audDevManager();
    }

    protected SipServiceBroadcastEmitter getBroadcastEmitter() {
        return mBroadcastEmitter;
    }
}
