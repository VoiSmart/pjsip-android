package net.gotev.sipservice;

import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.OnIpChangeProgressParam;
import org.pjsip.pjsua2.OnTransportStateParam;
import org.pjsip.pjsua2.SslCertName;
import org.pjsip.pjsua2.pj_constants_;
import org.pjsip.pjsua2.pj_ssl_cert_verify_flag_t;
import org.pjsip.pjsua2.pjsua_ip_change_op;

import java.util.ArrayList;

public class SipEndpoint extends Endpoint {
    private final SipService service;
    @SuppressWarnings("FieldCanBeLocal")
    private final String TAG = "SipEndpoint";

    public SipEndpoint(SipService service) {
        super();
        this.service = service;
    }

    @Override
    public void onTransportState(OnTransportStateParam prm) {
        super.onTransportState(prm);

        if (service.getSharedPreferencesHelper().isVerifySipServerCert() &&
                prm.getType().equalsIgnoreCase("TLS")
        ) {
            long verifyMsg = prm.getTlsInfo().getVerifyStatus();
            int binSuccessMsg = pj_ssl_cert_verify_flag_t.PJ_SSL_CERT_ESUCCESS;
            int binIdentityNotMatchMsg = pj_ssl_cert_verify_flag_t.PJ_SSL_CERT_EIDENTITY_NOT_MATCH;
            boolean isSuccess = verifyMsg == binSuccessMsg;
            boolean isIdentityMismatch = verifyMsg == binIdentityNotMatchMsg;
            String host = SipService.getActiveSipAccounts().elements().nextElement().getData().getHost();
            if (!(isSuccess || (isIdentityMismatch && SipTlsUtils.isWildcardValid(getCertNames(prm), host)))) {
                Logger.error(TAG, "The Sip Certificate is not valid");
                service.getBroadcastEmitter().notifyTlsVerifyStatusFailed();
                service.stopSelf();
            } else {
                Logger.info(TAG, "The Sip Certificate verification succeeded");
            }
        }
    }

    @Override
    public void onIpChangeProgress(OnIpChangeProgressParam prm) {
        super.onIpChangeProgress(prm);
        if (prm.getStatus() != pj_constants_.PJ_SUCCESS) {
            hangupAllCalls();
            service.getBroadcastEmitter().callReconnectionState(CallReconnectionState.FAILED);
            return;
        }

        if (prm.getOp() == pjsua_ip_change_op.PJSUA_IP_CHANGE_OP_COMPLETED) {
            service.getBroadcastEmitter().callReconnectionState(CallReconnectionState.SUCCESS);
        }
    }

    private ArrayList<String> getCertNames(OnTransportStateParam prm) {
        ArrayList<String> certNames = new ArrayList<>();
        certNames.add(prm.getTlsInfo().getRemoteCertInfo().getSubjectCn());
        for (SslCertName name : prm.getTlsInfo().getRemoteCertInfo().getSubjectAltName()) {
            certNames.add(name.getName());
        }
        return certNames;
    }

}
