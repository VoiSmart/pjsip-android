package net.gotev.sipservice;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.view.Surface;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.pjsip.pjsua2.AudDevManager;
import org.pjsip.pjsua2.CodecInfo;
import org.pjsip.pjsua2.CodecInfoVector;
import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.TransportConfig;
import org.pjsip.pjsua2.VidDevManager;
import org.pjsip.pjsua2.pj_qos_type;
import org.pjsip.pjsua2.pjmedia_orient;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_transport_type_e;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static net.gotev.sipservice.SipServiceCommand.ACTION_ACCEPT_INCOMING_CALL;
import static net.gotev.sipservice.SipServiceCommand.ACTION_DECLINE_INCOMING_CALL;
import static net.gotev.sipservice.SipServiceCommand.ACTION_GET_CALL_STATUS;
import static net.gotev.sipservice.SipServiceCommand.ACTION_GET_CODEC_PRIORITIES;
import static net.gotev.sipservice.SipServiceCommand.ACTION_GET_REGISTRATION_STATUS;
import static net.gotev.sipservice.SipServiceCommand.ACTION_HANG_UP_CALL;
import static net.gotev.sipservice.SipServiceCommand.ACTION_HANG_UP_CALLS;
import static net.gotev.sipservice.SipServiceCommand.ACTION_HOLD_CALLS;
import static net.gotev.sipservice.SipServiceCommand.ACTION_MAKE_CALL;
import static net.gotev.sipservice.SipServiceCommand.ACTION_REFRESH_REGISTRATION;
import static net.gotev.sipservice.SipServiceCommand.ACTION_REMOVE_ACCOUNT;
import static net.gotev.sipservice.SipServiceCommand.ACTION_RESTART_SIP_STACK;
import static net.gotev.sipservice.SipServiceCommand.ACTION_SEND_DTMF;
import static net.gotev.sipservice.SipServiceCommand.ACTION_SET_ACCOUNT;
import static net.gotev.sipservice.SipServiceCommand.ACTION_SET_CODEC_PRIORITIES;
import static net.gotev.sipservice.SipServiceCommand.ACTION_SET_DND;
import static net.gotev.sipservice.SipServiceCommand.ACTION_SET_HOLD;
import static net.gotev.sipservice.SipServiceCommand.ACTION_SET_MUTE;
import static net.gotev.sipservice.SipServiceCommand.ACTION_SET_INCOMING_VIDEO;
import static net.gotev.sipservice.SipServiceCommand.ACTION_SET_SELF_VIDEO_ORIENTATION;
import static net.gotev.sipservice.SipServiceCommand.ACTION_START_VIDEO_PREVIEW;
import static net.gotev.sipservice.SipServiceCommand.ACTION_STOP_VIDEO_PREVIEW;
import static net.gotev.sipservice.SipServiceCommand.ACTION_TOGGLE_HOLD;
import static net.gotev.sipservice.SipServiceCommand.ACTION_TOGGLE_MUTE;
import static net.gotev.sipservice.SipServiceCommand.ACTION_TOGGLE_VIDEO_MUTE;
import static net.gotev.sipservice.SipServiceCommand.ACTION_TRANSFER_CALL;
import static net.gotev.sipservice.SipServiceCommand.AGENT_NAME;
import static net.gotev.sipservice.SipServiceCommand.PARAM_ACCOUNT_DATA;
import static net.gotev.sipservice.SipServiceCommand.PARAM_ACCOUNT_ID;
import static net.gotev.sipservice.SipServiceCommand.PARAM_CALL_ID;
import static net.gotev.sipservice.SipServiceCommand.PARAM_CODEC_PRIORITIES;
import static net.gotev.sipservice.SipServiceCommand.PARAM_DND;
import static net.gotev.sipservice.SipServiceCommand.PARAM_DTMF;
import static net.gotev.sipservice.SipServiceCommand.PARAM_HOLD;
import static net.gotev.sipservice.SipServiceCommand.PARAM_IS_VIDEO;
import static net.gotev.sipservice.SipServiceCommand.PARAM_IS_VIDEO_CONF;
import static net.gotev.sipservice.SipServiceCommand.PARAM_MUTE;
import static net.gotev.sipservice.SipServiceCommand.PARAM_NUMBER;
import static net.gotev.sipservice.SipServiceCommand.PARAM_ORIENTATION;
import static net.gotev.sipservice.SipServiceCommand.PARAM_REG_EXP_TIMEOUT;
import static net.gotev.sipservice.SipServiceCommand.PARAM_REG_CONTACT_PARAMS;
import static net.gotev.sipservice.SipServiceCommand.PARAM_SURFACE;
import static net.gotev.sipservice.SipServiceCommand.refreshRegistration;

