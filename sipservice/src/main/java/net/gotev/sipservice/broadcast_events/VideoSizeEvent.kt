package net.gotev.sipservice.broadcast_events

data class VideoSizeEvent(
    val width: Int,
    val height: Int
) {
    override fun toString(): String {
        return "VideoSizeEvent(width=$width, height=$height)"
    }
}
