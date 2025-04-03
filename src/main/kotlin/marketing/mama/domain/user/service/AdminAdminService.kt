package marketing.mama.domain.user.service

import marketing.mama.domain.user.dto.response.UserResponse

interface AdminAdminService {
    fun getPendingAdmins(): List<UserResponse>
    fun approveAdmin(userId: Long): String
    fun rejectAdmin(userId: Long, reason: String): String
    fun getRejectedAdmins(): List<UserResponse>
    fun restoreAdmin(userId: Long): String}