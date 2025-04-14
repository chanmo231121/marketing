package marketing.mama.domain.search.service

import jakarta.transaction.Transactional
import marketing.mama.domain.search.dto.SearchUsageInfoResponse
import marketing.mama.domain.search.model.SearchUsage
import marketing.mama.domain.search.repository.SearchUsageRepository
import marketing.mama.domain.user.model.Role
import marketing.mama.domain.user.model.User
import marketing.mama.domain.user.repository.UserRepository
import marketing.mama.infra.security.UserPrincipal
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SearchUsageService(
    private val searchUsageRepository: SearchUsageRepository,
    private val userRepository: UserRepository
) {
    private fun getCurrentUser(): User {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        val userPrincipal = principal as? UserPrincipal
            ?: throw RuntimeException("인증된 사용자 정보를 찾을 수 없습니다.")

        return userRepository.findById(userPrincipal.id)
            .orElseThrow { RuntimeException("사용자를 찾을 수 없습니다: ${userPrincipal.id}") }
    }

    @Transactional
    fun incrementSingleSearchWithLimit(limit: Int = 200) {
        val user = getCurrentUser()

        // ✅ 관리자, 개발자는 제한 없이 통과
        if (user.role != Role.PRO) return

        val today = LocalDate.now()

        val usage = searchUsageRepository.findByUserIdAndUsageDate(user.id!!, today)
            ?: searchUsageRepository.save(SearchUsage(user = user, usageDate = today))

        if (usage.singleSearchCount >= limit) {
            throw IllegalStateException("⛔ 단일 검색은 하루 최대 ${limit}회까지 가능합니다.")
        }

        usage.singleSearchCount++
        searchUsageRepository.save(usage)
    }

    @Transactional
    fun incrementRankingSearchWithLimit(limit: Int = 50) {
        val user = getCurrentUser()

        // ✅ 관리자, 개발자는 제한 없이 통과
        if (user.role != Role.PRO) return

        val today = LocalDate.now()

        val usage = searchUsageRepository.findByUserIdAndUsageDate(user.id!!, today)
            ?: searchUsageRepository.save(SearchUsage(user = user, usageDate = today))

        if (usage.rankingSearchCount >= limit) {
            throw IllegalStateException("⛔ 랭킹 순위 검색은 하루 최대 ${limit}회까지 가능합니다.")
        }

        usage.rankingSearchCount++
        searchUsageRepository.save(usage)
    }

    fun getUserSearchUsageInfo(userId: Long): SearchUsageInfoResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw IllegalArgumentException("유저를 찾을 수 없습니다.")

        val today = LocalDate.now()
        val usage = searchUsageRepository.findByUserIdAndUsageDate(userId, today)
            ?: SearchUsage(user = user, usageDate = today, singleSearchCount = 0, rankingSearchCount = 0)

        val isPro = user.role == Role.PRO

        return SearchUsageInfoResponse(
            singleSearchLimit = if (isPro) 200 else Int.MAX_VALUE,
            singleSearchUsed = usage.singleSearchCount,
            rankingSearchLimit = if (isPro) 50 else Int.MAX_VALUE,
            rankingSearchUsed = usage.rankingSearchCount,
            canUseSingleSearch = isPro,
            canUseRankingSearch = isPro
        )
    }

    fun getUsageInfo(user: User): SearchUsageInfoResponse {
        val today = LocalDate.now()

        val usage = searchUsageRepository.findByUserIdAndUsageDate(user.id!!, today)
        val singleUsed = usage?.singleSearchCount ?: 0
        val rankingUsed = usage?.rankingSearchCount ?: 0

        val singleLimit = if (user.role == Role.PRO) 200 else Int.MAX_VALUE
        val rankingLimit = if (user.role == Role.PRO) 50 else Int.MAX_VALUE

        return SearchUsageInfoResponse(
            singleSearchLimit = singleLimit,
            singleSearchUsed = singleUsed,
            rankingSearchLimit = rankingLimit,
            rankingSearchUsed = rankingUsed,
            canUseSingleSearch = singleUsed < singleLimit,
            canUseRankingSearch = rankingUsed < rankingLimit
        )
    }
    @Transactional
    fun resetTodayUsage(userId: Long, type: String) {
        val today = LocalDate.now()
        val user = userRepository.findById(userId).orElseThrow()

        val usage = searchUsageRepository.findByUserIdAndUsageDate(user.id!!, today)
            ?: searchUsageRepository.save(SearchUsage(user = user, usageDate = today))

        when (type) {
            "single" -> usage.singleSearchCount = 0
            "ranking" -> usage.rankingSearchCount = 0
            else -> throw IllegalArgumentException("잘못된 타입: $type")
        }

        searchUsageRepository.save(usage)
    }

}
