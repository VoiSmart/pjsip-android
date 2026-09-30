package net.gotev.sipservice;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.pjsip.pjsua2.CallInfo;

public class CallerInfoTest {

    private static final class FakeCallInfo extends CallInfo {
        private final String remoteUri;

        FakeCallInfo(String remoteUri) {
            super(0, false);
            this.remoteUri = remoteUri;
        }

        @Override
        public String getRemoteUri() {
            return remoteUri;
        }
    }

    private static CallerInfo callerInfo(String remoteUri) {
        return new CallerInfo(new FakeCallInfo(remoteUri));
    }

    @Test
    public void parsesSipUriWithDisplayName() {
        CallerInfo info = callerInfo("\"Alice\" <sip:1001@pbx.example.com>");

        assertEquals("Alice", info.getDisplayName());
        assertEquals("1001@pbx.example.com", info.getRemoteUri());
    }

    @Test
    public void parsesSipsUriWithDisplayName() {
        CallerInfo info = callerInfo("\"Alice\" <sips:1001@pbx.example.com>");

        assertEquals("Alice", info.getDisplayName());
        assertEquals("1001@pbx.example.com", info.getRemoteUri());
    }

    @Test
    public void parsesSipUriWithoutDisplayName() {
        CallerInfo info = callerInfo("<sip:1001@pbx.example.com>");

        assertEquals("1001@pbx.example.com", info.getDisplayName());
        assertEquals("1001@pbx.example.com", info.getRemoteUri());
    }

    @Test
    public void parsesSipsUriWithoutDisplayName() {
        CallerInfo info = callerInfo("<sips:1001@pbx.example.com>");

        assertEquals("1001@pbx.example.com", info.getDisplayName());
        assertEquals("1001@pbx.example.com", info.getRemoteUri());
    }
}
