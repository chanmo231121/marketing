package marketing.mama.domain.user.dto.request

import jakarta.validation.constraints.NotNull
import marketing.mama.domain.user.model.Role
import java.time.LocalDateTime

data class ApprovalRequest(
    val role: Role, // ✅ enum으로 직접 받기
    val approvedUntil: LocalDateTime? = null,
    val autoExtend: Boolean? = null
)