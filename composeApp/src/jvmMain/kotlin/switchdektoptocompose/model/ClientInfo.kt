package switchdektoptocompose.model

data class ClientInfo(
    val id: String,
    val name: String,
    val isTrusted: Boolean = false,
    val verificationCode: String? = null
)
