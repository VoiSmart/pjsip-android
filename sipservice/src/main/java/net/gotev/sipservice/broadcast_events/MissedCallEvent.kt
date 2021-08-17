package net.gotev.sipservice.broadcast_events

data class MissedCallEvent(
    val displayName: String,
    val uri: String
) {
    override fun toString(): String {
        return "MissedCallEvent(displayName='$displayName', uri='$uri')"
    }
}