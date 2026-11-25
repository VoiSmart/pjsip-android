package net.gotev.sipservice;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for {@link SipCall#reasonHeaderValue(int, String)}, the RFC 3326 Reason header value
 * added to the final response when an incoming call is declined with a reason.
 */
public class SipCallDeclineReasonTest {

    @Test
    public void reasonCarriesTheStatusCodeAndTheText() {
        assertEquals("SIP;cause=500;text=\"Microphone permission denied\"",
                SipCall.reasonHeaderValue(500, "Microphone permission denied"));
    }

    @Test
    public void quotesAndBackslashesInTheTextAreEscaped() {
        assertEquals("SIP;cause=500;text=\"Call \\\"screen\\\" C:\\\\app\"",
                SipCall.reasonHeaderValue(500, "Call \"screen\" C:\\app"));
    }

    @Test
    public void lineBreaksInTheTextAreDroppedSoNoHeaderCanBeInjected() {
        assertEquals("SIP;cause=500;text=\"BrokenX-Injected: yes\"",
                SipCall.reasonHeaderValue(500, "Broken\r\nX-Injected: yes"));
    }
}
