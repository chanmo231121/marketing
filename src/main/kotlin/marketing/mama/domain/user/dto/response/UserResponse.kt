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
    var createdAt: String?,// ZonedDateTime 대신 String
    val rejectReason: String? = null
) {
    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        fun from(user: User) = UserResponse(
            id = user.id!!,
            name = user.name,
            email = user.email,
            introduction = user.introduction,
            tlno = user.tlno,
            role = user.role.name,
            nickname = user.name,
            createdAt = user.createdAt.format(formatter) , // String 포맷으로 반환
            rejectReason = user.rejectReason
        )
    }
}
