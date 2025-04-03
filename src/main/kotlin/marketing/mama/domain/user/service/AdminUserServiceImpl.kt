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
class AdminUserServiceImpl(
    private val userRepository: UserRepository
) : AdminUserService {

    override fun getPendingPros(): List<UserResponse> {
        val pendingPros = userRepository.findAllByRoleAndStatus(Role.프로, Status.PENDING_APPROVAL)
        return pendingPros.map { UserResponse.from(it) }
    }

    @Transactional
    override fun approvePro(userId: Long): String {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다") }

        if (user.role != Role.프로) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "프로 유저만 승인할 수 있습니다")
        }

        user.status = Status.NORMAL
        userRepository.save(user)

        return "프로 승인 완료"
    }

    override fun findRejectedUsers(): List<UserResponse> {
        return userRepository.findAllByRoleAndStatus(Role.프로, Status.REJECTED)
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

        if (user.role != Role.프로) {
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

        if (user.role != Role.프로 || user.status != Status.REJECTED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "복구 가능한 프로 유저가 아닙니다.")
        }

        user.status = Status.PENDING_APPROVAL
        user.rejectReason = null
        userRepository.save(user)

        return "프로 유저 복구 완료"
    }
}