package net.gotev.sipservice.broadcast_events

data class CodecPrioritiesSetStatusEvent(
    val isSuccess: Boolean
) {
    override fun toString(): String {
        return "CodecPrioritiesSetStatusEvent(isSuccess=$isSuccess)"
    }
}
