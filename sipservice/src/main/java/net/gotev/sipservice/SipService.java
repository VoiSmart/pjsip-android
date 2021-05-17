package net.gotev.sipservice;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Surface;

import org.pjsip.pjsua2.AudDevManager;
import org.pjsip.pjsua2.CallVidSetStreamParam;
import org.pjsip.pjsua2.CodecFmtpVector;
import org.pjsip.pjsua2.CodecInfo;
import org.pjsip.pjsua2.CodecInfoVector2;
import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.IpChangeParam;
import org.pjsip.pjsua2.MediaFormatVideo;
import org.pjsip.pjsua2.TransportConfig;
import org.pjsip.pjsua2.VidCodecParam;
import org.pjsip.pjsua2.VidDevManager;
import org.pjsip.pjsua2.pj_qos_type;
import org.pjsip.pjsua2.pjmedia_orient;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_transport_type_e;
import org.pjsip.pjsua2.pjsua_call_vid_strm_op;
import org.pjsip.pjsua2.pjsua_destroy_flag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static net.gotev.sipservice.SipServiceCommand.AGENT_NAME;

/**
 * Sip Service.
 * @author gotev (Aleksandar Gotev)
 */
public class SipService extends BackgroundService implements SipServiceConstants {

    private static final String TAG = SipService.class.getSimpleName();

