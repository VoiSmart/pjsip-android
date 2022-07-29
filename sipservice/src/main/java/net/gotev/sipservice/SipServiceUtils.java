package net.gotev.sipservice;

import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.LogConfig;

/**
 * connect
 * <p>
 * Created by Vincenzo Esposito on 11/02/22.
 * Copyright © 2022 VoiSmart S.r.l. All rights reserved.
 */
public class SipServiceUtils {

    public static boolean ENABLE_SIP_LOGGING = false;

    // Keeping the reference avoids the logger being garbage collected thus crashing the lib
    @SuppressWarnings("FieldCanBeLocal")
    private static SipLogger sipLogger;

    /**
     * Sets logger writer and decor flags on the endpoint config
     * Change flags as needed
     * Applies only for debug builds
     */
    public static void setSipLogger(EpConfig epConfig) {
        if (BuildConfig.DEBUG && ENABLE_SIP_LOGGING) {
            LogConfig logCfg = epConfig.getLogConfig();
            sipLogger = new SipLogger();
            logCfg.setWriter(sipLogger);
            logCfg.setDecor(sipLogger.getDecor() | sipLogger.getDecor());
        }
    }
}
