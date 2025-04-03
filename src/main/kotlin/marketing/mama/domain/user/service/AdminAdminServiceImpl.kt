package marketing.mama.domain.user.service

import jakarta.transaction.Transactional
import marketing.mama.domain.user.dto.response.UserResponse
import marketing.mama.domain.user.model.Role
import marketing.mama.domain.user.model.Status
import marketing.mama.domain.user.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class AdminAdminServiceImpl(
    private val userRepository: UserRepository
) : AdminAdminService {

    override fun getPendingAdmins(): List<UserResponse> {
        val admins = userRepository.findAllByRoleAndStatus(Role.관리자, Status.PENDING_APPROVAL)
        return admins.map { UserResponse.from(it) }
    }
    @Transactional
    override fun approveAdmin(userId: Long): String {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다") }

        if (user.role != Role.관리자) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "관리자 유저만 승인할 수 있습니다")
        }

        user.status = Status.NORMAL
        userRepository.save(user)
        return "관리자 승인 완료"
    }

    @Transactional
    override fun rejectAdmin(userId: Long, reason: String): String {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다") }

        if (user.role != Role.관리자) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "관리자 유저만 거절할 수 있습니다")
        }

        user.status = Status.REJECTED
        user.rejectReason = reason
        userRepository.save(user)
        return "관리자 거절 완료"
    }

    override fun getRejectedAdmins(): List<UserResponse> {
        return userRepository.findAllByRoleAndStatus(Role.관리자, Status.REJECTED)
            .map { UserResponse.from(it) }
    }

    @Transactional
    override fun restoreAdmin(userId: Long): String {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다") }

        if (user.role != Role.관리자 || user.status != Status.REJECTED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "복구 가능한 관리자 유저가 아닙니다.")
        }

        user.status = Status.PENDING_APPROVAL
        user.rejectReason = null
        userRepository.save(user)

        return "관리자 유저 복구 완료"
    }
}