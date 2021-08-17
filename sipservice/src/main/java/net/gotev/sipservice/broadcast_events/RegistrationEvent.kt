package net.gotev.sipservice.broadcast_events

data class RegistrationEvent(
    val accountID: String,
    val registrationStateCode: Int
) {
    override fun toString(): String {
        return "RegistrationEvent(accountID='$accountID', registrationStateCode=$registrationStateCode)"
    }
}