/**
 * Sip Service.
 * @author gotev (Aleksandar Gotev)
 */
public class SipService extends BackgroundService {

    private static final String TAG = SipService.class.getSimpleName();
    private static final long[] VIBRATOR_PATTERN = {0, 1000, 1000};

    private static final String PREFS_NAME = TAG + "prefs";
    private static final String PREFS_KEY_ACCOUNTS = "accounts";
    private static final String PREFS_KEY_CODEC_PRIORITIES = "codec_priorities";
    private static final String PREFS_KEY_DND = "dnd_pref";

    private List<SipAccountData> mConfiguredAccounts = new ArrayList<>();
    private static ConcurrentHashMap<String, SipAccount> mActiveSipAccounts = new ConcurrentHashMap<>();
    private Ringtone mRingTone;
    private Vibrator mVibrator;
    private Uri mRingtoneUri;
    private BroadcastEventEmitter mBroadcastEmitter;
    private Endpoint mEndpoint;
    private volatile boolean mStarted;
    private int callStatus;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        enqueueJob(new Runnable() {
            @Override
            public void run() {
                Logger.debug(TAG, "Creating SipService with priority: " + Thread.currentThread().getPriority());

                loadNativeLibraries();

                mRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(SipService.this, RingtoneManager.TYPE_RINGTONE);
                mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

                mBroadcastEmitter = new BroadcastEventEmitter(SipService.this);
                loadConfiguredAccounts();
                addAllConfiguredAccounts();

                Logger.debug(TAG, "SipService created!");
            }
        });
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        enqueueJob(new Runnable() {
            @Override
            public void run() {
                if (intent == null) return;

                String action = intent.getAction();

                if (action == null) return;

                switch(action) {
                    case ACTION_SET_ACCOUNT:
                        handleSetAccount(intent);
                        break;
                    case ACTION_REMOVE_ACCOUNT:
                        handleRemoveAccount(intent);
                        break;
                    case ACTION_RESTART_SIP_STACK:
                        handleRestartSipStack();
                        break;
                    case ACTION_MAKE_CALL:
                        handleMakeCall(intent);
                        break;
                    case ACTION_HANG_UP_CALL:
                        handleHangUpCall(intent);
                        break;
                    case ACTION_HANG_UP_CALLS:
                        handleHangUpActiveCalls(intent);
                        break;
                    case ACTION_HOLD_CALLS:
                        handleHoldActiveCalls(intent);
                        break;
                    case ACTION_GET_CALL_STATUS:
                        handleGetCallStatus(intent);
                        break;
                    case ACTION_SEND_DTMF:
                        handleSendDTMF(intent);
                        break;
                    case ACTION_ACCEPT_INCOMING_CALL:
                        handleAcceptIncomingCall(intent);
                        break;
                    case ACTION_DECLINE_INCOMING_CALL:
                        handleDeclineIncomingCall(intent);
                        break;
                    case ACTION_SET_HOLD:
                        handleSetCallHold(intent);
                        break;
                    case ACTION_TOGGLE_HOLD:
                        handleToggleCallHold(intent);
                        break;
                    case ACTION_SET_MUTE:
                        handleSetCallMute(intent);
                        break;
                    case ACTION_TOGGLE_MUTE:
                        handleToggleCallMute(intent);
                        break;
                    case ACTION_TRANSFER_CALL:
                        handleTransferCall(intent);
                        break;
                    case ACTION_GET_CODEC_PRIORITIES:
                        handleGetCodecPriorities();
                        break;
                    case ACTION_SET_CODEC_PRIORITIES:
                        handleSetCodecPriorities(intent);
                        break;
                    case ACTION_GET_REGISTRATION_STATUS:
                        handleGetRegistrationStatus(intent);
                        break;
                    case ACTION_REFRESH_REGISTRATION:
                        handleRefreshRegistration(intent);
                        break;
                    case ACTION_SET_DND:
                        handleSetDND(intent);
                        break;
                    case ACTION_SET_INCOMING_VIDEO:
                        handleSetIncomingVideoFeed(intent);
                        break;
                    case ACTION_SET_SELF_VIDEO_ORIENTATION:
                        handleSetSelfVideoOrientation(intent);
                        break;
                    case ACTION_TOGGLE_VIDEO_MUTE:
                        handleToggleVideoMute(intent);
                        break;
                    case ACTION_START_VIDEO_PREVIEW:
                        handleStartVideoPreview(intent);
                        break;
                    case ACTION_STOP_VIDEO_PREVIEW:
                        handleStopVideoPreview(intent);
                        break;
                    default: break;
                }

                if (mConfiguredAccounts.isEmpty()) {
                    Logger.debug(TAG, "No more configured accounts. Shutting down service");
                    stopSelf();
                }
            }
        });

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        enqueueJob(new Runnable() {
            @Override
            public void run() {
                Logger.debug(TAG, "Destroying SipService");
                stopStack();
            }
        });
        super.onDestroy();
    }

    private SipCall getCall(String accountID, int callID) {
        SipAccount account = mActiveSipAccounts.get(accountID);

        if (account == null) return null;
        return account.getCall(callID);
    }

    private void notifyCallDisconnected(String accountID, int callID) {

        mBroadcastEmitter.callState(accountID, callID,
                pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED.swigValue(),
                callStatus, 0,
                false, false, false);
    }

    private void handleGetCallStatus(Intent intent) {

        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);
        int callID = intent.getIntExtra(PARAM_CALL_ID, 0);

        SipCall sipCall = getCall(accountID, callID);
        if (sipCall == null) {
            notifyCallDisconnected(accountID, callID);
            return;
        }

        int callStatusCode = callStatus;
        try {
            callStatusCode = sipCall.getInfo().getLastStatusCode().swigValue();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        mBroadcastEmitter.callState(accountID, callID, sipCall.getCurrentState().swigValue(), callStatusCode,
                                    sipCall.getConnectTimestamp(), sipCall.isLocalHold(),
                                    sipCall.isLocalMute(), sipCall.isLocalVideoMute());
    }

    private void handleSendDTMF(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);
        int callID = intent.getIntExtra(PARAM_CALL_ID, 0);
        String dtmf = intent.getStringExtra(PARAM_DTMF);

        SipCall sipCall = getCall(accountID, callID);
        if (sipCall == null) {
            notifyCallDisconnected(accountID, callID);
            return;
        }

        try {
            sipCall.dialDtmf(dtmf);
        } catch (Exception exc) {
            Logger.error(TAG, "Error while dialing dtmf: " + dtmf + ". AccountID: "
                         + accountID + ", CallID: " + callID);
        }
    }

    private void handleAcceptIncomingCall(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);
        int callID = intent.getIntExtra(PARAM_CALL_ID, 0);
        boolean isVideo = intent.getBooleanExtra(PARAM_IS_VIDEO, false);

        SipCall sipCall = getCall(accountID, callID);
        if (sipCall == null) {
            notifyCallDisconnected(accountID, callID);
            return;
        }

        try {
            sipCall.setVideoParams(isVideo, false);
            sipCall.acceptIncomingCall();
        } catch (Exception exc) {
            Logger.error(TAG, "Error while accepting incoming call. AccountID: "
                         + accountID + ", CallID: " + callID);
        }
    }

    private void handleSetCallHold(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);
        int callID = intent.getIntExtra(PARAM_CALL_ID, 0);
        boolean hold = intent.getBooleanExtra(PARAM_HOLD, false);

        SipCall sipCall = getCall(accountID, callID);
        if (sipCall == null) {
            notifyCallDisconnected(accountID, callID);
            return;
        }

        try {
            sipCall.setHold(hold);
        } catch (Exception exc) {
            Logger.error(TAG, "Error while setting hold. AccountID: "
                    + accountID + ", CallID: " + callID);
        }
    }

    private void handleToggleCallHold(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);
        int callID = intent.getIntExtra(PARAM_CALL_ID, 0);

        SipCall sipCall = getCall(accountID, callID);
        if (sipCall == null) {
            notifyCallDisconnected(accountID, callID);
            return;
        }

        try {
            sipCall.toggleHold();
        } catch (Exception exc) {
            Logger.error(TAG, "Error while toggling hold. AccountID: "
                    + accountID + ", CallID: " + callID);
        }
    }

    private void handleSetCallMute(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);
        int callID = intent.getIntExtra(PARAM_CALL_ID, 0);
        boolean mute = intent.getBooleanExtra(PARAM_MUTE, false);

        SipCall sipCall = getCall(accountID, callID);
        if (sipCall == null) {
            notifyCallDisconnected(accountID, callID);
            return;
        }

        try {
            sipCall.setMute(mute);
        } catch (Exception exc) {
            Logger.error(TAG, "Error while setting mute. AccountID: "
                         + accountID + ", CallID: " + callID);
        }
    }

    private void handleToggleCallMute(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);
        int callID = intent.getIntExtra(PARAM_CALL_ID, 0);

        SipCall sipCall = getCall(accountID, callID);
        if (sipCall == null) {
            notifyCallDisconnected(accountID, callID);
            return;
        }

        try {
            sipCall.toggleMute();
        } catch (Exception exc) {
            Logger.error(TAG, "Error while toggling mute. AccountID: "
                    + accountID + ", CallID: " + callID);
        }
    }

    private void handleDeclineIncomingCall(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);
        int callID = intent.getIntExtra(PARAM_CALL_ID, 0);

        SipCall sipCall = getCall(accountID, callID);
        if (sipCall == null) {
            notifyCallDisconnected(accountID, callID);
            return;
        }

        try {
            sipCall.declineIncomingCall();
        } catch (Exception exc) {
            Logger.error(TAG, "Error while declining incoming call. AccountID: "
                    + accountID + ", CallID: " + callID);
        }
    }

    private void handleHangUpCall(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);
        int callID = intent.getIntExtra(PARAM_CALL_ID, 0);

        try {
            SipCall sipCall = getCall(accountID, callID);

            if (sipCall == null) {
                notifyCallDisconnected(accountID, callID);
                return;
            }

            sipCall.hangUp();

        } catch (Exception exc) {
            Logger.error(TAG, "Error while hanging up call", exc);
            notifyCallDisconnected(accountID, callID);
        }
    }

    private void handleHangUpActiveCalls(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);

        SipAccount account = mActiveSipAccounts.get(accountID);
        if (account == null) return;

        Set<Integer> activeCallIDs = account.getCallIDs();

        if (activeCallIDs == null || activeCallIDs.isEmpty()) return;

        for (int callID : activeCallIDs) {
            try {
                SipCall sipCall = getCall(accountID, callID);

                if (sipCall == null) {
                    notifyCallDisconnected(accountID, callID);
                    return;
                }

                sipCall.hangUp();
            } catch (Exception exc) {
                Logger.error(TAG, "Error while hanging up call", exc);
                notifyCallDisconnected(accountID, callID);
            }
        }
    }

    private void handleHoldActiveCalls(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);

        SipAccount account = mActiveSipAccounts.get(accountID);
        if (account == null) return;

        Set<Integer> activeCallIDs = account.getCallIDs();

        if (activeCallIDs == null || activeCallIDs.isEmpty()) return;

        for (int callID : activeCallIDs) {
            try {
                SipCall sipCall = getCall(accountID, callID);

                if (sipCall == null) {
                    notifyCallDisconnected(accountID, callID);
                    return;
                }

                sipCall.setHold(true);
            } catch (Exception exc) {
                Logger.error(TAG, "Error while holding call", exc);
            }
        }
    }

    private void handleTransferCall(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);
        int callID = intent.getIntExtra(PARAM_CALL_ID, 0);
        String number = intent.getStringExtra(PARAM_NUMBER);

        try {
            SipCall sipCall = getCall(accountID, callID);

            if (sipCall == null) {
                notifyCallDisconnected(accountID, callID);
                return;
            }

            sipCall.transferTo(number);

        } catch (Exception exc) {
            Logger.error(TAG, "Error while transferring call to " + number, exc);
            notifyCallDisconnected(accountID, callID);
        }
    }

    private void handleMakeCall(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);
        String number = intent.getStringExtra(PARAM_NUMBER);
        boolean isVideo = intent.getBooleanExtra(PARAM_IS_VIDEO, false);
        boolean isVideoConference = false;
        if (isVideo) {
            isVideoConference = intent.getBooleanExtra(PARAM_IS_VIDEO_CONF, false);
        }

        Logger.debug(TAG, "Making call to " + number);

        try {
            SipCall call = mActiveSipAccounts.get(accountID).addOutgoingCall(number, isVideo, isVideoConference);
            call.setVideoParams(isVideo, isVideoConference);
            mBroadcastEmitter.outgoingCall(accountID, call.getId(), number, isVideo, isVideoConference);
        } catch (Exception exc) {
            Logger.error(TAG, "Error while making outgoing call", exc);
            mBroadcastEmitter.outgoingCall(accountID, -1, number, false, false);
        }
    }

    private void handleRefreshRegistration(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);
        int regExpTimeout = intent.getIntExtra(PARAM_REG_EXP_TIMEOUT, 0);
        String regContactParams = intent.getStringExtra(PARAM_REG_CONTACT_PARAMS);
        boolean refresh = true;
        if (!mActiveSipAccounts.isEmpty() && mActiveSipAccounts.containsKey(accountID)){
            try {
                SipAccount sipAccount = mActiveSipAccounts.get(accountID);
                if (regExpTimeout != 0 && regExpTimeout != sipAccount.getData().getRegExpirationTimeout()) {
                    sipAccount.getData().setRegExpirationTimeout(regExpTimeout);
                    Logger.debug(TAG, String.valueOf(regExpTimeout));
                    refresh = false;
                }
                if (regContactParams != null && !(String.valueOf(regContactParams).equals(sipAccount.getData().getContactUriParams()))) {
                    Logger.debug(TAG, regContactParams);
                    sipAccount.getData().setContactUriParams(regContactParams);
                    refresh = false;
                    mActiveSipAccounts.put(accountID, sipAccount);
                    mConfiguredAccounts.clear();
                    mConfiguredAccounts.add(sipAccount.getData());
                    persistConfiguredAccounts();
                }
                if (refresh) {
                    sipAccount.setRegistration(true);
                } else {
                    sipAccount.modify(sipAccount.getData().getAccountConfig());
                    sipAccount.getData().setRegExpirationTimeout(100);
                }
            } catch (Exception ex) {
                Logger.error(TAG, "Error while refreshing registration");
                ex.printStackTrace();
            }
        } else {
            Logger.debug(TAG, "account "+accountID+" not set");
        }
    }

    private void handleRestartSipStack() {
        Logger.debug(TAG, "Restarting SIP stack");
        stopStack();
        addAllConfiguredAccounts();
    }

    private void handleResetAccounts(Intent intent) {
        Logger.debug(TAG, "Removing all the configured accounts");

        Iterator<SipAccountData> iterator = mConfiguredAccounts.iterator();

        while (iterator.hasNext()) {
            SipAccountData data = iterator.next();

            try {
                removeAccount(data.getIdUri());
                iterator.remove();
            } catch (Exception exc) {
                Logger.error(TAG, "Error while removing account " + data.getIdUri(), exc);
            }
        }

        persistConfiguredAccounts();
    }

    private void handleRemoveAccount(Intent intent) {
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
    }

    private void handleSetAccount(Intent intent) {
        SipAccountData data = intent.getParcelableExtra(PARAM_ACCOUNT_DATA);

        int index = mConfiguredAccounts.indexOf(data);
        if (index == -1) {
            handleResetAccounts(intent);
            Logger.debug(TAG, "Adding " + data.getIdUri());

            try {
                handleSetCodecPriorities(intent);
                addAccount(data);
                mConfiguredAccounts.add(data);
                persistConfiguredAccounts();
            } catch (Exception exc) {
                Logger.error(TAG, "Error while adding " + data.getIdUri(), exc);
            }
        } else {
            Logger.debug(TAG, "Reconfiguring " + data.getIdUri());

            try {
                //removeAccount(data.getIdUri());
                handleSetCodecPriorities(intent);
                addAccount(data);
                mConfiguredAccounts.set(index, data);
                persistConfiguredAccounts();
            } catch (Exception exc) {
                Logger.error(TAG, "Error while reconfiguring " + data.getIdUri(), exc);
            }
        }
    }

    private void loadNativeLibraries() {
        try {
            System.loadLibrary("openh264");
            Logger.debug(TAG, "OpenH264 loaded");
        } catch (UnsatisfiedLinkError error) {
            Logger.error(TAG, "Error while loading OpenH264 native library", error);
            throw new RuntimeException(error);
        }

        // libYUV removed -> integrated from pjsip 2.6 and later

        try {
            System.loadLibrary("pjsua2");
            Logger.debug(TAG, "PJSIP pjsua2 loaded");
        } catch (UnsatisfiedLinkError error) {
            Logger.error(TAG, "Error while loading PJSIP pjsua2 native library", error);
            throw new RuntimeException(error);
        }
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
            epConfig.getMedConfig().setHasIoqueue(true);
            epConfig.getMedConfig().setClockRate(16000);
            epConfig.getMedConfig().setQuality(10);
            epConfig.getMedConfig().setEcOptions(1);
            epConfig.getMedConfig().setEcTailLen(200);
            epConfig.getMedConfig().setThreadCnt(2);
            mEndpoint.libInit(epConfig);

            TransportConfig udpTransport = new TransportConfig();
            udpTransport.setQosType(pj_qos_type.PJ_QOS_TYPE_VOICE);
            TransportConfig tcpTransport = new TransportConfig();
            tcpTransport.setQosType(pj_qos_type.PJ_QOS_TYPE_VOICE);

            mEndpoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, udpTransport);
            mEndpoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TCP, tcpTransport);
            mEndpoint.libStart();

            ArrayList<CodecPriority> codecPriorities = getConfiguredCodecPriorities();
            if (codecPriorities != null) {
                Logger.debug(TAG, "Setting saved codec priorities...");
                for (CodecPriority codecPriority : codecPriorities) {
                    Logger.debug(TAG, "Setting " + codecPriority.getCodecId() + " priority to " + codecPriority.getPriority());
                    mEndpoint.codecSetPriority(codecPriority.getCodecId(), (short) codecPriority.getPriority());
                }
                Logger.debug(TAG, "Saved codec priorities set!");
            } else {
                mEndpoint.codecSetPriority("PCMA/8000", (short) (CodecPriority.PRIORITY_MAX - 1));
                mEndpoint.codecSetPriority("PCMU/8000", (short) (CodecPriority.PRIORITY_MAX - 2));
                mEndpoint.codecSetPriority("G729/8000", (short) CodecPriority.PRIORITY_DISABLED);
                mEndpoint.codecSetPriority("speex/8000", (short) CodecPriority.PRIORITY_DISABLED);
                mEndpoint.codecSetPriority("speex/16000", (short) CodecPriority.PRIORITY_DISABLED);
                mEndpoint.codecSetPriority("speex/32000", (short) CodecPriority.PRIORITY_DISABLED);
                mEndpoint.codecSetPriority("GSM/8000", (short) CodecPriority.PRIORITY_DISABLED);
                mEndpoint.codecSetPriority("G722/16000", (short) CodecPriority.PRIORITY_DISABLED);
                mEndpoint.codecSetPriority("G7221/16000", (short) CodecPriority.PRIORITY_DISABLED);
                mEndpoint.codecSetPriority("G7221/32000", (short) CodecPriority.PRIORITY_DISABLED);
                mEndpoint.codecSetPriority("ilbc/8000", (short) CodecPriority.PRIORITY_DISABLED);
            }

            Logger.debug(TAG, "PJSIP started!");
            mStarted = true;
            mBroadcastEmitter.stackStatus(true);

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
            mEndpoint = null;

            Logger.debug(TAG, "PJSIP stopped");
            mBroadcastEmitter.stackStatus(false);

        } catch (Exception exc) {
            Logger.error(TAG, "Error while stopping PJSIP", exc);

        } finally {
            mStarted = false;
            mEndpoint = null;
        }
    }

    private ArrayList<CodecPriority> getCodecPriorityList() {
        startStack();

        if (!mStarted) {
            Logger.error(TAG, "Can't get codec priority list! The SIP Stack has not been " +
                    "initialized! Add an account first!");
            return null;
        }

        try {
            CodecInfoVector codecs = mEndpoint.codecEnum();
            if (codecs == null || codecs.size() == 0) return null;

            ArrayList<CodecPriority> codecPrioritiesList = new ArrayList<>((int)codecs.size());

            for (int i = 0; i < (int)codecs.size(); i++) {
                CodecInfo codecInfo = codecs.get(i);
                CodecPriority newCodec = new CodecPriority(codecInfo.getCodecId(),
                                                           codecInfo.getPriority());
                if (!codecPrioritiesList.contains(newCodec))
                    codecPrioritiesList.add(newCodec);
                codecInfo.delete();
            }

            codecs.delete();

            Collections.sort(codecPrioritiesList);
            return codecPrioritiesList;

        } catch (Exception exc) {
            Logger.error(TAG, "Error while getting codec priority list!", exc);
            return null;
        }
    }

    private void handleGetCodecPriorities() {
        ArrayList<CodecPriority> codecs = getCodecPriorityList();

        if (codecs != null) {
            mBroadcastEmitter.codecPriorities(codecs);
        }
    }

    private void handleSetCodecPriorities(Intent intent) {
        ArrayList<CodecPriority> codecPriorities = intent.getParcelableArrayListExtra(PARAM_CODEC_PRIORITIES);

        if (codecPriorities == null) {
            return;
        }

        startStack();

        if (!mStarted) {
            mBroadcastEmitter.codecPrioritiesSetStatus(false);
            return;
        }

        try {
            StringBuilder log = new StringBuilder();
            log.append("Codec priorities successfully set. The priority order is now:\n");

            for (CodecPriority codecPriority : codecPriorities) {
                mEndpoint.codecSetPriority(codecPriority.getCodecId(), (short) codecPriority.getPriority());
                log.append(codecPriority.toString()).append("\n");
            }

            persistConfiguredCodecPriorities(codecPriorities);
            Logger.debug(TAG, log.toString());
            mBroadcastEmitter.codecPrioritiesSetStatus(true);

        } catch (Exception exc) {
            Logger.error(TAG, "Error while setting codec priorities", exc);
            mBroadcastEmitter.codecPrioritiesSetStatus(false);
        }
    }

    private void handleGetRegistrationStatus(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);

        if (!mStarted || mActiveSipAccounts.get(accountID) == null) {
            mBroadcastEmitter.registrationState("", 400);
            return;
        }

        SipAccount account = mActiveSipAccounts.get(accountID);
        try {
            mBroadcastEmitter.registrationState(accountID, account.getInfo().getRegStatus().swigValue());
        } catch (Exception exc) {
            Logger.error(TAG, "Error while getting registration status for " + accountID, exc);
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

        SipAccount sipAccount = mActiveSipAccounts.get(accountString);

        if (sipAccount == null || !sipAccount.isValid() || !account.equals(sipAccount.getData())) {
            if (mActiveSipAccounts.containsKey(accountString)) {
                sipAccount.delete();
            }
            startStack();
            SipAccount pjSipAndroidAccount = new SipAccount(this, account);
            pjSipAndroidAccount.create();
            mActiveSipAccounts.put(accountString, pjSipAndroidAccount);
            Logger.debug(TAG, "SIP account " + account.getIdUri() + " successfully added");
        } else {
            sipAccount.setRegistration(true);
        }
    }

    /**
     * Removes a SIP Account and performs un-registration.
     */
    private void removeAccount(String accountID) throws Exception {
        SipAccount account = mActiveSipAccounts.remove(accountID);

        if (account == null) {
            Logger.error(TAG, "No account for ID: " + accountID);
            return;
        }

        Logger.debug(TAG, "Removing SIP account " + accountID);
        account.delete();
        Logger.debug(TAG, "SIP account " + accountID + " successfully removed");
    }

    private void persistConfiguredAccounts() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(PREFS_KEY_ACCOUNTS, new Gson().toJson(mConfiguredAccounts)).apply();
    }

    private void persistConfiguredCodecPriorities(ArrayList<CodecPriority> codecPriorities) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(PREFS_KEY_CODEC_PRIORITIES, new Gson().toJson(codecPriorities)).apply();
    }

    private ArrayList<CodecPriority> getConfiguredCodecPriorities() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        String codecPriorities = prefs.getString(PREFS_KEY_CODEC_PRIORITIES, "");
        if (codecPriorities.isEmpty()) {
            return null;
        }

        Type listType = new TypeToken<ArrayList<CodecPriority>>(){}.getType();
        return new Gson().fromJson(codecPriorities, listType);
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
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        try {
            if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                mVibrator.vibrate(VIBRATOR_PATTERN, 0);
                mRingTone = RingtoneManager.getRingtone(this, mRingtoneUri);
                mRingTone.play();
            } else if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE){
                mVibrator.vibrate(VIBRATOR_PATTERN, 0);
            }
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
        }
    }

    protected synchronized AudDevManager getAudDevManager() {
        return mEndpoint.audDevManager();
    }

    protected synchronized VidDevManager getVidDevManager() {
        return mEndpoint.vidDevManager();
    }

    protected BroadcastEventEmitter getBroadcastEmitter() {
        return mBroadcastEmitter;
    }

    /****           TEST STUFF DO NOT USE               ****/
    public void checkRegistrationTimeout(final String message, final String id){
        enqueueJob(new Runnable() {
            @Override
            public void run() {
                SharedPreferences preferences = getSharedPreferences("push_preferences", Context.MODE_PRIVATE);
                String tok = preferences.getString("fcm_token", "");
                int expire = 0;
                boolean found = false, validMessage = false;
                int indexTok = 0, indexLastTok = 0, indexExp = 0, indexLastExp = 0, indexSemiColon = 0;
                indexTok = message.indexOf("pn-tok=");
                while (!found && !tok.isEmpty() && indexTok != -1) {
                    validMessage = true;
                    indexTok += 6;
                    indexSemiColon = message.indexOf(";", indexTok);
                    if (message.substring(indexTok + 1, indexSemiColon).equals(tok)) {
                        String exp = message.substring((message.indexOf("=", indexSemiColon) + 1), message.indexOf("\r", indexSemiColon));
                        expire = Integer.parseInt(exp);
                        if (expire > TimeUnit.DAYS.toSeconds(4L)){
                            found = true;
                        }
                    }
                    indexTok = message.indexOf("pn-tok=", indexTok);
                }
                if (!found && validMessage){
                    enqueueDelayedJob(new Runnable() {
                        @Override
                        public void run() {
                            refreshRegistration(SipService.this, id, 604800, null);
                        }
                    }, 5000);
                }
            }
        });
    }

    /****           END TEST STUFF               ****/

    public void setLastCallStatus(int callStatus) {
        this.callStatus = callStatus;
    }

    private void handleSetDND(Intent intent) {
        boolean dnd = intent.getBooleanExtra(PARAM_DND, false);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(PREFS_KEY_DND, dnd).apply();
    }

    public boolean isDND() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(PREFS_KEY_DND, false);
    }

    private void handleSetIncomingVideoFeed(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);
        int callID = intent.getIntExtra(PARAM_CALL_ID, 0);
        SipCall sipCall = getCall(accountID, callID);

        if (sipCall == null) {
            notifyCallDisconnected(accountID, callID);
            return;
        }
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Surface surface = intent.getExtras().getParcelable(PARAM_SURFACE);
            sipCall.setIncomingVideoFeed(surface);
        }
    }
    private void handleSetSelfVideoOrientation(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);
        int orientSwigValue = intent.getIntExtra(PARAM_ORIENTATION, -1);
        SipAccount sipAccount = mActiveSipAccounts.get(accountID);
        if (sipAccount != null && orientSwigValue > 0) {
            try {
                getVidDevManager().setCaptureOrient(
                        sipAccount.getData().getAccountConfig().getVideoConfig().getDefaultCaptureDevice(),
                        pjmedia_orient.swigToEnum(orientSwigValue), true);

            } catch (Exception iex) {
                Logger.error(TAG, "Error while changing video orientation");
            }
        }
    }

    private void handleToggleVideoMute(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);
        int callID = intent.getIntExtra(PARAM_CALL_ID, 0);
        SipCall sipCall = getCall(accountID, callID);

        if (sipCall == null) {
            notifyCallDisconnected(accountID, callID);
            return;
        }

        sipCall.toggleVideoMute();
    }

    private void handleStartVideoPreview(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);
        int callID = intent.getIntExtra(PARAM_CALL_ID, 0);
        SipCall sipCall = getCall(accountID, callID);

        if (sipCall == null) {
            notifyCallDisconnected(accountID, callID);
            return;
        }
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Surface surface = intent.getExtras().getParcelable(PARAM_SURFACE);
            sipCall.startPreviewVideoFeed(surface);
        }
    }

    private void handleStopVideoPreview(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);
        int callID = intent.getIntExtra(PARAM_CALL_ID, 0);
        SipCall sipCall = getCall(accountID, callID);

        if (sipCall == null) {
            notifyCallDisconnected(accountID, callID);
            return;
        }

        sipCall.stopPreviewVideoFeed();
    }
}
