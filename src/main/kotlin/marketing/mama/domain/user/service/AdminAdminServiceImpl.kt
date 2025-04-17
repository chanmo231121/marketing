package marketing.mama.domain.user.service

import jakarta.transaction.Transactional
import marketing.mama.domain.user.dto.response.UserResponse
import marketing.mama.domain.user.model.Role
import marketing.mama.domain.user.model.Status
import marketing.mama.domain.user.model.User
import marketing.mama.domain.user.repository.UserRepository
import marketing.mama.global.exception.ModelNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime


@Service
class AdminAdminServiceImpl(
    private val userRepository: UserRepository
) : AdminAdminService {

    override fun getPendingAdmins(): List<UserResponse> {
        val admins = userRepository.findAllByRoleAndStatus(Role.ADMIN, Status.PENDING_APPROVAL)
        return admins.map { UserResponse.from(it) }
    }



    @Transactional
    override fun approveAdmin(userId: Long, role: Role): String {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw IllegalArgumentException("해당 유저를 찾을 수 없습니다")

        user.status = Status.NORMAL
        user.role = role  // ✅ DEV가 선택한 Role로 변경
        user.approvedAt = user.approvedAt ?: LocalDateTime.now()
        user.lastApprovedAt = LocalDateTime.now()

        userRepository.save(user)
        return "관리자 승인 완료"
    }

    @Transactional
    override fun rejectAdmin(userId: Long, reason: String): String {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다") }

        if (user.role != Role.ADMIN) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "관리자 유저만 거절할 수 있습니다")
        }

        user.role = Role.PRO
        user.status = Status.REJECTED
        user.rejectReason = reason
        userRepository.save(user)
        return "관리자 거절 완료"
    }

    override fun getRejectedAdmins(): List<UserResponse> {
        return userRepository.findAllByRoleAndStatus(Role.ADMIN, Status.REJECTED)
            .map { UserResponse.from(it) }
    }

    @Transactional
    override fun restoreAdmin(userId: Long): String {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다") }

        if (user.role != Role.ADMIN || user.status != Status.REJECTED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "복구 가능한 관리자 유저가 아닙니다.")
        }

        user.status = Status.PENDING_APPROVAL
        user.rejectReason = null
        userRepository.save(user)

        return "관리자 유저 복구 완료"
    }

    override fun getApprovedAdminsAndPros(): List<UserResponse> {
        val users = userRepository.findAllByStatusAndRoleIn(
            Status.NORMAL,
            listOf(Role.ADMIN, Role.PRO)
        )
        return users.map { UserResponse.from(it) }
    }

    override fun getReapprovalPendingAdmins(): List<UserResponse> {
        val users = userRepository.findAllByRoleAndStatus(Role.ADMIN, Status.PENDING_REAPPROVAL)
        return users.map { UserResponse.from(it) }
    }

    @Transactional
    override fun deleteAdmin(userId: Long): String {
        // 관리자 삭제 전 존재 여부 확인 (또는 논리적 삭제를 고려)
        val user: User = userRepository.findById(userId)
            .orElseThrow { RuntimeException("관리자를 찾을 수 없습니다. (ID: $userId)") }
        userRepository.delete(user)
        return "관리자 삭제가 완료되었습니다."
    }

    override fun getAllUsers(): List<UserResponse> {
        val roles = listOf(Role.PRO, Role.ADMIN)
        return userRepository.findByRoleIn(roles)
            .map { UserResponse.from(it) }
    }

    override fun expireAdmin(userId: Long): String {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw ModelNotFoundException("user", userId)

        if (user.role != Role.ADMIN && user.role != Role.DEV) {
            throw IllegalArgumentException("관리자 권한이 아닌 유저는 만료할 수 없습니다.")
        }

        // 관리자 역할에서 PRO로 강등
        user.role = Role.PRO
        user.status = Status.PENDING_REAPPROVAL // 또는 원하는 상태로 설정
        user.approvedUntil = null
        user.deviceId = null // 기존 승인된 기기 초기화

        userRepository.save(user)
        return "관리자 계정이 만료되어 PRO로 강등되었습니다."
    }


}