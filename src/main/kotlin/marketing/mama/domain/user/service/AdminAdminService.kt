package marketing.mama.domain.user.service

import marketing.mama.domain.user.dto.response.UserResponse
import marketing.mama.domain.user.model.Role

interface AdminAdminService {
    fun getPendingAdmins(): List<UserResponse>
    fun rejectAdmin(userId: Long, reason: String): String
    fun getRejectedAdmins(): List<UserResponse>
    fun restoreAdmin(userId: Long): String
    fun getApprovedAdminsAndPros(): List<UserResponse>
    fun getReapprovalPendingAdmins(): List<UserResponse>
    fun approveAdmin(userId: Long, role: Role): String
    fun deleteAdmin(userId: Long): String
    fun getAllUsers(): List<UserResponse>
    fun expireAdmin(userId: Long): String


}
