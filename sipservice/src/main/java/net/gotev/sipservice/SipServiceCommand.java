package net.gotev.sipservice;

import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.view.Surface;

import org.pjsip.PjCameraInfo2;

import java.util.ArrayList;

/**
 * Triggers sip service commands.
 * @author gotev (Aleksandar Gotev)
 */
@SuppressWarnings("unused")
public class SipServiceCommand implements SipServiceConstants {

    /**
     * This should be changed on the app side
     * to reflect app version/name/... or whatever might be useful for debugging
     */
    public static String AGENT_NAME = "AndroidSipService";

    /**
     * Enables pjsip logging (valid only for debug builds)
     */
    public static void enableSipDebugLogging(boolean enable) {
        SipServiceUtils.ENABLE_SIP_LOGGING = enable;
    }

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
     * Adds a new SIP account and changes the sip stack codec priority settings.
     * This is handy to set an account plus the global codec priority configuration with
     * just a single call.
     * @param context application context
     * @param sipAccount sip account data
     * @param codecPriorities list with the codec priorities to set
     * @return sip account ID uri as a string
     */
    public static String setAccountWithCodecs(Context context, SipAccountData sipAccount,
                                              ArrayList<CodecPriority> codecPriorities) {
        if (sipAccount == null) {
            throw new IllegalArgumentException("sipAccount MUST not be null!");
        }

        String accountID = sipAccount.getIdUri();
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_SET_ACCOUNT);
        intent.putExtra(PARAM_ACCOUNT_DATA, sipAccount);
        intent.putParcelableArrayListExtra(PARAM_CODEC_PRIORITIES, codecPriorities);
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
     * @param isVideo whether the call has video or not
     * @param isVideoConference whether the call is video conference or not
     * @param isTransfer whether this (second) call will eventually be transferred to the current
     */
    public static void makeCall(
            Context context,
            String accountID,
            String numberToCall,
            boolean isVideo,
            boolean isVideoConference,
            boolean isTransfer
    ) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_MAKE_CALL);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_NUMBER, numberToCall);
        intent.putExtra(PARAM_IS_VIDEO, isVideo);
        intent.putExtra(PARAM_IS_VIDEO_CONF, isVideoConference);
        intent.putExtra(PARAM_IS_TRANSFER, isTransfer);
        context.startService(intent);
    }

    public static void makeCall(Context context, String accountID, String numberToCall, boolean isVideo, boolean isVideoConference) {
        makeCall(context, accountID, numberToCall, isVideo, isVideoConference, false);
    }

    public static void makeCall(Context context, String accountID, String numberToCall) {
        makeCall(context, accountID, numberToCall, false, false, false);
    }

    public static void makeCall(Context context, String accountID, String numberToCall, boolean isTransfer) {
        makeCall(context, accountID, numberToCall, false, false, isTransfer);
    }

    /**
     * Makes a silent call, i.e. the outgoing call event is sent through broadcast
     * {@link BroadcastEventEmitter#silentCallStatus(boolean, String)}
     * Instead of {@link BroadcastEventEmitter#outgoingCall(String, int, String, boolean, boolean, boolean)}
     * Useful when the calls only enables/disables features/services on the pbx via feature codes
     * E.g. enable dnd, join/leave queue, ...
     * @param context application context
     * @param accountID account ID used to make the call
     * @param numberToCall number to call
     */
    public static void makeSilentCall(Context context, String accountID, String numberToCall) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_MAKE_SILENT_CALL);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_NUMBER, numberToCall);
        context.startService(intent);
    }

    /**
     * Makes a Direct call.
     * @param context application context
     * @param guestName name to display when making guest calls
     * @param host sip host
     * @param sipUri sip uri to call in the format: sip:number@realm:port
     * @param isVideo whether the call has video or not
     * @param isVideoConference whether the call is video conference or not
     * @param transport transport to be configured on guest account
     */
    public static void makeDirectCall(
            Context context,
            String guestName,
            Uri sipUri,
            String host,
            boolean isVideo,
            boolean isVideoConference,
            SipAccountTransport transport
    ) {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_MAKE_DIRECT_CALL);
        intent.putExtra(PARAM_GUEST_NAME, guestName);
        intent.putExtra(PARAM_DIRECT_CALL_URI, sipUri);
        intent.putExtra(PARAM_DIRECT_CALL_SIP_SERVER, host);
        intent.putExtra(PARAM_IS_VIDEO, isVideo);
        intent.putExtra(PARAM_IS_VIDEO_CONF, isVideoConference);
        intent.putExtra(PARAM_DIRECT_CALL_TRANSPORT, transport.ordinal());
        context.startService(intent);
    }

    public static void makeDirectCall(Context context, String guestName, Uri sipUri, String host, boolean isVideo, boolean isVideoConference) {
        makeDirectCall(context, guestName, sipUri, host, isVideo, isVideoConference, SipAccountTransport.UDP);
    }

    /**
     * Checks the status of a call. You will receive the result in
     * {@link BroadcastEventReceiver#onCallState(String, int, int, int, long)}
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
     * {@link BroadcastEventReceiver#onCallState(String, int, int, int, long)}
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
     * Hangs up active calls.
     * @param context application context
     * @param accountID account ID
     */
    public static void hangUpActiveCalls(Context context, String accountID) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_HANG_UP_CALLS);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        context.startService(intent);
    }

    /**
     * Hangs up active calls.
     * @param context application context
     * @param accountID account ID
     */
    public static void holdActiveCalls(Context context, String accountID) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_HOLD_CALLS);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        context.startService(intent);
    }

    /**
     * Send DTMF. If the call does not exist or has been terminated, a disconnected
     * state will be sent to
     * {@link BroadcastEventReceiver#onCallState(String, int, int, int, long)}
     * @param context application context
     * @param accountID account ID
     * @param callID call ID to hang up
     * @param dtmfTone DTMF tone to send (e.g. number from 0 to 9 or # or *).
     *                 You can send only one DTMF at a time.
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
     * {@link BroadcastEventReceiver#onCallState(String, int, int, int, long)}
     * @param context application context
     * @param accountID account ID
     * @param callID call ID to hang up
     * @param isVideo video call or not
     */
    public static void acceptIncomingCall(Context context, String accountID, int callID, boolean isVideo) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_ACCEPT_INCOMING_CALL);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_CALL_ID, callID);
        intent.putExtra(PARAM_IS_VIDEO, isVideo);
        context.startService(intent);
    }

    public static void acceptIncomingCall(Context context, String accountID, int callID) {
        acceptIncomingCall(context, accountID, callID, false);
    }

    /**
     * Decline an incoming call. If the call does not exist or has been terminated, a disconnected
     * state will be sent to
     * {@link BroadcastEventReceiver#onCallState(String, int, int, int, long)}
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
     * {@link BroadcastEventReceiver#onCallState(String, int, int, int, long)}
     * @param context application context
     * @param accountID account ID
     * @param callID call ID
     * @param number number to which to transfer the call
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
     * Attended call transfer. If the call does not exist or has been terminated, a disconnected
     * state will be sent to
     * {@link BroadcastEventReceiver#onCallState(String, int, int, int, long)}
     * @param context application context
     * @param accountID account ID
     * @param callIdOrig call ID of the original call
     * @param callIdDest call ID of the destination call
     */
    public static void attendedTransferCall(Context context, String accountID, int callIdOrig, int callIdDest) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_ATTENDED_TRANSFER_CALL);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_CALL_ID, callIdOrig);
        intent.putExtra(PARAM_CALL_ID_DEST, callIdDest);
        context.startService(intent);
    }

    /**
     * Sets hold status for a call. If the call does not exist or has been terminated, a disconnected
     * state will be sent to
     * {@link BroadcastEventReceiver#onCallState(String, int, int, int, long)}
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
     * {@link BroadcastEventReceiver#onCallState(String, int, int, int, long)}
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
     * {@link BroadcastEventReceiver#onCallState(String, int, int, int, long)}
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
     * {@link BroadcastEventReceiver#onCallState(String, int, int, int, long)}
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
        if (accountID == null || !accountID.startsWith("sip:")) {
            throw new IllegalArgumentException("Invalid accountID! Example: sip:user@domain");
        }
    }

    /**
     * Gets the registration status for an account.
     * @param context application context
     * @param accountID sip account data
     */
    public static void getRegistrationStatus(Context context, String accountID) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_GET_REGISTRATION_STATUS);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        context.startService(intent);
    }

    public static void refreshRegistration(Context context, String accountID, int regExpTimeout, String regContactParams){
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_REFRESH_REGISTRATION);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_REG_EXP_TIMEOUT, regExpTimeout);
        intent.putExtra(PARAM_REG_CONTACT_PARAMS, regContactParams);
        context.startService(intent);
    }

    public static void setDND(Context context, boolean dnd) {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_SET_DND);
        intent.putExtra(PARAM_DND, dnd);
        context.startService(intent);
    }

    /**
     * Sets up the incoming video feed. If the call does not exist or has been terminated, a disconnected
     * state will be sent to
     * {@link BroadcastEventReceiver#onCallState(String, int, int, int, long)}
     * @param context application context
     * @param accountID account ID
     * @param callID call ID
     * @param surface surface on which to render the incoming video
     */
    public static void setupIncomingVideoFeed(Context context, String accountID, int callID, Surface surface) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_SET_INCOMING_VIDEO);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_CALL_ID, callID);
        intent.putExtra(PARAM_SURFACE, surface);
        context.startService(intent);
    }

    /**
     * Mutes and Un-Mutes video for a call. If the call does not exist or has been terminated, a disconnected
     * state will be sent to
     * {@link BroadcastEventReceiver#onCallState(String, int, int, int, long)}
     * @param context application context
     * @param accountID account ID
     * @param callID call ID
     * @param mute whether to mute or un-mute the video
     */
    public static void setVideoMute(Context context, String accountID, int callID, boolean mute) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_SET_VIDEO_MUTE);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_CALL_ID, callID);
        intent.putExtra(PARAM_VIDEO_MUTE, mute);
        context.startService(intent);
    }

    /**
     * Starts the preview for a call. If the call does not exist or has been terminated, a disconnected
     * state will be sent to
     * {@link BroadcastEventReceiver#onCallState(String, int, int, int, long)}
     * @param context application context
     * @param accountID account ID
     * @param callID call ID
     * @param surface surface on which to render the preview
     */
    public static void startVideoPreview(Context context, String accountID,  int callID, Surface surface) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_START_VIDEO_PREVIEW);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_CALL_ID, callID);
        intent.putExtra(PARAM_SURFACE, surface);
        context.startService(intent);
    }

    /**
     * Rotates the transmitting video (heads up always), according to the device orientation.
     * If the call does not exist or has been terminated, a disconnected state will be sent to
     * {@link BroadcastEventReceiver#onCallState(String, int, int, int, long)}
     * @param context application context
     * @param accountID account ID
     * @param callID call ID
     * @param orientation call ID
     */
    public static void changeVideoOrientation(Context context, String accountID, int callID, int orientation) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_SET_SELF_VIDEO_ORIENTATION);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_CALL_ID, callID);
        intent.putExtra(PARAM_ORIENTATION, orientation);
        context.startService(intent);
    }

    /**
     * Stops the preview for a call. If the call does not exist or has been terminated, a disconnected
     * state will be sent to
     * {@link BroadcastEventReceiver#onCallState(String, int, int, int, long)}
     * @param context application context
     * @param accountID account ID
     * @param callID call ID
     */
    public static void stopVideoPreview(Context context, String accountID, int callID) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_STOP_VIDEO_PREVIEW);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_CALL_ID, callID);
        context.startService(intent);
    }

    /**
     * Switches between front and back camera. If the call does not exist or has been terminated, a disconnected
     * state will be sent to
     * {@link BroadcastEventReceiver#onCallState(String, int, int, int, long)}
     * @param context application context
     * @param accountID account ID
     * @param callID call ID
     */
    public static void switchVideoCaptureDevice(Context context, String accountID, int callID) {
        checkAccount(accountID);

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_SWITCH_VIDEO_CAPTURE_DEVICE);
        intent.putExtra(PARAM_ACCOUNT_ID, accountID);
        intent.putExtra(PARAM_CALL_ID, callID);
        context.startService(intent);
    }

    /**
     * Depending on the configuration (accountConfig.setIpChangeConfig) the reconnection process may differ
     * By default it will try to recover an existing call if present by
     *      restarting the transport
     *      registering
     *      updating via & contact
     *
     * Before calling this you should listen to network connection/disconnection events.
     * As soon as the connection comes back after a disconnection event you can call this
     * to try to reconnect the ongoing call
     * @param context the context
     */
    public static void reconnectCall(Context context) {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_RECONNECT_CALL);
        context.startService(intent);
    }

    /**
     * Sets the camera manager within the PjCamera2Info class
     * it is used to enumerate the video devices without the CAMERA permission
     * @param cm CameraManager retrieved with {@link Context#getSystemService(String)}}
     */
    public static void setCameraManager(CameraManager cm) {
        PjCameraInfo2.SetCameraManager(cm);
    }

    /**
     *
     * @param context the context
     * @param verify enables and disables the sip server certificate verification
     */
    public static void setVerifySipServerCert(Context context, boolean verify) {
        SharedPreferencesHelper.getInstance(context).setVerifySipServerCert(verify);
    }
}
