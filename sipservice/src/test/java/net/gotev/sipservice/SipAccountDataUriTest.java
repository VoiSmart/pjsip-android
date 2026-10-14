package net.gotev.sipservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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
        assertEquals("sip:apps.sip.example.com;transport=tls",
                account(SipAccountTransport.TLS).getProxyUri(true));
        assertEquals("sip:apps.sip.example.com;transport=tcp",
                account(SipAccountTransport.TCP).getProxyUri(true));
    }

    @Test
    public void registrarUriFollowsTheSrvMode() {
        SipAccountData data = account(SipAccountTransport.TCP);

        assertEquals("sip:apps.sip.example.com:5070", data.getRegistrarUri(false));
        assertEquals("sip:apps.sip.example.com", data.getRegistrarUri(true));
    }

    // The TLS certificate check matches against the configured host, never the SRV target.
    @Test
    public void srvModeNeverChangesTheConfiguredHost() {
        SipAccountData data = account(SipAccountTransport.TLS);

        data.getProxyUri(true);
        data.getRegistrarUri(true);

        assertEquals("apps.sip.example.com", data.getHost());
    }

    @Test
    public void srvModeUsesTheSrvDomainAsGiven() {
        SipAccountData data = account(SipAccountTransport.TLS).setSrvDomain("example.com");

        assertEquals("sip:example.com;transport=tls", data.getProxyUri(true));
        assertEquals("sip:example.com", data.getRegistrarUri(true));
        assertEquals("example.com", data.getSipServerDomain(true));
        assertEquals("apps.sip.example.com", data.getHost());
    }

    @Test
    public void srvDomainIsIgnoredWithoutSrv() {
        SipAccountData data = account(SipAccountTransport.TLS).setSrvDomain("example.com");

        assertEquals("sip:apps.sip.example.com:5070;transport=tls", data.getProxyUri(false));
        assertEquals("sip:apps.sip.example.com:5070", data.getRegistrarUri(false));
        assertEquals("apps.sip.example.com", data.getSipServerDomain(false));
    }

    @Test
    public void unsetSrvDomainFallsBackToTheHost() {
        assertEquals("apps.sip.example.com",
                account(SipAccountTransport.TCP).getSipServerDomain(true));
        assertEquals("apps.sip.example.com",
                account(SipAccountTransport.TCP).setSrvDomain("").getSipServerDomain(true));
    }

    // A changed SRV domain must re-create the account through the modify path.
    @Test
    public void srvDomainTakesPartInEquality() {
        SipAccountData plain = account(SipAccountTransport.TCP).setPassword("pwd").setSipInstance("id");
        SipAccountData same = account(SipAccountTransport.TCP).setPassword("pwd").setSipInstance("id");
        SipAccountData withSrv = account(SipAccountTransport.TCP).setPassword("pwd").setSipInstance("id")
                .setSrvDomain("example.com");

        assertEquals(plain, same);
        assertEquals(plain.hashCode(), same.hashCode());
        assertNotEquals(plain, withSrv);
        assertNotEquals(plain.hashCode(), withSrv.hashCode());
    }
}
