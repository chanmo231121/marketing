package marketing.mama.domain.user.service


import marketing.mama.domain.user.dto.request.ExtendUserRequest
import marketing.mama.domain.user.dto.response.UserResponse
import marketing.mama.domain.user.model.Role

interface AdminUserService {
    fun getPendingPros(): List<UserResponse>
    fun findRejectedUsers(): List<UserResponse>
    fun deleteUser(userId: Long)
    fun rejectPro(userId: Long, reason: String): String
    fun restorePro(userId: Long): String
    fun getApprovedProUsers(): List<UserResponse>
    fun getReapprovalPendingPros(): List<UserResponse>
    fun extendApproval(userId: Long, request: ExtendUserRequest)
    fun approvePro(userId: Long, role: Role): String

}