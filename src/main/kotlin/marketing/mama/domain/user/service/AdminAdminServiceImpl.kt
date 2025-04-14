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
class AdminAdminServiceImpl(
    private val userRepository: UserRepository
) : AdminAdminService {

    override fun getPendingAdmins(): List<UserResponse> {
        val admins = userRepository.findAllByRoleAndStatus(Role.ADMIN, Status.WAITING)
        return admins.map { UserResponse.from(it) }
    }

    @Transactional
    override fun approveAdmin(userId: Long): String {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw IllegalArgumentException("해당 유저를 찾을 수 없습니다")

        user.status = Status.NORMAL
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


}