package marketing.mama.domain.user.service

import jakarta.transaction.Transactional
import marketing.mama.domain.user.dto.response.UserResponse
import marketing.mama.domain.user.model.Role
import marketing.mama.domain.user.model.Status
import marketing.mama.domain.user.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@Service
class AdminUserServiceImpl(
    private val userRepository: UserRepository
) : AdminUserService {

    override fun getReapprovalPendingPros(): List<UserResponse> {
        val users = userRepository.findAllByRoleAndStatus(Role.PRO, Status.PENDING_REAPPROVAL)
        return users.map { UserResponse.from(it) }
    }

    override fun getPendingPros(): List<UserResponse> {
        val pendingUsers = userRepository.findAllByStatusAndRole(Status.PENDING_APPROVAL, Role.PRO)
        return pendingUsers.map { user -> UserResponse.from(user) }
    }

    @Transactional
    override fun approvePro(userId: Long, role: Role): String {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw IllegalArgumentException("해당 유저를 찾을 수 없습니다")

        // 관리자가 요청한 role로 변경 (예: ADMIN, PRO)
        user.role = role

        // 승인 상태로 변경
        user.status = Status.NORMAL
        user.approvedAt = user.approvedAt ?: LocalDateTime.now()
        user.lastApprovedAt = LocalDateTime.now()
        // approvedUntil을 현재 시점 기준 7일 후로 설정
        // 프로 유저로 승인된 경우에만 만료일 설정 (7일 후)

        if (role == Role.PRO) {
            user.approvedUntil = user.lastApprovedAt?.plusDays(7)  // 프로 유저만 만료일 설정
        } else {
            user.approvedUntil = null  // 관리자일 경우 만료일은 필요 없으므로 null
        }
        userRepository.save(user)
        return "승인되었습니다. 부여된 역할: ${role.name}"
    }

    override fun findRejectedUsers(): List<UserResponse> {
        return userRepository.findAllByRoleAndStatus(Role.PRO, Status.REJECTED)
            .map { UserResponse.from(it) }
    }

    override fun deleteUser(userId: Long) {
        if (!userRepository.existsById(userId)) throw RuntimeException("존재하지 않는 사용자입니다.")
        userRepository.deleteById(userId)
    }

    @Transactional
    override fun rejectPro(userId: Long, reason: String): String {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다") }

        if (user.role != Role.PRO) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "프로 유저만 거절할 수 있습니다")
        }
        user.status = Status.REJECTED
        user.rejectReason = reason
        userRepository.save(user)

        return "프로 거절 완료"
    }

    @Transactional
    override fun restorePro(userId: Long): String {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다") }

        if (user.role != Role.PRO || user.status != Status.REJECTED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "복구 가능한 프로 유저가 아닙니다.")
        }

        user.status = Status.PENDING_APPROVAL
        user.rejectReason = null
        userRepository.save(user)

        return "프로 유저 복구 완료"
    }

    override fun getApprovedProUsers(): List<UserResponse> {
        val users = userRepository.findAllByStatusAndRole(Status.NORMAL, Role.PRO)
        return users.map { UserResponse.from(it) }
    }

    override fun extendApproval(userId: Long, request: marketing.mama.domain.user.dto.request.ExtendUserRequest) {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("유저를 찾을 수 없습니다.") }

        user.status = Status.NORMAL

        if (request.autoExtend) {
            user.lastApprovedAt = LocalDateTime.now()
            user.approvedUntil = user.lastApprovedAt?.plusDays(7)
        } else {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val newDate = java.time.LocalDate.parse(request.newApprovedUntil, formatter)
            user.lastApprovedAt = newDate.atStartOfDay()
            user.approvedUntil = newDate.atStartOfDay()
        }

        user.autoExtend = request.autoExtend
        userRepository.save(user)
    }

    // 기타 관리자 관련 메소드들은 그대로 유지합니다.
}
