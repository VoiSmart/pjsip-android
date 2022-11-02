package net.gotev.sipservice;

import static net.gotev.sipservice.SipServiceConstants.OPENH264_CODEC_ID;
import static net.gotev.sipservice.SipServiceConstants.H264_DEF_HEIGHT;
import static net.gotev.sipservice.SipServiceConstants.H264_DEF_WIDTH;
import static net.gotev.sipservice.SipServiceConstants.PROFILE_LEVEL_ID_HEADER;
import static net.gotev.sipservice.SipServiceConstants.PROFILE_LEVEL_ID_JANUS_BRIDGE;

import org.pjsip.pjsua2.CodecFmtpVector;
import org.pjsip.pjsua2.CodecInfo;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.LogConfig;
import org.pjsip.pjsua2.MediaFormatVideo;
import org.pjsip.pjsua2.VidCodecParam;

import java.util.ArrayList;

/**
 * connect
 * <p>
 * Created by Vincenzo Esposito on 11/02/22.
 * Copyright © 2022 VoiSmart S.r.l. All rights reserved.
 */
public class SipServiceUtils {

    private static final String TAG = "SipServiceUtils";
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

    public static void setAudioCodecPriorities (
            ArrayList<CodecPriority> codecPriorities,
            SipEndpoint sipEndpoint
    ) throws Exception {
        if (codecPriorities != null) {
            Logger.debug(TAG, "Setting saved codec priorities...");
            StringBuilder log = new StringBuilder();
            log.append("Saved codec priorities set:\n");
            for (CodecPriority codecPriority : codecPriorities) {
                sipEndpoint.codecSetPriority(codecPriority.getCodecId(), (short) codecPriority.getPriority());
                log.append(codecPriority).append(",");
            }
            Logger.debug(TAG, log.toString());
        } else {
            sipEndpoint.codecSetPriority("OPUS", (short) (CodecPriority.PRIORITY_MAX - 1));
            sipEndpoint.codecSetPriority("PCMA/8000", (short) (CodecPriority.PRIORITY_MAX - 2));
            sipEndpoint.codecSetPriority("PCMU/8000", (short) (CodecPriority.PRIORITY_MAX - 3));
            sipEndpoint.codecSetPriority("G729/8000", (short) CodecPriority.PRIORITY_DISABLED);
            sipEndpoint.codecSetPriority("speex/8000", (short) CodecPriority.PRIORITY_DISABLED);
            sipEndpoint.codecSetPriority("speex/16000", (short) CodecPriority.PRIORITY_DISABLED);
            sipEndpoint.codecSetPriority("speex/32000", (short) CodecPriority.PRIORITY_DISABLED);
            sipEndpoint.codecSetPriority("GSM/8000", (short) CodecPriority.PRIORITY_DISABLED);
            sipEndpoint.codecSetPriority("G722/16000", (short) CodecPriority.PRIORITY_DISABLED);
            sipEndpoint.codecSetPriority("G7221/16000", (short) CodecPriority.PRIORITY_DISABLED);
            sipEndpoint.codecSetPriority("G7221/32000", (short) CodecPriority.PRIORITY_DISABLED);
            sipEndpoint.codecSetPriority("ilbc/8000", (short) CodecPriority.PRIORITY_DISABLED);
            sipEndpoint.codecSetPriority("AMR-WB/16000", (short) CodecPriority.PRIORITY_DISABLED);
            sipEndpoint.codecSetPriority("AMR/8000", (short) CodecPriority.PRIORITY_DISABLED);
            Logger.debug(TAG, "Default codec priorities set!");
        }
    }

    public static void setVideoCodecPriorities (SipEndpoint sipEndpoint) throws Exception {
        sipEndpoint.videoCodecSetPriority(OPENH264_CODEC_ID, (short) (CodecPriority.PRIORITY_MAX_VIDEO -1));

        for (CodecInfo codecInfo: sipEndpoint.videoCodecEnum2()) {
            if (!OPENH264_CODEC_ID.equals(codecInfo.getCodecId())) {
                sipEndpoint.videoCodecSetPriority(
                        codecInfo.getCodecId(),
                        (short) CodecPriority.PRIORITY_DISABLED
                );
            }
        }

        // Set H264 Parameters
        VidCodecParam vidCodecParam = sipEndpoint.getVideoCodecParam(OPENH264_CODEC_ID);
        CodecFmtpVector codecFmtpVector = vidCodecParam.getDecFmtp();
        MediaFormatVideo mediaFormatVideo = vidCodecParam.getEncFmt();
        mediaFormatVideo.setWidth(H264_DEF_WIDTH);
        mediaFormatVideo.setHeight(H264_DEF_HEIGHT);
        vidCodecParam.setEncFmt(mediaFormatVideo);

        for (int i = 0; i < codecFmtpVector.size(); i++) {
            if (PROFILE_LEVEL_ID_HEADER.equals(codecFmtpVector.get(i).getName())) {
                codecFmtpVector.get(i).setVal(PROFILE_LEVEL_ID_JANUS_BRIDGE);
                break;
            }
        }
        vidCodecParam.setDecFmtp(codecFmtpVector);
        sipEndpoint.setVideoCodecParam(OPENH264_CODEC_ID, vidCodecParam);
    }
}
