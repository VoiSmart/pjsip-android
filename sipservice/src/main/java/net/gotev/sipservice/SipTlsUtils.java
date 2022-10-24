package net.gotev.sipservice;

import android.content.Context;

import org.pjsip.pjsua2.TlsConfig;
import org.pjsip.pjsua2.TransportConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * connect
 * <p>
 * Created by Vincenzo Esposito on 07/10/22.
 * Copyright © 2022 VoiSmart S.r.l. All rights reserved.
 */
public class SipTlsUtils {

    public static final String TAG = "Endpoint";
    private static final String CA_FILE_NAME = "ca-bundle.crt";

    public static boolean isWildcardValid(ArrayList<String> certNames, String host) {
        Logger.info(TAG, "Trying to verify if wildcard certificate is valid");

        for (String name: certNames) {
            Logger.debug(TAG, "Validating host: "+host+" against cert name: "+name );
            if (SipTlsUtils.isWildCardCertValid(name, host.split("\\."))) return true;
        }
        return false;
    }

    private static boolean isWildCardCertValid(String certName, String[] tokens) {
        boolean match = false;

        String[] certTokens = certName.split("\\.");
        if (certTokens.length == tokens.length) {
            for (int i = tokens.length -1; i >= 0; i--) {
                String certToken = certTokens[i];
                if (certToken.contains("*")) {
                    certToken = certToken.replace("*", ".*");
                }
                if (tokens[i].equals(certToken) || tokens[i].matches(certToken)) {
                    match = true;
                } else {
                    Logger.debug(TAG, certName + " is not valid");
                    match = false;
                    break;
                }
            }
        }
        return match;
    }

    public static void setTlsConfig(
            Context context,
            boolean verifyEnabled,
            TransportConfig tlsTransport
    ) {
        if (verifyEnabled && caFileExists(context)) {
            try {
                Logger.info(TAG, "Setting sip ca verification");
                InputStream inputStream = context.getAssets().open(CA_FILE_NAME);
                int size = inputStream.available();
                byte[] buffer = new byte[size];
                inputStream.read(buffer);
                TlsConfig tlsConfig = tlsTransport.getTlsConfig();
                tlsConfig.setCaBuf(new String(buffer));
                /*
                 * The server verification check is disabled
                 * since we need to perform the check internally to validate wildcard
                 * see {@link SipEndpoint#onTransportState(OnTransportStateParam)}
                 * and https://github.com/pjsip/pjproject/pull/2328#issuecomment-595004025
                 *
                 * tlsConfig.setVerifyServer(true);
                 */
                tlsTransport.setTlsConfig(tlsConfig);
            } catch (IOException e) {
                Logger.error(TAG, "Unable to set server tls verification", e);
            }
        }
    }

    private static boolean caFileExists(Context context) {
        try {
            String[] list = context.getAssets().list("");
            for (String file: list) {
                if (file.equals(CA_FILE_NAME))
                    return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
