package marketing.mama.domain.user.dto.response

import marketing.mama.domain.user.model.User
import java.time.format.DateTimeFormatter

data class UserResponse(
    var id: Long,
    var name: String,
    var nickname: String,
    var email: String,
    var introduction: String,
    var tlno: String,
    var role: String,
    var createdAt: String?, // ✅ 날짜만 표시
    val rejectReason: String? = null,
    val ipAddress: String?,
    val status: String,
    val approvedUntil: String?, // ← approvedUntilStr → approvedUntil 로 이름 바꿔도 무방
    val autoExtend: Boolean,
    val deviceId: String? = null,
    val canUseSingleSearch: Boolean,
    val canUseRankingSearch: Boolean,
    val canUseKeywordMix: Boolean,
    val canUseRelatedSearch: Boolean,
    val canUseShoppingSearch: Boolean,
) {
    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        fun from(user: User): UserResponse {
            val approvedUntilStr = user.approvedUntil?.format(dateFormatter)

            return UserResponse(
                id = user.id!!,
                name = user.name,
                nickname = user.name,
                email = user.email,
                introduction = user.introduction,
                tlno = user.tlno,
                role = user.role.name,
                createdAt = user.createdAt.format(dateFormatter),
                rejectReason = user.rejectReason,
                ipAddress = user.ipAddress,
                status = user.status.name,
                approvedUntil = approvedUntilStr,
                autoExtend = user.autoExtend,
                deviceId = user.deviceId,
                canUseSingleSearch = user.canUseSingleSearch,
                canUseRankingSearch = user.canUseRankingSearch,
                canUseKeywordMix = user.canUseKeywordMix,
                canUseRelatedSearch = user.canUseRelatedSearch,
                canUseShoppingSearch = user.canUseShoppingSearch,
            )
        }
    }
}
