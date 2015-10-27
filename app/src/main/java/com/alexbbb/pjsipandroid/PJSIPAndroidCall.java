package com.alexbbb.pjsipandroid;

import android.util.Log;

import org.pjsip.pjsua2.AudDevManager;
import org.pjsip.pjsua2.AudioMedia;
import org.pjsip.pjsua2.Call;
import org.pjsip.pjsua2.CallInfo;
import org.pjsip.pjsua2.CallMediaInfo;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.CallSetting;
import org.pjsip.pjsua2.Media;
import org.pjsip.pjsua2.OnCallMediaStateParam;
import org.pjsip.pjsua2.OnCallStateParam;
import org.pjsip.pjsua2.pjmedia_type;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_status_code;
import org.pjsip.pjsua2.pjsua_call_flag;
import org.pjsip.pjsua2.pjsua_call_media_status;

/**
 * Wrapper around PJSUA2 Call object.
 * @author alexbbb (Aleksandar Gotev)
 */
public class PJSIPAndroidCall extends Call {

    private static final String LOG_TAG = "PJSIPCall";

    private PJSIPAndroidAccount account;
    private boolean localHold = false;
    private boolean localMute = false;

    /**
     * Incoming call constructor.
     * @param account the account which own this call
     * @param callID the id of this call
     */
    public PJSIPAndroidCall(PJSIPAndroidAccount account, int callID) {
        super(account, callID);
        this.account = account;
    }

    /**
     * Outgoing call constructor.
     * @param account account which owns this call
     */
    public PJSIPAndroidCall(PJSIPAndroidAccount account) {
        super(account);
        this.account = account;
    }

    @Override
    public void onCallState(OnCallStateParam prm) {
        try {
            CallInfo info = getInfo();
            int callID = info.getId();
            pjsip_inv_state callState = info.getState();

            PJSIPAndroid.getBroadcastEmitter()
                        .callState(account.getData().getIdUri(), callID, callState.swigValue());

            /**
             * From: http://www.pjsip.org/docs/book-latest/html/call.html#call-disconnection
             *
             * Call disconnection event is a special event since once the callback that
             * reports this event returns, the call is no longer valid and any operations
             * invoked to the call object will raise error exception.
             * Thus, it is recommended to delete the call object inside the callback.
             */
            if (callState == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED) {
                PJSIPAndroid.stopRingTone();
                account.removeCall(callID);
            } else if (callState == pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED) {
                PJSIPAndroid.stopRingTone();
            }

        } catch (Exception exc) {
            Log.e(LOG_TAG, "onCallState: error while getting call info", exc);
        }

    }

    @Override
    public void onCallMediaState(OnCallMediaStateParam prm) {

        CallInfo info;
        try {
            info = getInfo();
        } catch (Exception exc) {
            Log.e(LOG_TAG, "onCallMediaState: error while getting call info", exc);
            return;
        }

        for (int i = 0; i < info.getMedia().size(); i++) {
            Media media = getMedia(i);
            CallMediaInfo mediaInfo = info.getMedia().get(i);

            if (mediaInfo.getType() == pjmedia_type.PJMEDIA_TYPE_AUDIO
                    && media != null
                    && mediaInfo.getStatus() == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE) {
                AudioMedia audioMedia = AudioMedia.typecastFromMedia(media);

                PJSIPAndroid.stopRingTone();

                // connect the call audio media to sound device
                try {
                    AudDevManager mgr = PJSIPAndroid.getAudDevManager();
                    audioMedia.startTransmit(mgr.getPlaybackDevMedia());
                    mgr.getCaptureDevMedia().startTransmit(audioMedia);
                } catch (Exception exc) {
                    Log.e(LOG_TAG, "Error while connecting audio media to sound device", exc);
                }
            }
        }
    }

    public void acceptIncomingCall() {
        CallOpParam param = new CallOpParam();
        param.setStatusCode(pjsip_status_code.PJSIP_SC_OK);

        try {
            answer(param);
        } catch (Exception exc) {
            Log.e(LOG_TAG, "Failed to accept incoming call", exc);
        }
    }

