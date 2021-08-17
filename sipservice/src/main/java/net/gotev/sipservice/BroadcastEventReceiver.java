package net.gotev.sipservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.ArrayList;

import static net.gotev.sipservice.ObfuscationHelper.getValue;

import net.gotev.sipservice.broadcast_events.CallStateEvent;
import net.gotev.sipservice.broadcast_events.CallStatsEvent;
import net.gotev.sipservice.broadcast_events.CodecPrioritiesEvent;
import net.gotev.sipservice.broadcast_events.CodecPrioritiesSetStatusEvent;
import net.gotev.sipservice.broadcast_events.IncomingCallEvent;
import net.gotev.sipservice.broadcast_events.CallMediaStateEvent;
import net.gotev.sipservice.broadcast_events.MissedCallEvent;
import net.gotev.sipservice.broadcast_events.OutgoingCallEvent;
import net.gotev.sipservice.broadcast_events.RegistrationEvent;
import net.gotev.sipservice.broadcast_events.StackStatusEvent;
import net.gotev.sipservice.broadcast_events.VideoSizeEvent;

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
            onRegistration(new RegistrationEvent(intent.getStringExtra(PARAM_ACCOUNT_ID), stateCode));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.INCOMING_CALL).equals(action)) {
            onIncomingCall(new IncomingCallEvent(
                    intent.getStringExtra(PARAM_ACCOUNT_ID),
                    intent.getIntExtra(PARAM_CALL_ID, -1),
                    intent.getStringExtra(PARAM_DISPLAY_NAME),
                    intent.getStringExtra(PARAM_REMOTE_URI),
                    intent.getBooleanExtra(PARAM_IS_VIDEO, false)
            ));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.CALL_STATE).equals(action)) {
            int callState = intent.getIntExtra(PARAM_CALL_STATE, -1);
            int callStatus = intent.getIntExtra(PARAM_CALL_STATUS, -1);
            onCallState(new CallStateEvent(
                    intent.getStringExtra(PARAM_ACCOUNT_ID),
                    intent.getIntExtra(PARAM_CALL_ID, -1),
                    callState, callStatus,
                    intent.getLongExtra(PARAM_CONNECT_TIMESTAMP, -1)
            ));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.CALL_MEDIA_STATE).equals(action)) {
            onCallMediaState(new CallMediaStateEvent(
                    intent.getStringExtra(PARAM_ACCOUNT_ID),
                    intent.getIntExtra(PARAM_CALL_ID, -1),
                    (MediaState) intent.getSerializableExtra(PARAM_MEDIA_STATE_KEY),
                    intent.getBooleanExtra(PARAM_MEDIA_STATE_VALUE, false)
            ));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.OUTGOING_CALL).equals(action)) {
            onOutgoingCall(new OutgoingCallEvent(
                    intent.getStringExtra(PARAM_ACCOUNT_ID),
                    intent.getIntExtra(PARAM_CALL_ID, -1),
                    intent.getStringExtra(PARAM_NUMBER),
                    intent.getBooleanExtra(PARAM_IS_VIDEO, false),
                    intent.getBooleanExtra(PARAM_IS_VIDEO_CONF, false),
                    intent.getBooleanExtra(PARAM_IS_TRANSFER, false)
            ));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.STACK_STATUS).equals(action)) {
            onStackStatus(new StackStatusEvent(
                    intent.getBooleanExtra(PARAM_STACK_STARTED, false)
            ));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.CODEC_PRIORITIES).equals(action)) {
            ArrayList<CodecPriority> codecList = intent.getParcelableArrayListExtra(PARAM_CODEC_PRIORITIES_LIST);
            onReceivedCodecPriorities(new CodecPrioritiesEvent(codecList));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.CODEC_PRIORITIES_SET_STATUS).equals(action)) {
            onCodecPrioritiesSetStatus(new CodecPrioritiesSetStatusEvent(intent.getBooleanExtra(PARAM_SUCCESS, false)));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.MISSED_CALL).equals(action)) {
            onMissedCall(new MissedCallEvent(
                    intent.getStringExtra(PARAM_DISPLAY_NAME),
                    intent.getStringExtra(PARAM_REMOTE_URI)
            ));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.VIDEO_SIZE).equals(action)) {
            onVideoSize(new VideoSizeEvent(
                    intent.getIntExtra(PARAM_INCOMING_VIDEO_WIDTH, H264_DEF_WIDTH),
                    intent.getIntExtra(PARAM_INCOMING_VIDEO_HEIGHT, H264_DEF_HEIGHT)

            ));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.CALL_STATS).equals(action)) {
            int callStatus = intent.getIntExtra(PARAM_CALL_STATUS, -1);
            onCallStats(new CallStatsEvent(
                    intent.getIntExtra(PARAM_CALL_ID, -1),
                    intent.getIntExtra(PARAM_CALL_STATS_DURATION, 0),
                    intent.getStringExtra(PARAM_CALL_STATS_AUDIO_CODEC), callStatus,
                    intent.getParcelableExtra(PARAM_CALL_STATS_RX_STREAM),
                    intent.getParcelableExtra(PARAM_CALL_STATS_TX_STREAM)
            ));

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
                BroadcastEventEmitter.BroadcastAction.CALL_MEDIA_STATE));
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

    public void onRegistration(RegistrationEvent registrationEvent) {
        Logger.debug(LOG_TAG, "onRegistration - registrationEvent: " + registrationEvent.toString());
    }

    public void onIncomingCall(IncomingCallEvent incomingCallEvent) {
        Logger.debug(LOG_TAG, "onIncomingCall - incomingCallEvent: " + incomingCallEvent.toString());
    }

    public void onCallState(CallStateEvent callStateEvent) {
        Logger.debug(LOG_TAG, "onCallState - callStateEvent: " + callStateEvent.toString());
    }

    public void onCallMediaState(CallMediaStateEvent callMediaStateEvent) {
        Logger.debug(LOG_TAG, "onCallState - callMediaStateEvent: " + callMediaStateEvent.toString());
    }

    public void onOutgoingCall(OutgoingCallEvent outgoingCallEvent) {
        Logger.debug(LOG_TAG, "onOutgoingCall - outgoingCallEvent: " + outgoingCallEvent.toString());
    }

    public void onStackStatus(StackStatusEvent stackStatusEvent) {
        Logger.debug(LOG_TAG, "onStackStatus - stackStatusEvent : " + (stackStatusEvent.isStarted() ? "started" : "stopped"));
    }

    public void onReceivedCodecPriorities(CodecPrioritiesEvent codecPrioritiesEvent) {
        Logger.debug(LOG_TAG, "onReceivedCodecPriorities - codecPrioritiesEvent : " + codecPrioritiesEvent.toString());
        for (CodecPriority codec : codecPrioritiesEvent.getCodecPriorities()) {
            Logger.debug(LOG_TAG, codec.toString());
        }
    }

    public void onCodecPrioritiesSetStatus(CodecPrioritiesSetStatusEvent codecPrioritiesSetStatusEvent) {
        Logger.debug(LOG_TAG, "onCodecPrioritiesSetStatus - codecPrioritiesSetStatusEvent : " + (codecPrioritiesSetStatusEvent.isSuccess() ? "successfully set" : "set error"));
    }

    public void onMissedCall(MissedCallEvent missedCallEvent) {
        Logger.debug(LOG_TAG, "onMissedCall - missedCallEvent " + getValue(getReceiverContext(), missedCallEvent.getDisplayName()));
    }

    protected void onVideoSize(VideoSizeEvent videoSizeEvent) {
        Logger.debug(LOG_TAG, "onVideoSize - videoSizeEvent " + videoSizeEvent.getWidth()+"x"+videoSizeEvent.getHeight());
    }

    protected void onCallStats(CallStatsEvent callStatsEvent) {
        Logger.debug(LOG_TAG, "onCallStats - callStatsEvent : " + callStatsEvent.toString());
    }

    protected void onCallReconnectionState(CallReconnectionState state) {
        Logger.debug(LOG_TAG, "onCallReconnectionState - state : " + state.name());
    }
}
