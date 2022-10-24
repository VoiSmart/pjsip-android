package net.gotev.sipservice;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.util.ArrayList;

/**
 * connect
 * <p>
 * Created by Vincenzo Esposito on 24/10/22.
 * Copyright © 2022 VoiSmart S.r.l. All rights reserved.
 */
public class SipTlsUtilsTest {

    private final String host = "cert.test.com";

    @Test
    public void testWildCardCertValidWOStar() {
        ArrayList<String> certNames = new ArrayList<>();
        certNames.add("test.com");
        certNames.add("t.test.com");
        certNames.add("cert.test.com");
        assert SipTlsUtils.isWildcardValid(certNames, host);
    }

    @Test
    public void testWildCardCertValidWStar() {
        ArrayList<String> certNames = new ArrayList<>();
        certNames.add("*pp.test.com");
        certNames.add("test.com");
        certNames.add("*.test.com");
        certNames.add("t.test.com");
        assert SipTlsUtils.isWildcardValid(certNames, host);
    }

    @Test
    public void testWildCardCertValidWStarAfter() {
        ArrayList<String> certNames = new ArrayList<>();
        certNames.add("c*.test.com");
        certNames.add("t.test.com");
        assert SipTlsUtils.isWildcardValid(certNames, host);
    }

    @Test
    public void testWildCardCertValidWStarBefore() {
        ArrayList<String> certNames = new ArrayList<>();
        certNames.add("*t.test.com");
        assert SipTlsUtils.isWildcardValid(certNames, host);
    }

    @Test
    public void testWildCardCertInvalidWOStarDifferent() {
        ArrayList<String> certNames = new ArrayList<>();
        certNames.add("test.cert.cm");
        assertFalse(SipTlsUtils.isWildcardValid(certNames, host));
    }

    @Test
    public void testWildCardCertInvalidWOStarSize() {
        ArrayList<String> certNames = new ArrayList<>();
        certNames.add("first.test.cert.com");
        assertFalse(SipTlsUtils.isWildcardValid(certNames, host));
    }

    @Test
    public void testWildCardCertInvalidWStar() {
        ArrayList<String> certNames = new ArrayList<>();
        certNames.add("first.*.test.com");
        certNames.add("t.test.com");
        assertFalse(SipTlsUtils.isWildcardValid(certNames, host));
    }

    @Test
    public void testWildCardCertInvalidWStarAfter() {
        ArrayList<String> certNames = new ArrayList<>();
        certNames.add("test.com");
        certNames.add("first.c*.test.com");
        assertFalse(SipTlsUtils.isWildcardValid(certNames, host));
    }
}
