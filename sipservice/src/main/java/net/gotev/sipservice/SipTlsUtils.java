package net.gotev.sipservice;

import android.content.Context;

import org.pjsip.pjsua2.TlsConfig;
import org.pjsip.pjsua2.TransportConfig;

import java.io.IOException;
import java.io.InputStream;

/**
 * connect
 * <p>
 * Created by Vincenzo Esposito on 07/10/22.
 * Copyright © 2022 VoiSmart S.r.l. All rights reserved.
 */
public class SipTlsUtils {

    public static final String TAG = "SipTlsUtils";
    private static final String CA_FILE_NAME = "ca-bundle.crt";

    public static void setTlsConfig(
            Context context,
            boolean verifyEnabled,
            TransportConfig tlsTransport
    ) {
        if (verifyEnabled && caFileExists(context)) {
            try {
                InputStream inputStream = context.getAssets().open(CA_FILE_NAME);
                int size = inputStream.available();
                byte[] buffer = new byte[size];
                inputStream.read(buffer);
                TlsConfig tlsConfig = tlsTransport.getTlsConfig();
                tlsConfig.setCaBuf(new String(buffer));
                tlsConfig.setVerifyServer(true);
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
