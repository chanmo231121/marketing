package marketing.mama.domain.user.service

import marketing.mama.domain.user.dto.response.UserResponse

interface AdminUserService {
    fun getPendingPros(): List<UserResponse>
    fun approvePro(userId: Long): String
    fun findRejectedUsers(): List<UserResponse>
    fun deleteUser(userId: Long)
    fun rejectPro(userId: Long, reason: String): String
    fun restorePro(userId: Long): String
}