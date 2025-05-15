package marketing.mama.domain.user.dto.response

import com.fasterxml.jackson.annotation.JsonFormat
import marketing.mama.domain.user.model.Role
import marketing.mama.domain.user.model.Status
import java.time.LocalDateTime

data class LoginResponse(
    val id: Long,
    val name: String,
    val role: Role,
    val status :Status,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val approvedUntil: LocalDateTime?,  // 만료일 필드 추가 (필요시 타입을 LocalDateTime 등으로 변경 가능)
    val canUseSingleSearch: Boolean,
    val canUseRankingSearch: Boolean,
    val canUseKeywordMix: Boolean,
    val canUseRelatedSearch: Boolean,
    val canUseShoppingSearch: Boolean,
    val canUseTrendSearch: Boolean

)
