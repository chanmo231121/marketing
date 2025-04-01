package marketing.mama.domain.user.service

import marketing.mama.domain.user.dto.response.UserResponse

interface AdminUserService {
    fun getPendingPros(): List<UserResponse>
    fun approvePro(userId: Long): String
}