package marketing.mama.domain.user.dto.response

import marketing.mama.domain.user.model.Role
import marketing.mama.domain.user.model.Status

data class LoginResponse(
    val name: String,
    val role: Role,
    val stats :Status
)
