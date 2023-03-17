package net.gotev.sipservice;

import org.pjsip.pjsua2.LogEntry;
import org.pjsip.pjsua2.LogWriter;
import org.pjsip.pjsua2.pj_log_decoration;

/**
 * connect
 * <p>
 * Created by Vincenzo Esposito on 15/10/21.
 * Copyright © 2021 VoiSmart S.r.l. All rights reserved.
 */
public class SipLogger extends LogWriter {
    public void write(LogEntry entry) {
        switch(entry.getLevel()) {
            case 0:
            case 1:
                Logger.error("PJSIP "+entry.getThreadName(), entry.getMsg());
                break;
            case 2:
                Logger.warning("PJSIP "+entry.getThreadName(), entry.getMsg());
                break;
            case 3:
                Logger.info("PJSIP "+entry.getThreadName(), entry.getMsg());
                break;
            case 4:
            default:
                Logger.debug("PJSIP "+entry.getThreadName(), entry.getMsg());
                break;
        }
    }

    /**
     * Change decor flags as needed
     * @return decor flags
     */
    public long getDecor() {
        return pj_log_decoration.PJ_LOG_HAS_CR
                | pj_log_decoration.PJ_LOG_HAS_INDENT
                | pj_log_decoration.PJ_LOG_HAS_SENDER;
    }
}
