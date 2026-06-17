package net.gotev.sipservice;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for {@link SipCall#isCompletedElsewhere(String)}, the parser that detects
 * an incoming CANCEL carrying an RFC 3326 "Reason: SIP;cause=200" header (call
 * answered on another device). Matching is on the SIP cause code only.
 */
public class SipCallReasonHeaderTest {

    private static final String CANCEL_LINE = "CANCEL sip:200@example.com SIP/2.0\r\n";

    @Test
    public void cancelWithReasonCause200IsCompletedElsewhere() {
        String msg = CANCEL_LINE + "Via: SIP/2.0/UDP example.com\r\n" + "Reason: SIP;cause=200;" +
                "text=\"Call completed elsewhere\"\r\n" + "\r\n";
        assertTrue(SipCall.isCompletedElsewhere(msg));
    }

    @Test
    public void cancelWithReasonCause200AndWhitespaceIsCompletedElsewhere() {
        String msg = CANCEL_LINE + "Reason: SIP ;cause = 200\r\n\r\n";
        assertTrue(SipCall.isCompletedElsewhere(msg));
    }

    @Test
    public void cancelWithoutReasonHeaderIsNotCompletedElsewhere() {
        String msg = CANCEL_LINE + "Via: SIP/2.0/UDP example.com\r\n\r\n";
        assertFalse(SipCall.isCompletedElsewhere(msg));
    }

    @Test
    public void cancelWithDifferentCauseIsNotCompletedElsewhere() {
        String msg = CANCEL_LINE + "Reason: SIP;cause=487;text=\"Request Terminated\"\r\n\r\n";
        assertFalse(SipCall.isCompletedElsewhere(msg));
    }

    @Test
    public void cause200OnNonSipProtocolIsNotCompletedElsewhere() {
        // Q.850 cause numbering is unrelated to SIP status codes; must not match.
        String msg = CANCEL_LINE + "Reason: Q.850;cause=200\r\n\r\n";
        assertFalse(SipCall.isCompletedElsewhere(msg));
    }

    @Test
    public void cause2000IsNotMatchedAsCause200() {
        String msg = CANCEL_LINE + "Reason: SIP;cause=2000\r\n\r\n";
        assertFalse(SipCall.isCompletedElsewhere(msg));
    }

    @Test
    public void nonCancelMessageWithReasonCause200IsNotCompletedElsewhere() {
        // Only the CANCEL request must trigger suppression, not e.g. a BYE.
        String msg = "BYE sip:200@example.com SIP/2.0\r\n" + "Reason: SIP;cause=200\r\n\r\n";
        assertFalse(SipCall.isCompletedElsewhere(msg));
    }

    @Test
    public void nullOrEmptyMessageIsNotCompletedElsewhere() {
        assertFalse(SipCall.isCompletedElsewhere(null));
        assertFalse(SipCall.isCompletedElsewhere(""));
    }
}
