package net.gotev.sipservice;

import android.content.Context;
import android.content.Intent;

import org.pjsip.pjsua2.pjsip_inv_state;

import java.util.ArrayList;

/**
 * Triggers sip service commands.
 * @author gotev (Aleksandar Gotev)
 */
public class SipServiceCommand {

    public static String AGENT_NAME = "AndroidSipService/" + BuildConfig.VERSION_CODE;

    protected static final String ACTION_RESTART_SIP_STACK = "restartSipStack";
    protected static final String ACTION_SET_ACCOUNT = "setAccount";
    protected static final String ACTION_REMOVE_ACCOUNT = "removeAccount";
    protected static final String ACTION_MAKE_CALL = "makeCall";
    protected static final String ACTION_HANG_UP_CALL = "hangUpCall";
    protected static final String ACTION_GET_CALL_STATUS = "getCallStatus";
    protected static final String ACTION_SEND_DTMF = "sendDtmf";
    protected static final String ACTION_ACCEPT_INCOMING_CALL = "acceptIncomingCall";
    protected static final String ACTION_DECLINE_INCOMING_CALL = "declineIncomingCall";
    protected static final String ACTION_SET_HOLD = "callSetHold";
    protected static final String ACTION_SET_MUTE = "callSetMute";
    protected static final String ACTION_TOGGLE_HOLD = "callToggleHold";
    protected static final String ACTION_TOGGLE_MUTE = "callToggleMute";
    protected static final String ACTION_TRANSFER_CALL = "callTransfer";
    protected static final String ACTION_GET_CODEC_PRIORITIES = "codecPriorities";
    protected static final String ACTION_SET_CODEC_PRIORITIES = "setCodecPriorities";