    private List<SipAccountData> mConfiguredAccounts = new ArrayList<>();
    private SipAccountData mConfiguredGuestAccount;
    private static final ConcurrentHashMap<String, SipAccount> mActiveSipAccounts = new ConcurrentHashMap<>();
    private BroadcastEventEmitter mBroadcastEmitter;
    private Endpoint mEndpoint;
    private SharedPreferencesHelper mSharedPreferencesHelper;
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
                mSharedPreferencesHelper = SharedPreferencesHelper.getInstance(SipService.this)
                        .init(SipService.this);
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
                    case ACTION_SET_VIDEO_MUTE:
                        handleSetVideoMute(intent);
                        break;
                    case ACTION_START_VIDEO_PREVIEW:
                        handleStartVideoPreview(intent);
                        break;
                    case ACTION_STOP_VIDEO_PREVIEW:
                        handleStopVideoPreview(intent);
                        break;
                    case ACTION_SWITCH_VIDEO_CAPTURE_DEVICE:
                        handleSwitchVideoCaptureDevice(intent);
                        break;
                    case ACTION_MAKE_DIRECT_CALL:
                        handleMakeDirectCall(intent);
                        break;
                    case ACTION_RECONNECT_CALL:
                        handleReconnectCall();
                        break;
                    default: break;
                }

                if (mConfiguredAccounts.isEmpty() && mConfiguredGuestAccount == null) {
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
                pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED,
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
            callStatusCode = sipCall.getInfo().getLastStatusCode();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        mBroadcastEmitter.callState(accountID, callID, sipCall.getCurrentState(), callStatusCode,
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
                if (regContactParams != null && !(regContactParams.equals(sipAccount.getData().getContactUriParams()))) {
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

    private void handleResetAccounts() {
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
            handleResetAccounts();
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
            System.loadLibrary("c++_shared");
            Logger.debug(TAG, "libc++_shared loaded");
        } catch (UnsatisfiedLinkError error) {
            Logger.error(TAG, "Error while loading libc++_shared native library", error);
            throw new RuntimeException(error);
        }

        try {
            System.loadLibrary("openh264");
            Logger.debug(TAG, "OpenH264 loaded");
        } catch (UnsatisfiedLinkError error) {
            Logger.error(TAG, "Error while loading OpenH264 native library", error);
            throw new RuntimeException(error);
        }

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
            mEndpoint = new SipEndpoint(this);
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
                mEndpoint.codecSetPriority("OPUS", (short) (CodecPriority.PRIORITY_MAX - 1));
                mEndpoint.codecSetPriority("PCMA/8000", (short) (CodecPriority.PRIORITY_MAX - 2));
                mEndpoint.codecSetPriority("PCMU/8000", (short) (CodecPriority.PRIORITY_MAX -3));
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

            // Set H264 Parameters
            VidCodecParam vidCodecParam = mEndpoint.getVideoCodecParam(H264_CODEC_ID);
            CodecFmtpVector codecFmtpVector = vidCodecParam.getDecFmtp();
            MediaFormatVideo mediaFormatVideo = vidCodecParam.getEncFmt();
            mediaFormatVideo.setWidth(H264_DEF_WIDTH);
            mediaFormatVideo.setHeight(H264_DEF_HEIGHT);
            vidCodecParam.setEncFmt(mediaFormatVideo);

            for (int i = 0; i < codecFmtpVector.size(); i++) {
                if (PROFILE_LEVEL_ID_HEADER.equals(codecFmtpVector.get(i).getName())) {
                    codecFmtpVector.get(i).setVal(PROFILE_LEVEL_ID_JANUS_BRIDGE);
                    break;
                }
            }
            vidCodecParam.setDecFmtp(codecFmtpVector);
            mEndpoint.setVideoCodecParam(H264_CODEC_ID, vidCodecParam);

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
     */
    private void stopStack() {

        if (!mStarted) return;

        try {
            Logger.debug(TAG, "Stopping PJSIP");

            /*
             * Do not remove accounts on service stop anymore
             * They should have already been removed (unregistered)
             * In case they have not, it is ok, it means app has been just killed
             * or service force stopped
             *
             * *************************************
             * removeAllActiveAccounts();
             * *************************************
             */

            /* Try to force GC to do its job before destroying the library
             * since it's recommended to do that by PJSUA examples
             */
            Runtime.getRuntime().gc();

            mEndpoint.libDestroy(pjsua_destroy_flag.PJSUA_DESTROY_NO_NETWORK);
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
            CodecInfoVector2 codecs = mEndpoint.codecEnum2();
            if (codecs == null || codecs.size() == 0) return null;

            ArrayList<CodecPriority> codecPrioritiesList = new ArrayList<>(codecs.size());

            for (int i = 0; i < codecs.size(); i++) {
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
            mBroadcastEmitter.registrationState(accountID, account.getInfo().getRegStatus());
        } catch (Exception exc) {
            Logger.error(TAG, "Error while getting registration status for " + accountID, exc);
        }
    }

    @SuppressWarnings("unused")
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
            if (mActiveSipAccounts.containsKey(accountString) && sipAccount != null) {
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
    private void removeAccount(String accountID) {
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
        mSharedPreferencesHelper.persistConfiguredAccounts(mConfiguredAccounts);
    }

    private void persistConfiguredCodecPriorities(ArrayList<CodecPriority> codecPriorities) {
        mSharedPreferencesHelper.persistConfiguredCodecPriorities(codecPriorities);
    }

    private void loadConfiguredAccounts() {
        mConfiguredAccounts = mSharedPreferencesHelper.retrieveConfiguredAccounts();
    }

    private ArrayList<CodecPriority> getConfiguredCodecPriorities() {
        return mSharedPreferencesHelper.retrieveConfiguredCodecPriorities();
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

    public void setLastCallStatus(int callStatus) {
        this.callStatus = callStatus;
    }

    private void handleSetDND(Intent intent) {
        boolean dnd = intent.getBooleanExtra(PARAM_DND, false);
        mSharedPreferencesHelper.setDND(dnd);
    }

    public boolean isDND() {
        return mSharedPreferencesHelper.isDND();
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
            Surface surface = bundle.getParcelable(PARAM_SURFACE);
            sipCall.setIncomingVideoFeed(surface);
        }
    }
    private void handleSetSelfVideoOrientation(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);
        int callID = intent.getIntExtra(PARAM_CALL_ID, 0);
        int orientation = intent.getIntExtra(PARAM_ORIENTATION, -1);

        SipAccount sipAccount = mActiveSipAccounts.get(accountID);
        if (sipAccount != null) {
            SipCall sipCall = getCall(accountID, callID);
            if (sipCall == null) {
                notifyCallDisconnected(accountID, callID);
                return;
            }
            setSelfVideoOrientation(sipCall, orientation);
        }
    }

    void setSelfVideoOrientation(SipCall sipCall, int orientation) {
        try {
            int pjmediaOrientation;

            switch (orientation) {
                case Surface.ROTATION_0:   // Portrait
                    pjmediaOrientation = pjmedia_orient.PJMEDIA_ORIENT_ROTATE_270DEG;
                    break;
                case Surface.ROTATION_90:  // Landscape, home button on the right
                    pjmediaOrientation = pjmedia_orient.PJMEDIA_ORIENT_NATURAL;
                    break;
                case Surface.ROTATION_180:
                    pjmediaOrientation = pjmedia_orient.PJMEDIA_ORIENT_ROTATE_90DEG;
                    break;
                case Surface.ROTATION_270: // Landscape, home button on the left
                    pjmediaOrientation = pjmedia_orient.PJMEDIA_ORIENT_ROTATE_180DEG;
                    break;
                default:
                    pjmediaOrientation = pjmedia_orient.PJMEDIA_ORIENT_UNKNOWN;
            }

            if (pjmediaOrientation != pjmedia_orient.PJMEDIA_ORIENT_UNKNOWN)
            // set orientation to the correct current device
            getVidDevManager().setCaptureOrient(
                    sipCall.isFrontCamera()
                            ? FRONT_CAMERA_CAPTURE_DEVICE
                            : BACK_CAMERA_CAPTURE_DEVICE,
                    pjmediaOrientation, true);

        } catch (Exception iex) {
            Logger.error(TAG, "Error while changing video orientation");
        }
    }

    private void handleSetVideoMute(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);
        int callID = intent.getIntExtra(PARAM_CALL_ID, 0);
        SipCall sipCall = getCall(accountID, callID);

        if (sipCall == null) {
            notifyCallDisconnected(accountID, callID);
            return;
        }
        boolean mute = intent.getBooleanExtra(PARAM_VIDEO_MUTE, false);
        sipCall.setVideoMute(mute);
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

    // Switch Camera
    private void handleSwitchVideoCaptureDevice(Intent intent) {
        String accountID = intent.getStringExtra(PARAM_ACCOUNT_ID);
        int callID = intent.getIntExtra(PARAM_CALL_ID, 0);

        final SipCall sipCall = getCall(accountID, callID);
        if (sipCall == null) {
            notifyCallDisconnected(accountID, callID);
            return;
        }

        try {
            CallVidSetStreamParam callVidSetStreamParam = new CallVidSetStreamParam();
            callVidSetStreamParam.setCapDev(sipCall.isFrontCamera()
                    ? BACK_CAMERA_CAPTURE_DEVICE
                    : FRONT_CAMERA_CAPTURE_DEVICE);
            sipCall.setFrontCamera(!sipCall.isFrontCamera());
            sipCall.vidSetStream(pjsua_call_vid_strm_op.PJSUA_CALL_VID_STRM_CHANGE_CAP_DEV, callVidSetStreamParam);
        } catch (Exception ex) {
            Logger.error(TAG, "Error while switching capture device", ex);
        }
    }

    private void handleMakeDirectCall(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) return;
        Uri uri = bundle.getParcelable(PARAM_DIRECT_CALL_URI);
        if (uri == null) return;
        String sipServer = intent.getStringExtra(PARAM_DIRECT_CALL_SIP_SERVER);
        String name = intent.getStringExtra(PARAM_GUEST_NAME);
        boolean isVideo = intent.getBooleanExtra(PARAM_IS_VIDEO, false);
        boolean isVideoConference = false;
        if (isVideo) {
            isVideoConference = intent.getBooleanExtra(PARAM_IS_VIDEO_CONF, false);
        }

        Logger.debug(TAG, "Making call to " + uri.getUserInfo());
        String accountID = "sip:"+name+"@"+uri.getHost();
        String sipUri = "sip:" + uri.getUserInfo()+"@"+uri.getHost();

        try {
            startStack();
            SipAccountData sipAccountData = new SipAccountData()
                    .setHost(sipServer != null ? sipServer : uri.getHost())
                    .setUsername(name)
                    .setPort((uri.getPort() > 0) ? uri.getPort() : 5060)
                    .setRealm(uri.getHost());
                    /* display name not yet implemented server side for direct calls */
                    /* .setUsername("guest") */
                    /* .setGuestDisplayName(name)*/
            SipAccount pjSipAndroidAccount = new SipAccount(this, sipAccountData);
            pjSipAndroidAccount.createGuest();
            mConfiguredGuestAccount = pjSipAndroidAccount.getData();

            // Overwrite the old value if present
            mActiveSipAccounts.put(accountID, pjSipAndroidAccount);

            SipCall call = mActiveSipAccounts.get(accountID).addOutgoingCall(sipUri, isVideo, isVideoConference);
            if (call != null) {
                call.setVideoParams(isVideo, isVideoConference);
                mBroadcastEmitter.outgoingCall(accountID, call.getId(), uri.getUserInfo(), isVideo, isVideoConference);
            } else {
                Logger.error(TAG, "Error while making a direct call as Guest");
                mBroadcastEmitter.outgoingCall(accountID, -1, uri.getUserInfo(), false, false);
            }
        } catch (Exception ex) {
            Logger.error(TAG, "Error while making a direct call as Guest", ex);
            mBroadcastEmitter.outgoingCall(accountID, -1, uri.getUserInfo(), false, false);
        }
    }

    private void handleReconnectCall() {
        try {
            getBroadcastEmitter().callReconnectionState(CallReconnectionState.PROGRESS);
            mEndpoint.handleIpChange(new IpChangeParam());
            Logger.info(TAG, "Call reconnection started");
        } catch (Exception exc) {
            Logger.error(TAG, "Error while reconnecting the call", exc);
        }
    }

    public static ConcurrentHashMap<String, SipAccount> getActiveSipAccounts() {
        return mActiveSipAccounts;
    }
}
