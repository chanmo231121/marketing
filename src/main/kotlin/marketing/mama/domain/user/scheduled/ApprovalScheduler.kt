package marketing.mama.domain.user.scheduled

import marketing.mama.domain.user.model.Role
import marketing.mama.domain.user.model.Status
import marketing.mama.domain.user.repository.UserRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ApprovalScheduler(
    private val userRepository: UserRepository
) {

    // ✅ 매일 자정에 실행
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정
    fun expireOldApprovals() {
        val now = LocalDateTime.now()

        // 🔹 자동 연장 OFF → 만료된 유저는 재승인 필요
        val expiredUsers = userRepository.findByStatusAndRole(Status.NORMAL, Role.PRO)
            .filter { it.approvedUntil?.isBefore(now) == true && !it.autoExtend }

        expiredUsers.forEach {
            it.status = Status.PENDING_APPROVAL
        }
        userRepository.saveAll(expiredUsers)
        println("✅ [스케줄러] 수동 재승인 대상: ${expiredUsers.size}")

        // 🔹 자동 연장 대상 유저 → +7일 연장
        val autoExtendUsers = userRepository.findByStatusAndRole(Status.NORMAL, Role.PRO)
            .filter { it.autoExtend && it.approvedUntil?.isBefore(now) == true }

        autoExtendUsers.forEach {
            it.lastApprovedAt = now
            it.approvedUntil = now.plusDays(7) // ✅ 변경됨: 7일 연장
        }

        userRepository.saveAll(autoExtendUsers)
        println("✅ [스케줄러] 자동 연장된 유저 수: ${autoExtendUsers.size}")
    }
}
