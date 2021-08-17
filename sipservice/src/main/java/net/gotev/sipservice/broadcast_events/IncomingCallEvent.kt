package net.gotev.sipservice.broadcast_events

data class IncomingCallEvent(
    val accountID: String,
    val callID: Int,
    val displayName: String, val remoteUri: String,
    val isVideo: Boolean = false
) {
    override fun toString(): String {
        return "IncomingCallEvent(accountID='$accountID', callID=$callID, displayName='$displayName', remoteUri='$remoteUri', isVideo=$isVideo)"
    }
}
