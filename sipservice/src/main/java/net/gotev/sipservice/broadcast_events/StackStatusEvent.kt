package net.gotev.sipservice.broadcast_events

data class StackStatusEvent(
    val isStarted: Boolean
) {
    override fun toString(): String {
        return "StackStatusEvent(isStarted=$isStarted)"
    }
}