    protected static final String PARAM_ACCOUNT_DATA = "accountData";
    protected static final String PARAM_ACCOUNT_ID = "accountID";
    protected static final String PARAM_NUMBER = "number";
    protected static final String PARAM_CALL_ID = "callId";
    protected static final String PARAM_DTMF = "dtmf";
    protected static final String PARAM_HOLD = "hold";
    protected static final String PARAM_MUTE = "mute";
    protected static final String PARAM_CODEC_PRIORITIES = "codecPriorities";

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
     * Makes a call.
     * @param context application context
     * @param accountID account ID used to make the call
     * @param numberToCall number to call
     */
    public static void makeCall(Context context, String accountID, String numberToCall) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_MAKE_CALL);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_NUMBER, numberToCall);
        context.startService(intent);
    }

    /**
     * Checks the status of a call. You will receive the result in
     * {@link BroadcastEventReceiver#onCallState(String, int, pjsip_inv_state, long)}
     * @param context application context
     * @param accountID account ID used to make the call
     * @param callID call ID
     */
    public static void getCallStatus(Context context, String accountID, int callID) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_GET_CALL_STATUS);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_CALL_ID, callID);
        context.startService(intent);
    }

    /**
     * Hangs up an active call. If the call does not exist or has been terminated, a disconnected
     * state will be sent to
     * {@link BroadcastEventReceiver#onCallState(String, int, pjsip_inv_state, long)}
     * @param context application context
     * @param accountID account ID
     * @param callID call ID to hang up
     */
    public static void hangUpCall(Context context, String accountID, int callID) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_HANG_UP_CALL);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_CALL_ID, callID);
        context.startService(intent);
    }

    /**
     * Send DTMF. If the call does not exist or has been terminated, a disconnected
     * state will be sent to
     * {@link BroadcastEventReceiver#onCallState(String, int, pjsip_inv_state, long)}
     * @param context application context
     * @param accountID account ID
     * @param callID call ID to hang up
     */
    public static void sendDTMF(Context context, String accountID, int callID, String dtmfTone) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_SEND_DTMF);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_CALL_ID, callID);
        intent.putExtra(PARAM_DTMF, dtmfTone);
        context.startService(intent);
    }

    /**
     * Accept an incoming call. If the call does not exist or has been terminated, a disconnected
     * state will be sent to
     * {@link BroadcastEventReceiver#onCallState(String, int, pjsip_inv_state, long)}
     * @param context application context
     * @param accountID account ID
     * @param callID call ID to hang up
     */
    public static void acceptIncomingCall(Context context, String accountID, int callID) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_ACCEPT_INCOMING_CALL);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_CALL_ID, callID);
        context.startService(intent);
    }

    /**
     * Decline an incoming call. If the call does not exist or has been terminated, a disconnected
     * state will be sent to
     * {@link BroadcastEventReceiver#onCallState(String, int, pjsip_inv_state, long)}
     * @param context application context
     * @param accountID account ID
     * @param callID call ID to hang up
     */
    public static void declineIncomingCall(Context context, String accountID, int callID) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_DECLINE_INCOMING_CALL);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_CALL_ID, callID);
        context.startService(intent);
    }

    /**
     * Blind call transfer. If the call does not exist or has been terminated, a disconnected
     * state will be sent to
     * {@link BroadcastEventReceiver#onCallState(String, int, pjsip_inv_state, long)}
     * @param context application context
     * @param accountID account ID
     * @param callID call ID
     * @param number number to which to trasnfer the call
     */
    public static void transferCall(Context context, String accountID, int callID, String number) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_TRANSFER_CALL);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_CALL_ID, callID);
        intent.putExtra(PARAM_NUMBER, number);
        context.startService(intent);
    }

    /**
     * Sets hold status for a call. If the call does not exist or has been terminated, a disconnected
     * state will be sent to
     * {@link BroadcastEventReceiver#onCallState(String, int, pjsip_inv_state, long)}
     * @param context application context
     * @param accountID account ID
     * @param callID call ID
     * @param hold true to hold the call, false to un-hold it
     */
    public static void setCallHold(Context context, String accountID, int callID, boolean hold) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_SET_HOLD);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_CALL_ID, callID);
        intent.putExtra(PARAM_HOLD, hold);
        context.startService(intent);
    }

    /**
     * Toggle hold status for a call. If the call does not exist or has been terminated, a disconnected
     * state will be sent to
     * {@link BroadcastEventReceiver#onCallState(String, int, pjsip_inv_state, long)}
     * @param context application context
     * @param accountID account ID
     * @param callID call ID
     */
    public static void toggleCallHold(Context context, String accountID, int callID) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_TOGGLE_HOLD);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_CALL_ID, callID);
        context.startService(intent);
    }

    /**
     * Sets mute status for a call. If the call does not exist or has been terminated, a disconnected
     * state will be sent to
     * {@link BroadcastEventReceiver#onCallState(String, int, pjsip_inv_state, long)}
     * @param context application context
     * @param accountID account ID
     * @param callID call ID
     * @param mute true to mute the call, false to un-mute it
     */
    public static void setCallMute(Context context, String accountID, int callID, boolean mute) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_SET_MUTE);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_CALL_ID, callID);
        intent.putExtra(PARAM_MUTE, mute);
        context.startService(intent);
    }

    /**
     * Toggle mute status for a call. If the call does not exist or has been terminated, a disconnected
     * state will be sent to
     * {@link BroadcastEventReceiver#onCallState(String, int, pjsip_inv_state, long)}
     * @param context application context
     * @param accountID account ID
     * @param callID call ID
     */
    public static void toggleCallMute(Context context, String accountID, int callID) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_TOGGLE_MUTE);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_CALL_ID, callID);
        context.startService(intent);
    }

    /**
     * Requests the codec priorities. This is going to return results only if the sip stack has
     * been started, otherwise you will see an error message in LogCat.
     * @param context application context
     */
    public static void getCodecPriorities(Context context) {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_GET_CODEC_PRIORITIES);
        context.startService(intent);
    }

    /**
     * Set codec priorities. this is going to work only if the sip stack has
     * been started, otherwise you will see an error message in LogCat.
     * @param context application context
     * @param codecPriorities list with the codec priorities to set
     */
    public static void setCodecPriorities(Context context, ArrayList<CodecPriority> codecPriorities) {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_SET_CODEC_PRIORITIES);
        intent.putParcelableArrayListExtra(PARAM_CODEC_PRIORITIES, codecPriorities);
        context.startService(intent);
    }

    private static void checkAccount(String accountID) {
        if (accountID == null || accountID.isEmpty() || !accountID.startsWith("sip:")) {
            throw new IllegalArgumentException("Invalid accountID! Example: sip:user@domain");
        }
    }
}
