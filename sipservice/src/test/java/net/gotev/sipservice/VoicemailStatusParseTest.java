package net.gotev.sipservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for {@link VoicemailStatus#parse(String)}, the RFC 3842
 * {@code application/simple-message-summary} MWI body parser.
 */
public class VoicemailStatusParseTest {

    private static String notify(String messagesWaiting, String voiceMessageLine) {
        StringBuilder sb =
                new StringBuilder().append("NOTIFY sip:208@example SIP/2.0\r\n").append("Event: " +
                        "message-summary\r\n").append("Content-Type: application/simple-message" +
                        "-summary\r\n").append("Content-Length: 85\r\n").append("Messages-Waiting" +
                        ": ").append(messagesWaiting).append("\r\n").append("Message-Account: " +
                        "sip:jaime@example.com\r\n");
        if (voiceMessageLine != null) {
            sb.append(voiceMessageLine).append("\r\n");
        }
        return sb.append("\r\n").toString();
    }

    @Test
    public void newOldOnlyReadsCountsAndFlagsWaiting() {
        VoicemailStatus status = VoicemailStatus.parse(notify("yes", "Voice-Message: 2/0"));
        assertTrue(status.getMessagesWaiting());
        assertEquals(2, status.getNewMessages());
        assertEquals(0, status.getOldMessages());
        assertEquals(0, status.getUrgentNew());
        assertEquals(0, status.getUrgentOld());
        assertTrue(status.hasNewMessages());
    }

    @Test
    public void urgentPairReadsAllFourCounts() {
        VoicemailStatus status = VoicemailStatus.parse(notify("yes", "Voice-Message: 2/8 (1/2)"));
        assertEquals(2, status.getNewMessages());
        assertEquals(8, status.getOldMessages());
        assertEquals(1, status.getUrgentNew());
        assertEquals(2, status.getUrgentOld());
    }

    // The key RFC case: waiting=yes with no per-class line must still surface as "waiting".
    @Test
    public void waitingYesWithoutVoiceMessageStillReportsWaiting() {
        VoicemailStatus status = VoicemailStatus.parse(notify("yes", null));
        assertTrue(status.getMessagesWaiting());
        assertEquals(0, status.getNewMessages());
        assertTrue(status.hasNewMessages());
    }

    @Test
    public void waitingNoIsNotWaiting() {
        VoicemailStatus status = VoicemailStatus.parse(notify("no", null));
        assertFalse(status.getMessagesWaiting());
        assertFalse(status.hasNewMessages());
    }

    // A body of a different content type must be ignored entirely.
    @Test
    public void nonSimpleMessageSummaryBodyIsAllClear() {
        String other = "NOTIFY sip:208@example SIP/2.0\r\n" + "Event: dialog\r\n" + "Content-Type" +
                ": application/dialog-info+xml\r\n" + "Messages-Waiting: yes\r\n" + "Voice" +
                "-Message: 3/1\r\n\r\n";
        VoicemailStatus status = VoicemailStatus.parse(other);
        assertFalse(status.getMessagesWaiting());
        assertEquals(0, status.getNewMessages());
        assertFalse(status.hasNewMessages());
    }

    @Test
    public void nullOrEmptyIsAllClear() {
        assertFalse(VoicemailStatus.parse(null).hasNewMessages());
        assertFalse(VoicemailStatus.parse("").hasNewMessages());
    }
}
