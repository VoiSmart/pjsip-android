package net.gotev.sipservice.broadcast_events

import net.gotev.sipservice.CodecPriority
import java.util.ArrayList

data class CodecPrioritiesEvent(
    val codecPriorities: ArrayList<CodecPriority>
) {
    override fun toString(): String {
        return "CodecPriorityEvent(codecPriorities=$codecPriorities)"
    }
}
