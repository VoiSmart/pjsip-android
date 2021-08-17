package net.gotev.sipservice.broadcast_events

import net.gotev.sipservice.MediaState

data class CallMediaStateEvent(
    val accountID: String,
    val callID: Int,
    val stateType: MediaState,
    val stateValue: Boolean
) {
    override fun toString(): String {
        return "MediaStateEvent(accountID='$accountID', callID=$callID, stateType=$stateType, stateValue=$stateValue)"
    }
}
