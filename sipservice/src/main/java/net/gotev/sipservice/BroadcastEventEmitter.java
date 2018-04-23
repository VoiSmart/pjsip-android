package net.gotev.sipservice;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

/**
 * Emits the sip service broadcast intents.
 * @author gotev (Aleksandar Gotev)
 */
public class BroadcastEventEmitter {

    public static String NAMESPACE = "net.gotev";

    private Context mContext;

    /**
     * Enumeration of the broadcast actions
     */
    public enum BroadcastAction {
        REGISTRATION,
        INCOMING_CALL,
        CALL_STATE,
        OUTGOING_CALL,
        STACK_STATUS,
        CODEC_PRIORITIES,
        CODEC_PRIORITIES_SET_STATUS,
        MISSED_CALL
    }

    /**
     * Parameters passed in the broadcast intents.
     */
    public class BroadcastParameters {
        public static final String ACCOUNT_ID = "account_id";
        public static final String CALL_ID = "call_id";
        public static final String CODE = "code";
        public static final String REMOTE_URI = "remote_uri";
        public static final String DISPLAY_NAME = "display_name";
        public static final String CALL_STATE = "call_state";
        public static final String CALL_STATUS = "call_status";
        public static final String NUMBER = "number";
        public static final String CONNECT_TIMESTAMP = "connectTimestamp";
        public static final String STACK_STARTED = "stack_started";
        public static final String CODEC_PRIORITIES_LIST = "codec_priorities_list";
        public static final String LOCAL_HOLD = "local_hold";
        public static final String LOCAL_MUTE = "local_mute";
        public static final String LOCAL_VIDEO_MUTE = "local_video_mute";
        public static final String SUCCESS = "success";
        public static final String IS_VIDEO = "is_video";
    }

    public BroadcastEventEmitter(Context context) {
        mContext = context;
    }

    public static String getAction(BroadcastAction action) {
        return NAMESPACE + "." + action;
    }

    /**
     * Emit an incoming call broadcast intent.
     * @param accountID call's account IdUri
     * @param callID call ID number
     * @param displayName the display name of the remote party
     * @param remoteUri the IdUri of the remote party
     */
    public void incomingCall(String accountID, int callID, String displayName, String remoteUri, boolean isVideo) {
        final Intent intent = new Intent();

        intent.setAction(getAction(BroadcastAction.INCOMING_CALL));
        intent.putExtra(BroadcastParameters.ACCOUNT_ID, accountID);
        intent.putExtra(BroadcastParameters.CALL_ID, callID);
        intent.putExtra(BroadcastParameters.DISPLAY_NAME, displayName);
        intent.putExtra(BroadcastParameters.REMOTE_URI, remoteUri);
        intent.putExtra(BroadcastParameters.IS_VIDEO, isVideo);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);

        mContext.sendBroadcast(intent);
    }

    /**
     * Emit a registration state broadcast intent.
     * @param accountID account IdUri
     * @param registrationStateCode SIP registration status code
     */
    public void registrationState(String accountID, int registrationStateCode) {
        final Intent intent = new Intent();

        intent.setAction(getAction(BroadcastAction.REGISTRATION));
        intent.putExtra(BroadcastParameters.ACCOUNT_ID, accountID);
        intent.putExtra(BroadcastParameters.CODE, registrationStateCode);

        mContext.sendBroadcast(intent);
    }

    /**
     * Emit a call state broadcast intent.
     * @param accountID call's account IdUri
     * @param callID call ID number
     * @param callStateCode SIP call state code
     * @param connectTimestamp call start timestamp
     * @param isLocalHold true if the call is held locally
     * @param isLocalMute true if the call is muted locally
     * @param isLocalVideoMute true if the video is muted locally
     */
    public synchronized  void callState(String accountID, int callID, int callStateCode, int callStateStatus,
                          long connectTimestamp, boolean isLocalHold, boolean isLocalMute, boolean isLocalVideoMute) {
        final Intent intent = new Intent();

        intent.setAction(getAction(BroadcastAction.CALL_STATE));
        intent.putExtra(BroadcastParameters.ACCOUNT_ID, accountID);
        intent.putExtra(BroadcastParameters.CALL_ID, callID);
        intent.putExtra(BroadcastParameters.CALL_STATE, callStateCode);
        intent.putExtra(BroadcastParameters.CALL_STATUS, callStateStatus);
        intent.putExtra(BroadcastParameters.CONNECT_TIMESTAMP, connectTimestamp);
        intent.putExtra(BroadcastParameters.LOCAL_HOLD, isLocalHold);
        intent.putExtra(BroadcastParameters.LOCAL_MUTE, isLocalMute);
        intent.putExtra(BroadcastParameters.LOCAL_VIDEO_MUTE, isLocalVideoMute);

        mContext.sendBroadcast(intent);
    }

    public void outgoingCall(String accountID, int callID, String number, boolean isVideo) {
        final Intent intent = new Intent();

        intent.setAction(getAction(BroadcastAction.OUTGOING_CALL));
        intent.putExtra(BroadcastParameters.ACCOUNT_ID, accountID);
        intent.putExtra(BroadcastParameters.CALL_ID, callID);
        intent.putExtra(BroadcastParameters.NUMBER, number);
        intent.putExtra(BroadcastParameters.IS_VIDEO, isVideo);

        mContext.sendBroadcast(intent);
    }

    public void stackStatus(boolean started) {
        final Intent intent = new Intent();

        intent.setAction(getAction(BroadcastAction.STACK_STATUS));
        intent.putExtra(BroadcastParameters.STACK_STARTED, started);

        mContext.sendBroadcast(intent);
    }

    public void codecPriorities(ArrayList<CodecPriority> codecPriorities) {
        final Intent intent = new Intent();

        intent.setAction(getAction(BroadcastAction.CODEC_PRIORITIES));
        intent.putParcelableArrayListExtra(BroadcastParameters.CODEC_PRIORITIES_LIST, codecPriorities);

        mContext.sendBroadcast(intent);
    }

    public void codecPrioritiesSetStatus(boolean success) {
        final Intent intent = new Intent();

        intent.setAction(getAction(BroadcastAction.CODEC_PRIORITIES_SET_STATUS));
        intent.putExtra(BroadcastParameters.SUCCESS, success);

        mContext.sendBroadcast(intent);
    }

    void missedCall(String displayName, String uri) {
        final Intent intent = new Intent();

        intent.setAction(getAction(BroadcastAction.MISSED_CALL));
        intent.putExtra(BroadcastParameters.DISPLAY_NAME, displayName);
        intent.putExtra(BroadcastParameters.REMOTE_URI, uri);

        mContext.sendBroadcast(intent);
    }
}
