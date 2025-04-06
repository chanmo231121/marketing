package marketing.mama.domain.user.dto.request

data class ExtendUserRequest(
    val newApprovedUntil: String,
    val autoExtend: Boolean = false
)