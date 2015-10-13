package com.alexbbb.pjsipandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.alexbbb.pjsipandroid.PJSIPAndroidBroadcastEmitter.BroadcastParameters;

import static com.alexbbb.pjsipandroid.PJSIPAndroidBroadcastEmitter.BroadcastAction.CALL_STATE;
import static com.alexbbb.pjsipandroid.PJSIPAndroidBroadcastEmitter.BroadcastAction.INCOMING_CALL;
import static com.alexbbb.pjsipandroid.PJSIPAndroidBroadcastEmitter.BroadcastAction.REGISTRATION;

/**
 * Reference implementation to receive events emitted by the PJSIP Android library.
 * @author alexbbb (Aleksandar Gotev)
 */
public class PJSIPAndroidBroadcastReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "PJSIPAndroidBR";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        PJSIPAndroidBroadcastEmitter broadcastEmitter = PJSIPAndroid.getBroadcastEmitter();

        if (action.equals(broadcastEmitter.getAction(REGISTRATION))) {
            onRegistration(intent.getStringExtra(BroadcastParameters.ACCOUNT_ID),
                           intent.getIntExtra(BroadcastParameters.CODE, -1));

        } else if (action.equals(broadcastEmitter.getAction(INCOMING_CALL))) {
            onIncomingCall(intent.getStringExtra(BroadcastParameters.ACCOUNT_ID),
                           intent.getIntExtra(BroadcastParameters.CALL_ID, -1),
                           intent.getStringExtra(BroadcastParameters.DISPLAY_NAME),
                           intent.getStringExtra(BroadcastParameters.REMOTE_URI));

        } else if (action.equals(broadcastEmitter.getAction(CALL_STATE))) {
            onCallState(intent.getStringExtra(BroadcastParameters.ACCOUNT_ID),
                        intent.getIntExtra(BroadcastParameters.CALL_ID, -1),
                        intent.getIntExtra(BroadcastParameters.CALL_STATE, -1));
        }
    }

    public void onRegistration(String accountID, int registrationStateCode) {
        Log.d(LOG_TAG, "onRegistration - accountID: " + accountID +
                       ", registrationStateCode: " + registrationStateCode);
    }

    public void onIncomingCall(String accountID, int callID, String displayName, String remoteUri) {
        Log.d(LOG_TAG, "onIncomingCall - accountID: " + accountID +
                       ", callID: " + callID +
                       ", displayName: " + displayName +
                       ", remoteUri: " + remoteUri);
    }

    public void onCallState(String accountID, int callID, int callStateCode) {
        Log.d(LOG_TAG, "onIncomingCall - accountID: " + accountID +
                       ", callID: " + callID +
                       ", callStateCode: " + callStateCode);
    }
}
