package net.gotev.sipservicedemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.Toast;

import net.gotev.sipservice.BroadcastEventReceiver;
import net.gotev.sipservice.CodecPriority;
import net.gotev.sipservice.Logger;
import net.gotev.sipservice.SipAccountData;
import net.gotev.sipservice.SipServiceCommand;

import org.pjsip.pjsua2.pjsip_status_code;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.sipServer) EditText mSipServer;
    @Bind(R.id.sipPort) EditText mSipPort;
    @Bind(R.id.realm) EditText mRealm;
    @Bind(R.id.username) EditText mUsername;
    @Bind(R.id.password) EditText mPassword;
    @Bind(R.id.numberToCall) EditText mNumberToCall;

    private static final String KEY_SIP_ACCOUNT = "sip_account";
    private SipAccountData mSipAccount;

    private BroadcastEventReceiver sipEvents = new BroadcastEventReceiver() {

        @Override
        public void onRegistration(String accountID, pjsip_status_code registrationStateCode) {
            if (registrationStateCode == pjsip_status_code.PJSIP_SC_OK) {
                Toast.makeText(MainActivity.this, "Registered", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Unregistered", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onReceivedCodecPriorities(ArrayList<CodecPriority> codecPriorities) {
            for (CodecPriority codec : codecPriorities) {
                if (codec.getCodecName().startsWith("PCM")) {
                    codec.setPriority(CodecPriority.PRIORITY_MAX);
                } else {
                    codec.setPriority(CodecPriority.PRIORITY_DISABLED);
                }
            }
            SipServiceCommand.setCodecPriorities(MainActivity.this, codecPriorities);
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mSipAccount != null) {
            outState.putParcelable(KEY_SIP_ACCOUNT, mSipAccount);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Logger.setLogLevel(Logger.LogLevel.DEBUG);

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SIP_ACCOUNT)) {
            mSipAccount = savedInstanceState.getParcelable(KEY_SIP_ACCOUNT);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sipEvents.unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sipEvents.register(this);
    }

    @OnClick(R.id.register)
    public void onRegister() {
        mSipAccount = new SipAccountData();

        if (!mSipServer.getText().toString().isEmpty()) {
            mSipAccount.setHost(mSipServer.getText().toString())
                    .setPort(Integer.valueOf(mSipPort.getText().toString()))
                    .setTcpTransport(true)
                    .setUsername(mUsername.getText().toString())
                    .setPassword(mPassword.getText().toString())
                    .setRealm(mRealm.getText().toString());
        } else {
            mSipAccount.setHost("192.168.1.154")
                    .setPort(5060)
                    .setTcpTransport(true)
                    .setUsername("200")
                    .setPassword("password200")
                    .setRealm("devel.it");
        }

        SipServiceCommand.setAccount(this, mSipAccount);
        SipServiceCommand.getCodecPriorities(this);
    }

    @OnClick(R.id.call)
    public void onCall() {
        if (mSipAccount == null) {
            Toast.makeText(this, "Add an account and register it first", Toast.LENGTH_LONG).show();
            return;
        }

        String number = mNumberToCall.getText().toString();

        if (number.isEmpty()) {
            number = "*9000";
            //Toast.makeText(this, "Provide a number to call", Toast.LENGTH_SHORT).show();
            //return;
        }

        SipServiceCommand.makeCall(this, mSipAccount.getIdUri(), number);
    }

    @OnClick(R.id.hangUp)
    public void onTerminate() {
        SipServiceCommand.hangUpActiveCalls(this, mSipAccount.getIdUri());
    }
}
