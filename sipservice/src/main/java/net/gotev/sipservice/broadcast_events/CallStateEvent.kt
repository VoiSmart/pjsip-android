package net.gotev.sipservice.broadcast_events

data class CallStateEvent(
    val accountID: String,
    val callID: Int,
    val callStateCode: Int,
    val callStatusCode: Int,
    val connectTimestamp: Long
) {
    override fun toString(): String {
        return "CallStateEvent(accountID='$accountID', callID=$callID, callStateCode=$callStateCode, callStatusCode=$callStatusCode, connectTimestamp=$connectTimestamp)"
    }
}