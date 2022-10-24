package net.gotev.sipservice;

import org.pjsip.pjsua2.Account;
import org.pjsip.pjsua2.CallInfo;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.OnIncomingCallParam;
import org.pjsip.pjsua2.OnRegStateParam;
import org.pjsip.pjsua2.pjsip_status_code;

import java.util.HashMap;
import java.util.Set;

import static net.gotev.sipservice.ObfuscationHelper.getValue;

/**
 * Wrapper around PJSUA2 Account object.
 * @author gotev (Aleksandar Gotev)
 */
public class SipAccount extends Account {

    private static final String LOG_TAG = SipAccount.class.getSimpleName();

    private final HashMap<Integer, SipCall> activeCalls = new HashMap<>();
    private final SipAccountData data;
    private final SipService service;
    private boolean isGuest = false;

    protected SipAccount(SipService service, SipAccountData data) {
        super();
        this.service = service;
        this.data = data;
    }

    public SipService getService() {
        return service;
    }

    public SipAccountData getData() {
        return data;
    }

    public void create() throws Exception {
        create(data.getAccountConfig());
    }

    public void createGuest() throws Exception {
        isGuest = true;
        create(data.getGuestAccountConfig());
    }

    protected void removeCall(int callId) {
        SipCall call = activeCalls.get(callId);

        if (call != null) {
            Logger.debug(LOG_TAG, "Removing call with ID: " + callId);
            activeCalls.remove(callId);
        }

        if (isGuest) {
            service.removeGuestAccount();
            delete();
        }
    }

    public SipCall getCall(int callId) {
        return activeCalls.get(callId);
    }

    public Set<Integer> getCallIDs() {
        return activeCalls.keySet();
    }

    public SipCall addIncomingCall(int callId) {

        SipCall call = new SipCall(this, callId);
        activeCalls.put(callId, call);
        Logger.debug(LOG_TAG, "Added incoming call with ID " + callId
                + " to " + getValue(service.getApplicationContext(), data.getIdUri())
        );
        return call;
    }

    public SipCall addOutgoingCall(final String numberToDial, boolean isVideo, boolean isVideoConference, boolean isTransfer) {

        // check if there's already an ongoing call
        int totalCalls = 0;
        for (SipAccount _sipAccount: SipService.getActiveSipAccounts().values()) {
            totalCalls += _sipAccount.getCallIDs().size();
        }

        // allow calls only if there are no other ongoing calls
        if (totalCalls <= (isTransfer ? 1 : 0)) {
            SipCall call = new SipCall(this);
            call.setVideoParams(isVideo, isVideoConference);

            CallOpParam callOpParam = new CallOpParam();
            try {
                if (numberToDial.startsWith("sip:")) {
                    call.makeCall(numberToDial, callOpParam);
                } else {
                    if ("*".equals(data.getRealm())) {
                        call.makeCall("sip:" + numberToDial, callOpParam);
                    } else {
                        call.makeCall("sip:" + numberToDial + "@" + data.getRealm(), callOpParam);
                    }
                }
                activeCalls.put(call.getId(), call);
                Logger.debug(LOG_TAG, "New outgoing call with ID: " + call.getId());

                return call;

            } catch (Exception exc) {
                Logger.error(LOG_TAG, "Error while making outgoing call", exc);
                return null;
            }
        }
        return null;
    }

    public SipCall addOutgoingCall(final String numberToDial) {
        return addOutgoingCall(numberToDial, false, false, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SipAccount that = (SipAccount) o;

        return data.equals(that.data);

    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public void onRegState(OnRegStateParam prm) {
        service.getBroadcastEmitter().registrationState(data.getIdUri(), prm.getCode());
    }

    @Override
    public void onIncomingCall(OnIncomingCallParam prm) {

        SipCall call = addIncomingCall(prm.getCallId());

        // Send 603 Decline if in DND mode
        if (service.isDND()) {
            try {
                CallerInfo contactInfo = new CallerInfo(call.getInfo());
                service.getBroadcastEmitter().missedCall(contactInfo.getDisplayName(), contactInfo.getRemoteUri());
                call.declineIncomingCall();
                Logger.debug(LOG_TAG, "Decline call with ID: " + prm.getCallId());
            } catch(Exception ex) {
                Logger.error(LOG_TAG, "Error while getting missed call info", ex);
            }
            return;
        }

        // Send 486 Busy Here if there's an already ongoing call
        int totalCalls = 0;
        for (SipAccount _sipAccount: SipService.getActiveSipAccounts().values()) {
            totalCalls += _sipAccount.getCallIDs().size();
        }

        if (totalCalls > 1) {
            try {
                CallerInfo contactInfo = new CallerInfo(call.getInfo());
                service.getBroadcastEmitter().missedCall(contactInfo.getDisplayName(), contactInfo.getRemoteUri());
                call.sendBusyHereToIncomingCall();
                Logger.debug(LOG_TAG, "Sending busy to call ID: " + prm.getCallId());
            } catch(Exception ex) {
                Logger.error(LOG_TAG, "Error while getting missed call info", ex);
            }
            return;
        }

        try {
            // Answer with 180 Ringing
            CallOpParam callOpParam = new CallOpParam();
            callOpParam.setStatusCode(pjsip_status_code.PJSIP_SC_RINGING);
            call.answer(callOpParam);
            Logger.debug(LOG_TAG, "Sending 180 ringing");

            String displayName, remoteUri;
            try {
                CallerInfo contactInfo = new CallerInfo(call.getInfo());
                displayName = contactInfo.getDisplayName();
                remoteUri = contactInfo.getRemoteUri();
            } catch (Exception ex) {
                Logger.error(LOG_TAG, "Error while getting caller info", ex);
                throw ex;
            }

            // check for video in remote SDP
            CallInfo callInfo = call.getInfo();
            boolean isVideo = (callInfo.getRemOfferer() && callInfo.getRemVideoCount() > 0);

            service.getBroadcastEmitter().incomingCall(data.getIdUri(), prm.getCallId(),
                            displayName, remoteUri, isVideo);

        } catch (Exception ex) {
            Logger.error(LOG_TAG, "Error while getting caller info", ex);
        }
    }
}
