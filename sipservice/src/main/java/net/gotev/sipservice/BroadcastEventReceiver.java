package net.gotev.sipservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.ArrayList;

/**
 * Reference implementation to receive events emitted by the sip service.
 * @author gotev (Aleksandar Gotev)
 */
public class BroadcastEventReceiver extends BroadcastReceiver implements SipServiceConstants{

    private static final String LOG_TAG = "SipServiceBR";

    private Context receiverContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        //save context internally for convenience in subclasses, which can get it with
        //getReceiverContext method
        receiverContext = context;

        String action = intent.getAction();

        if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.REGISTRATION).equals(action)) {
            int stateCode = intent.getIntExtra(PARAM_REGISTRATION_CODE, -1);
            onRegistration(intent.getStringExtra(PARAM_ACCOUNT_ID), stateCode);

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.INCOMING_CALL).equals(action)) {
            onIncomingCall(intent.getStringExtra(PARAM_ACCOUNT_ID),
                    intent.getIntExtra(PARAM_CALL_ID, -1),
                    intent.getStringExtra(PARAM_DISPLAY_NAME),
                    intent.getStringExtra(PARAM_REMOTE_URI),
                    intent.getBooleanExtra(PARAM_IS_VIDEO, false));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.CALL_STATE).equals(action)) {
            int callState = intent.getIntExtra(PARAM_CALL_STATE, -1);
            int callStatus = intent.getIntExtra(PARAM_CALL_STATUS, -1);
            onCallState(intent.getStringExtra(PARAM_ACCOUNT_ID),
                        intent.getIntExtra(PARAM_CALL_ID, -1),
                        callState, callStatus,
                        intent.getLongExtra(PARAM_CONNECT_TIMESTAMP, -1),
                        intent.getBooleanExtra(PARAM_LOCAL_HOLD, false),
                        intent.getBooleanExtra(PARAM_LOCAL_MUTE, false),
                        intent.getBooleanExtra(PARAM_LOCAL_VIDEO_MUTE, false));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.OUTGOING_CALL).equals(action)) {
            onOutgoingCall(intent.getStringExtra(PARAM_ACCOUNT_ID),
                    intent.getIntExtra(PARAM_CALL_ID, -1),
                    intent.getStringExtra(PARAM_NUMBER),
                    intent.getBooleanExtra(PARAM_IS_VIDEO, false),
                    intent.getBooleanExtra(PARAM_IS_VIDEO_CONF, false));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.STACK_STATUS).equals(action)) {
            onStackStatus(intent.getBooleanExtra(PARAM_STACK_STARTED, false));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.CODEC_PRIORITIES).equals(action)) {
            ArrayList<CodecPriority> codecList = intent.getParcelableArrayListExtra(PARAM_CODEC_PRIORITIES_LIST);
            onReceivedCodecPriorities(codecList);

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.CODEC_PRIORITIES_SET_STATUS).equals(action)) {
            onCodecPrioritiesSetStatus(intent.getBooleanExtra(PARAM_SUCCESS, false));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.MISSED_CALL).equals(action)) {
            onMissedCall(intent.getStringExtra(PARAM_DISPLAY_NAME),
                    intent.getStringExtra(PARAM_REMOTE_URI));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.VIDEO_SIZE).equals(action)) {
            onVideoSize(intent.getIntExtra(PARAM_INCOMING_VIDEO_WIDTH, H264_DEF_WIDTH),
                    intent.getIntExtra(PARAM_INCOMING_VIDEO_HEIGHT, H264_DEF_HEIGHT));
        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.CALL_STATS).equals(action)) {
            int callStatus = intent.getIntExtra(PARAM_CALL_STATUS, -1);
            onCallStats(intent.getIntExtra(PARAM_CALL_STATS_DURATION, 0),
                intent.getStringExtra(PARAM_CALL_STATS_AUDIO_CODEC), callStatus,
                (RtpStreamStats) intent.getParcelableExtra(PARAM_CALL_STATS_RX_STREAM),
                (RtpStreamStats) intent.getParcelableExtra(PARAM_CALL_STATS_TX_STREAM));
        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.CALL_RECONNECTION_STATE).equals(action)) {
            onCallReconnectionState((CallReconnectionState) intent.getSerializableExtra(PARAM_CALL_RECONNECTION_STATE));
        }
    }

    protected Context getReceiverContext() {
        return receiverContext;
    }

    /**
     * Register this broadcast receiver.
     * It's recommended to register the receiver in Activity's onResume method.
     *
     * @param context context in which to register this receiver
     */
    public void register(final Context context) {

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BroadcastEventEmitter.getAction(
                BroadcastEventEmitter.BroadcastAction.REGISTRATION));
        intentFilter.addAction(BroadcastEventEmitter.getAction(
                BroadcastEventEmitter.BroadcastAction.INCOMING_CALL));
        intentFilter.addAction(BroadcastEventEmitter.getAction(
                BroadcastEventEmitter.BroadcastAction.CALL_STATE));
        intentFilter.addAction(BroadcastEventEmitter.getAction(
                BroadcastEventEmitter.BroadcastAction.OUTGOING_CALL));
        intentFilter.addAction(BroadcastEventEmitter.getAction(
                BroadcastEventEmitter.BroadcastAction.STACK_STATUS));
        intentFilter.addAction(BroadcastEventEmitter.getAction(
                BroadcastEventEmitter.BroadcastAction.CODEC_PRIORITIES));
        intentFilter.addAction(BroadcastEventEmitter.getAction(
                BroadcastEventEmitter.BroadcastAction.CODEC_PRIORITIES_SET_STATUS));
        intentFilter.addAction(BroadcastEventEmitter.getAction(
                BroadcastEventEmitter.BroadcastAction.MISSED_CALL));
        intentFilter.addAction(BroadcastEventEmitter.getAction(
                BroadcastEventEmitter.BroadcastAction.VIDEO_SIZE));
        intentFilter.addAction(BroadcastEventEmitter.getAction(
                BroadcastEventEmitter.BroadcastAction.CALL_STATS));
        intentFilter.addAction(BroadcastEventEmitter.getAction(
                BroadcastEventEmitter.BroadcastAction.CALL_RECONNECTION_STATE));
        context.registerReceiver(this, intentFilter);
    }

    /**
     * Unregister this broadcast receiver.
     * It's recommended to unregister the receiver in Activity's onPause method.
     *
     * @param context context in which to unregister this receiver
     */
    public void unregister(final Context context) {
        context.unregisterReceiver(this);
    }

    public void onRegistration(String accountID, int registrationStateCode) {
        Logger.debug(LOG_TAG, "onRegistration - accountID: " + accountID +
                ", registrationStateCode: " + registrationStateCode);
    }

    public void onIncomingCall(String accountID, int callID, String displayName, String remoteUri, boolean isVideo) {
        Logger.debug(LOG_TAG, "onIncomingCall - accountID: " + accountID +
                ", callID: " + callID +
                ", displayName: " + displayName +
                ", remoteUri: " + remoteUri);
    }

    public void onCallState(String accountID, int callID, int callStateCode, int callStatusCode,
                            long connectTimestamp, boolean isLocalHold, boolean isLocalMute, boolean isLocalVideoMute) {
        Logger.debug(LOG_TAG, "onCallState - accountID: " + accountID +
                ", callID: " + callID +
                ", callStateCode: " + callStateCode +
                ", callStatusCode: " + callStatusCode +
                ", connectTimestamp: " + connectTimestamp +
                ", isLocalHold: " + isLocalHold +
                ", isLocalMute: " + isLocalMute +
                ", isLocalVideoMute: " + isLocalVideoMute);
    }

    public void onOutgoingCall(String accountID, int callID, String number, boolean isVideo, boolean isVideoConference) {
        Logger.debug(LOG_TAG, "onOutgoingCall - accountID: " + accountID +
                ", callID: " + callID +
                ", number: " + number);
    }

    public void onStackStatus(boolean started) {
        Logger.debug(LOG_TAG, "SIP service stack " + (started ? "started" : "stopped"));
    }

    public void onReceivedCodecPriorities(ArrayList<CodecPriority> codecPriorities) {
        Logger.debug(LOG_TAG, "Received codec priorities");
        for (CodecPriority codec : codecPriorities) {
            Logger.debug(LOG_TAG, codec.toString());
        }
    }

    public void onCodecPrioritiesSetStatus(boolean success) {
        Logger.debug(LOG_TAG, "Codec priorities " + (success ? "successfully set" : "set error"));
    }

    public void onMissedCall(String displayName, String uri) {
        Logger.debug(LOG_TAG, "Missed call from " + displayName);
    }

    protected void onVideoSize(int width, int height) {
        Logger.debug(LOG_TAG, "Video resolution " + width+"x"+height);
    }

    protected void onCallStats(int duration, String audioCodec, int callStatusCode, RtpStreamStats rx, RtpStreamStats tx) {
        Logger.debug(LOG_TAG, "Call Stats sent "+duration+" "+audioCodec);
    }

    protected void onCallReconnectionState(CallReconnectionState state) {
        Logger.debug(LOG_TAG, "Call reconnection state " + state.name());
    }
}
