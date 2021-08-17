package net.gotev.sipservice.broadcast_events

data class OutgoingCallEvent(
    val accountID: String,
    val callID: Int,
    val number: String,
    val isVideo: Boolean = false,
    val isVideoConference: Boolean = false,
    val isTransfer: Boolean = false
) {
    override fun toString(): String {
        return "OutgoingCallEvent(accountID='$accountID', callID=$callID, number='$number', isVideo=$isVideo, isVideoConference=$isVideoConference, isTransfer=$isTransfer)"
    }
}
