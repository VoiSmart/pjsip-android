package net.gotev.sipservice;

/**
 * connect
 * <p>
 * Created by Vincenzo Esposito on 29/07/22.
 * Copyright © 2022 VoiSmart S.r.l. All rights reserved.
 */
public enum SipAccountTransport {
    UDP,
    TCP,
    TLS;

    public static SipAccountTransport getTransportByCode(int code) {
        switch (code) {
            case 0:
            default: return UDP;
            case 1: return TCP;
            case 2: return TLS;
        }
    }
}
