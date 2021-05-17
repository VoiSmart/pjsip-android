package net.gotev.sipservice;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Emits the sip service broadcast intents.
 * @author gotev (Aleksandar Gotev)
 */
public class BroadcastEventEmitter implements SipServiceConstants {

    public static String NAMESPACE = "com.voismart";

    private final Context mContext;

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
        MISSED_CALL,
        VIDEO_SIZE,
        CALL_STATS,
        CALL_RECONNECTION_STATE
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
     * @param isVideo whether the call has video or not
     */
    public void incomingCall(String accountID, int callID, String displayName, String remoteUri, boolean isVideo) {
        final Intent intent = new Intent();

        intent.setAction(getAction(BroadcastAction.INCOMING_CALL));
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_CALL_ID, callID);
        intent.putExtra(PARAM_DISPLAY_NAME, displayName);
        intent.putExtra(PARAM_REMOTE_URI, remoteUri);
        intent.putExtra(PARAM_IS_VIDEO, isVideo);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);

        sendExplicitBroadcast(intent);
    }

    /**
     * Emit a registration state broadcast intent.
     * @param accountID account IdUri
     * @param registrationStateCode SIP registration status code
     */
    public void registrationState(String accountID, int registrationStateCode) {
        final Intent intent = new Intent();

        intent.setAction(getAction(BroadcastAction.REGISTRATION));
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_REGISTRATION_CODE, registrationStateCode);

        mContext.sendBroadcast(intent);
    }

    /**
     * Emit a call state broadcast intent.
     * @param accountID call's account IdUri
     * @param callID call ID number
     * @param callStateCode SIP call state code
     * @param callStateStatus SIP call state status
     * @param connectTimestamp call start timestamp
     * @param isLocalHold true if the call is held locally
     * @param isLocalMute true if the call is muted locally
     * @param isLocalVideoMute true if the video is muted locally
     */
    public synchronized void callState(String accountID, int callID, int callStateCode, int callStateStatus,
                          long connectTimestamp, boolean isLocalHold, boolean isLocalMute, boolean isLocalVideoMute) {
        final Intent intent = new Intent();

        intent.setAction(getAction(BroadcastAction.CALL_STATE));
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_CALL_ID, callID);
        intent.putExtra(PARAM_CALL_STATE, callStateCode);
        intent.putExtra(PARAM_CALL_STATUS, callStateStatus);
        intent.putExtra(PARAM_CONNECT_TIMESTAMP, connectTimestamp);
        intent.putExtra(PARAM_LOCAL_HOLD, isLocalHold);
        intent.putExtra(PARAM_LOCAL_MUTE, isLocalMute);
        intent.putExtra(PARAM_LOCAL_VIDEO_MUTE, isLocalVideoMute);

        mContext.sendBroadcast(intent);
    }

    public void outgoingCall(String accountID, int callID, String number, boolean isVideo, boolean isVideoConference) {
        final Intent intent = new Intent();

        intent.setAction(getAction(BroadcastAction.OUTGOING_CALL));
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_CALL_ID, callID);
        intent.putExtra(PARAM_NUMBER, number);
        intent.putExtra(PARAM_IS_VIDEO, isVideo);
        intent.putExtra(PARAM_IS_VIDEO_CONF, isVideoConference);

        sendExplicitBroadcast(intent);
    }

    public void stackStatus(boolean started) {
        final Intent intent = new Intent();

        intent.setAction(getAction(BroadcastAction.STACK_STATUS));
        intent.putExtra(PARAM_STACK_STARTED, started);

        mContext.sendBroadcast(intent);
    }

    public void codecPriorities(ArrayList<CodecPriority> codecPriorities) {
        final Intent intent = new Intent();

        intent.setAction(getAction(BroadcastAction.CODEC_PRIORITIES));
        intent.putParcelableArrayListExtra(PARAM_CODEC_PRIORITIES_LIST, codecPriorities);

        mContext.sendBroadcast(intent);
    }

    public void codecPrioritiesSetStatus(boolean success) {
        final Intent intent = new Intent();

        intent.setAction(getAction(BroadcastAction.CODEC_PRIORITIES_SET_STATUS));
        intent.putExtra(PARAM_SUCCESS, success);

        mContext.sendBroadcast(intent);
    }

    void missedCall(String displayName, String uri) {
        final Intent intent = new Intent();

        intent.setAction(getAction(BroadcastAction.MISSED_CALL));
        intent.putExtra(PARAM_DISPLAY_NAME, displayName);
        intent.putExtra(PARAM_REMOTE_URI, uri);

        sendExplicitBroadcast(intent);
    }

    void videoSize(int width, int height) {
        final Intent intent = new Intent();

        intent.setAction(getAction(BroadcastAction.VIDEO_SIZE));
        intent.putExtra(PARAM_INCOMING_VIDEO_WIDTH, width);
        intent.putExtra(PARAM_INCOMING_VIDEO_HEIGHT, height);

        mContext.sendBroadcast(intent);
    }

    void callStats(int duration, String audioCodec, int callStateStatus, RtpStreamStats rx, RtpStreamStats tx) {
        final Intent intent = new Intent();

        intent.setAction(getAction(BroadcastAction.CALL_STATS));
        intent.putExtra(PARAM_CALL_STATS_DURATION, duration);
        intent.putExtra(PARAM_CALL_STATS_AUDIO_CODEC, audioCodec);
        intent.putExtra(PARAM_CALL_STATS_CALL_STATUS, callStateStatus);
        intent.putExtra(PARAM_CALL_STATS_RX_STREAM, rx);
        intent.putExtra(PARAM_CALL_STATS_TX_STREAM, tx);

        mContext.sendBroadcast(intent);
    }

    void callReconnectionState(CallReconnectionState state) {
        final Intent intent = new Intent();
        intent.setAction(getAction(BroadcastAction.CALL_RECONNECTION_STATE));
        intent.putExtra(PARAM_CALL_RECONNECTION_STATE, state);
        mContext.sendBroadcast(intent);
    }

    private boolean sendExplicitBroadcast(Intent intent) {
        PackageManager pm=mContext.getPackageManager();
        List<ResolveInfo> matches=pm.queryBroadcastReceivers(intent, 0);
        boolean sent = false;

        for (ResolveInfo resolveInfo : matches) {
            ComponentName cn=
                    new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName,
                            resolveInfo.activityInfo.name);

            intent.setComponent(cn);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            mContext.sendBroadcast(intent);
            sent = true;
        }
        return sent;
    }
}
