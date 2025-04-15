package marketing.mama.domain.user.dto.request

import jakarta.validation.constraints.NotNull
import marketing.mama.domain.user.model.Role

data class ApprovalRequest(
    @field:NotNull
    val role: Role
)