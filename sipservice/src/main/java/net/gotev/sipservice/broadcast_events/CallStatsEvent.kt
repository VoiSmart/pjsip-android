package net.gotev.sipservice.broadcast_events

import net.gotev.sipservice.RtpStreamStats

data class CallStatsEvent(
    val callID: Int, val duration: Int, val audioCodec: String, val callStatusCode: Int, val rx: RtpStreamStats, val tx: RtpStreamStats
) {
    override fun toString(): String {
        return "CallStatsEvent(callID=$callID, duration=$duration, audioCodec='$audioCodec', callStatusCode=$callStatusCode, rx=$rx, tx=$tx)"
    }
}
