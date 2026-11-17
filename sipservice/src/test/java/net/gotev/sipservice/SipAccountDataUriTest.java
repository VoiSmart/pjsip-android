package net.gotev.sipservice;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SipAccountDataUriTest {

    private SipAccountData account(SipAccountTransport transport) {
        return new SipAccountData()
                .setUsername("200")
                .setRealm("example.com")
                .setHost("apps.sip.example.com")
                .setPort(5070)
                .setTransport(transport);
    }

    @Test
    public void proxyUriKeepsThePortWithoutSrv() {
        assertEquals("sip:apps.sip.example.com:5070;transport=tls",
                account(SipAccountTransport.TLS).getProxyUri(false));
    }

    @Test
    public void proxyUriDropsThePortButKeepsTheTransportWithSrv() {
        assertEquals("sip:example.com;transport=tls",
                account(SipAccountTransport.TLS).getProxyUri(true));
        assertEquals("sip:example.com;transport=tcp",
                account(SipAccountTransport.TCP).getProxyUri(true));
    }

    @Test
    public void registrarUriFollowsTheSrvMode() {
        SipAccountData data = account(SipAccountTransport.TCP);

        assertEquals("sip:apps.sip.example.com:5070", data.getRegistrarUri(false));
        assertEquals("sip:example.com", data.getRegistrarUri(true));
    }

    // The host stays the fallback for when the stack can't use SRV.
    @Test
    public void srvModeNeverChangesTheConfiguredHost() {
        SipAccountData data = account(SipAccountTransport.TLS);

        data.getProxyUri(true);
        data.getRegistrarUri(true);

        assertEquals("apps.sip.example.com", data.getHost());
    }

    // The TLS certificate is checked against the realm, never the SRV target (RFC 5922).
    @Test
    public void srvModeUsesTheRealmAsTheServerDomain() {
        SipAccountData data = account(SipAccountTransport.TLS);

        assertEquals("example.com", data.getSipServerDomain(true));
        assertEquals("apps.sip.example.com", data.getSipServerDomain(false));
    }
}
