package marketing.mama.domain.user.service

import jakarta.transaction.Transactional
import marketing.mama.domain.user.dto.request.ExtendUserRequest
import marketing.mama.domain.user.dto.response.UserResponse
import marketing.mama.domain.user.model.Role
import marketing.mama.domain.user.model.Status
import marketing.mama.domain.user.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class AdminUserServiceImpl(
    private val userRepository: UserRepository
) : AdminUserService {

    override fun getPendingPros(): List<UserResponse> {
        val pendingStatuses = listOf(Status.PENDING_APPROVAL)
        val pendingPros = userRepository.findAllByRoleAndStatusIn(Role.프로, pendingStatuses)
        return pendingPros.map { UserResponse.from(it) }
    }

    override fun getReapprovalPendingPros(): List<UserResponse> {
        val users = userRepository.findAllByRoleAndStatus(Role.프로, Status.PENDING_REAPPROVAL)
        return users.map { UserResponse.from(it) }
    }

    @Transactional
    override fun approvePro(userId: Long): String {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw IllegalArgumentException("해당 유저를 찾을 수 없습니다")

        user.status = Status.NORMAL
        user.approvedAt = user.approvedAt ?: LocalDateTime.now()
        user.lastApprovedAt = LocalDateTime.now()

        // ✅ approvedUntil도 같이 설정 (현재 시점 기준 7일 후)
        user.approvedUntil = user.lastApprovedAt?.plusDays(7)

        userRepository.save(user)
        return "승인되었습니다."
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

    override fun getApprovedProUsers(): List<UserResponse> {
        val users = userRepository.findAllByStatusAndRole(Status.NORMAL, Role.프로)
        return users.map { UserResponse.from(it) }
    }

    override fun extendApproval(userId: Long, request: ExtendUserRequest) {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("유저를 찾을 수 없습니다.") }

        user.status = Status.NORMAL

        if (request.autoExtend) {
            // 자동 연장 ON: approvedUntil을 현재 기준 +7일로 자동 설정
            user.lastApprovedAt = LocalDateTime.now()
            user.approvedUntil = user.lastApprovedAt?.plusDays(7)
        } else {
            // 수동 연장: 프론트에서 날짜를 지정해온 것 적용
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val newDate = LocalDate.parse(request.newApprovedUntil, formatter)

            user.lastApprovedAt = newDate.atStartOfDay()
            user.approvedUntil = newDate.atStartOfDay()
        }

        user.autoExtend = request.autoExtend
        userRepository.save(user)
    }

}