    public void sendBusyHereToIncomingCall() {
        CallOpParam param = new CallOpParam();
        param.setStatusCode(pjsip_status_code.PJSIP_SC_BUSY_HERE);

        try {
            answer(param);
        } catch (Exception exc) {
            Log.e(LOG_TAG, "Failed to send busy here", exc);
        }
    }

    public void declineIncomingCall() {
        CallOpParam param = new CallOpParam();
        param.setStatusCode(pjsip_status_code.PJSIP_SC_DECLINE);

        try {
            answer(param);
        } catch (Exception exc) {
            Log.e(LOG_TAG, "Failed to decline incoming call", exc);
        }
    }

    public void hangUp() {
        CallOpParam param = new CallOpParam();
        param.setStatusCode(pjsip_status_code.PJSIP_SC_DECLINE);

        try {
            hangup(param);
        } catch (Exception exc) {
            Log.e(LOG_TAG, "Failed to hangUp call", exc);
        }
    }

    /**
     * Utility method to mute/unmute the device microphone during a call.
     * @param mute true to mute the microphone, false to un-mute it
     * @throws Exception if an error occurs during the mute/unmute operation
     */
    public void setMute(boolean mute) throws Exception {
        // return immediately if we are not changing the current state
        if ((localMute && mute) || (!localMute && !mute)) return;

        CallInfo info;
        try {
            info = getInfo();
        } catch (Exception exc) {
            Log.e(LOG_TAG, "setMute: error while getting call info", exc);
            return;
        }

        for (int i = 0; i < info.getMedia().size(); i++) {
            Media media = getMedia(i);
            CallMediaInfo mediaInfo = info.getMedia().get(i);

            if (mediaInfo.getType() == pjmedia_type.PJMEDIA_TYPE_AUDIO
                    && media != null
                    && mediaInfo.getStatus() == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE) {
                AudioMedia audioMedia = AudioMedia.typecastFromMedia(media);

                // connect or disconnect the captured audio
                try {
                    AudDevManager mgr = PJSIPAndroid.getAudDevManager();

                    if (mute) {
                        mgr.getCaptureDevMedia().stopTransmit(audioMedia);
                        localMute = true;
                    } else {
                        mgr.getCaptureDevMedia().startTransmit(audioMedia);
                        localMute = false;
                    }

                } catch (Exception exc) {
                    Log.e(LOG_TAG, "setMute: error while connecting audio media to sound device", exc);
                }
            }
        }
    }

    public boolean isLocalMute() {
        return localMute;
    }

    /**
     * Utility method to transfer a call to a number in the same realm as the account to
     * which this call belongs to. If you want to transfer the call to a different realm, you
     * have to pass the full string in this format: sip:NUMBER@REALM. E.g. sip:200@mycompany.com
     * @param destination destination to which to transfer the call.
     * @throws Exception if an error occurs during the call transfer
     */
    public void transferTo(String destination) throws Exception {
        String transferString;

        if (destination.startsWith("sip:")) {
            transferString = destination;
        } else {
            transferString = "sip:" + destination + "@" + account.getData().getRealm();
        }

        CallOpParam param = new CallOpParam();

        xfer(transferString, param);
    }

    public void setHold(boolean hold) {
        // return immediately if we are not changing the current state
        if ((localHold && hold) || (!localHold && !hold)) return;

        CallOpParam param = new CallOpParam();

        try {
            if (hold) {
                PJSIPAndroid.debugLog(LOG_TAG, "holding call with ID " + getId());
                setHold(param);
                localHold = true;
            } else {
                // http://lists.pjsip.org/pipermail/pjsip_lists.pjsip.org/2015-March/018246.html
                PJSIPAndroid.debugLog(LOG_TAG, "un-holding call with ID " + getId());
                CallSetting opt = param.getOpt();
                opt.setAudioCount(1);
                opt.setVideoCount(0);
                opt.setFlag(pjsua_call_flag.PJSUA_CALL_UNHOLD.swigValue());
                reinvite(param);
                localHold = false;
            }
        } catch (Exception exc) {
            String operation = hold ? "hold" : "unhold";
            Log.e(LOG_TAG, "Error while trying to " + operation + " call", exc);
        }
    }

    public boolean toggleHold() {
        if (localHold) {
            setHold(false);
            return !localHold;
        }

        setHold(true);
        return localHold;
    }

    public boolean isLocalHold() {
        return localHold;
    }
}
