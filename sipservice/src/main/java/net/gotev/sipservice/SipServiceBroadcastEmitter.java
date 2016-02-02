package net.gotev.sipservice;

import android.content.Context;
import android.content.Intent;

/**
 * Emits the broadcast intents.
 * @author gotev (Aleksandar Gotev)
 */
public class SipServiceBroadcastEmitter {

    public static String NAMESPACE = "net.gotev";

    private Context mContext;

    /**
     * Enumeration of the broadcast actions
     */
    public enum BroadcastAction {
        REGISTRATION,
        INCOMING_CALL,
        CALL_STATE
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
    }

    public SipServiceBroadcastEmitter(Context context) {
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
    public void incomingCall(String accountID, int callID, String displayName, String remoteUri) {
        final Intent intent = new Intent();

        intent.setAction(getAction(BroadcastAction.INCOMING_CALL));
        intent.putExtra(BroadcastParameters.ACCOUNT_ID, accountID);
        intent.putExtra(BroadcastParameters.CALL_ID, callID);
        intent.putExtra(BroadcastParameters.DISPLAY_NAME, displayName);
        intent.putExtra(BroadcastParameters.REMOTE_URI, remoteUri);

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
     */
    public void callState(String accountID, int callID, int callStateCode) {
        final Intent intent = new Intent();

        intent.setAction(getAction(BroadcastAction.CALL_STATE));
        intent.putExtra(BroadcastParameters.ACCOUNT_ID, accountID);
        intent.putExtra(BroadcastParameters.CALL_ID, callID);
        intent.putExtra(BroadcastParameters.CALL_STATE, callStateCode);

        mContext.sendBroadcast(intent);
    }
}
