package com.alexbbb.pjsipandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.alexbbb.pjsipandroid.PJSIPAndroidBroadcastEmitter.BroadcastParameters;

import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_status_code;

import static com.alexbbb.pjsipandroid.PJSIPAndroidBroadcastEmitter.BroadcastAction.CALL_STATE;
import static com.alexbbb.pjsipandroid.PJSIPAndroidBroadcastEmitter.BroadcastAction.INCOMING_CALL;
import static com.alexbbb.pjsipandroid.PJSIPAndroidBroadcastEmitter.BroadcastAction.REGISTRATION;

/**
 * Reference implementation to receive events emitted by the PJSIP Android library.
 * @author alexbbb (Aleksandar Gotev)
 */
public class PJSIPAndroidBroadcastReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "PJSIPAndroidBR";

    private Context receiverContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        //save context internally for convenience in subclasses, which can get it with
        //getReceiverContext method
        receiverContext = context;

        String action = intent.getAction();
        PJSIPAndroidBroadcastEmitter broadcastEmitter = PJSIPAndroid.getBroadcastEmitter();

        if (action.equals(broadcastEmitter.getAction(REGISTRATION))) {
            int stateCode = intent.getIntExtra(BroadcastParameters.CODE, -1);
            onRegistration(intent.getStringExtra(BroadcastParameters.ACCOUNT_ID),
                           pjsip_status_code.swigToEnum(stateCode));

        } else if (action.equals(broadcastEmitter.getAction(INCOMING_CALL))) {
            onIncomingCall(intent.getStringExtra(BroadcastParameters.ACCOUNT_ID),
                           intent.getIntExtra(BroadcastParameters.CALL_ID, -1),
                           intent.getStringExtra(BroadcastParameters.DISPLAY_NAME),
                           intent.getStringExtra(BroadcastParameters.REMOTE_URI));

        } else if (action.equals(broadcastEmitter.getAction(CALL_STATE))) {
            int callState = intent.getIntExtra(BroadcastParameters.CALL_STATE, -1);
            onCallState(intent.getStringExtra(BroadcastParameters.ACCOUNT_ID),
                        intent.getIntExtra(BroadcastParameters.CALL_ID, -1),
                        pjsip_inv_state.swigToEnum(callState));
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
        PJSIPAndroidBroadcastEmitter broadcastEmitter = PJSIPAndroid.getBroadcastEmitter();

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(broadcastEmitter.getAction(REGISTRATION));
        intentFilter.addAction(broadcastEmitter.getAction(INCOMING_CALL));
        intentFilter.addAction(broadcastEmitter.getAction(CALL_STATE));
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

    public void onRegistration(String accountID, pjsip_status_code registrationStateCode) {
        PJSIPAndroid.debugLog(LOG_TAG, "onRegistration - accountID: " + accountID +
                ", registrationStateCode: " + registrationStateCode);
    }

    public void onIncomingCall(String accountID, int callID, String displayName, String remoteUri) {
        PJSIPAndroid.debugLog(LOG_TAG, "onIncomingCall - accountID: " + accountID +
                ", callID: " + callID +
                ", displayName: " + displayName +
                ", remoteUri: " + remoteUri);
    }

    public void onCallState(String accountID, int callID, pjsip_inv_state callStateCode) {
        PJSIPAndroid.debugLog(LOG_TAG, "onCallState - accountID: " + accountID +
                ", callID: " + callID +
                ", callStateCode: " + callStateCode);
    }
}
