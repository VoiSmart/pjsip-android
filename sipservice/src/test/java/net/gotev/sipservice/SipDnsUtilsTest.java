package net.gotev.sipservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SipDnsUtilsTest {

    private static InetAddress ip(String address) throws UnknownHostException {
        return InetAddress.getByName(address);
    }

    @Test
    public void noAddressesGiveAnEmptyList() {
        assertTrue(SipDnsUtils.sanitize(null).isEmpty());
        assertTrue(SipDnsUtils.sanitize(Collections.emptyList()).isEmpty());
    }

    @Test
    public void ipv4ComesFirstAndOrderIsKept() throws UnknownHostException {
        List<String> servers = SipDnsUtils.sanitize(
                Arrays.asList(ip("2001:db8::1"), ip("192.168.1.1"), ip("8.8.8.8")));

        assertEquals(Arrays.asList("192.168.1.1", "8.8.8.8", "2001:db8:0:0:0:0:0:1"), servers);
    }

    @Test
    public void ipv6ScopeIdIsStripped() throws UnknownHostException {
        InetAddress scoped = Inet6Address.getByAddress(
                null, ip("fe80::1").getAddress(), 3);

        assertEquals(Collections.singletonList("fe80:0:0:0:0:0:0:1"),
                SipDnsUtils.sanitize(Collections.singletonList(scoped)));
    }

    @Test
    public void duplicatesAreRemoved() throws UnknownHostException {
        assertEquals(Collections.singletonList("8.8.8.8"),
                SipDnsUtils.sanitize(Arrays.asList(ip("8.8.8.8"), ip("8.8.8.8"))));
    }

    @Test
    public void onlyStrictPrivateDnsCountsAsStrict() {
        assertFalse(SipDnsUtils.isStrictPrivateDns(false, null));
        // Opportunistic: active, no server name.
        assertFalse(SipDnsUtils.isStrictPrivateDns(true, null));
        assertFalse(SipDnsUtils.isStrictPrivateDns(true, ""));
        assertTrue(SipDnsUtils.isStrictPrivateDns(true, "dns.example.com"));
    }
}
