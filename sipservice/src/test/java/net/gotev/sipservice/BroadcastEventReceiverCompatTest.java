package net.gotev.sipservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Backward-compatibility guarantees for {@link BroadcastEventReceiver#onCallState}.
 * <p>
 * This library is consumed by external code that subclasses {@link BroadcastEventReceiver}
 * and overrides the original 5-argument {@code onCallState} callback. Adding the
 * "completed elsewhere" flag introduced a 6-argument overload; the dispatcher now invokes
 * that overload, so we must guarantee legacy 5-argument overrides keep being notified.
 */
public class BroadcastEventReceiverCompatTest {

    /**
     * A legacy consumer that only knows the original 5-argument callback.
     */
    private static class LegacyReceiver extends BroadcastEventReceiver {
        boolean legacyCalled = false;
        int callStateCode = -1;
        int callStatusCode = -1;
        long connectTimestamp = -1;

        @Override
        public void onCallState(String accountID, int callID, int callStateCode,
                                int callStatusCode, long connectTimestamp) {
            this.legacyCalled = true;
            this.callStateCode = callStateCode;
            this.callStatusCode = callStatusCode;
            this.connectTimestamp = connectTimestamp;
        }
    }

    @Test
    public void sixArgOverloadStillInvokesLegacyFiveArgOverride() {
        LegacyReceiver receiver = new LegacyReceiver();

        // The dispatcher calls the 6-arg overload; a subclass that only overrides the
        // legacy 5-arg callback must still be notified via the default delegation.
        receiver.onCallState("acc", 1, 5, 200, 123L, true);

        assertTrue("legacy 5-arg onCallState must still be invoked", receiver.legacyCalled);
    }

    @Test
    public void sixArgOverloadForwardsLegacyArgumentsUnchanged() {
        LegacyReceiver receiver = new LegacyReceiver();

        receiver.onCallState("acc", 1, 5, 487, 999L, true);

        assertEquals(5, receiver.callStateCode);
        assertEquals(487, receiver.callStatusCode);
        assertEquals(999L, receiver.connectTimestamp);
    }
}
