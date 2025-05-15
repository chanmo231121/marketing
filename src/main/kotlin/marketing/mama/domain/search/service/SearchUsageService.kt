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
    fun incrementSingleSearchWithLimit() {
        val user = getCurrentUser()
        if (user.role != Role.PRO) return

        val limit = user.singleSearchLimit ?: 200
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
    fun incrementRankingSearchWithLimit() {
        val user = getCurrentUser()
        if (user.role != Role.PRO) return

        val limit = user.rankingSearchLimit ?: 50
        val today = LocalDate.now()

        val usage = searchUsageRepository.findByUserIdAndUsageDate(user.id!!, today)
            ?: searchUsageRepository.save(SearchUsage(user = user, usageDate = today))

        if (usage.rankingSearchCount >= limit) {
            throw IllegalStateException("⛔ 랭킹 순위 검색은 하루 최대 ${limit}회까지 가능합니다.")
        }

        usage.rankingSearchCount++
        searchUsageRepository.save(usage)
    }

    @Transactional
    fun incrementShoppingSearchWithLimit() {
        val user = getCurrentUser()

        if (user.role != Role.PRO) return

        val limit = user.shoppingSearchLimit ?: 100
        val today = LocalDate.now()

        val usage = searchUsageRepository.findByUserIdAndUsageDate(user.id!!, today)
            ?: searchUsageRepository.save(SearchUsage(user = user, usageDate = today))

        if (usage.shoppingSearchCount >= limit) {
            throw IllegalStateException("⛔ 쇼핑 검색은 하루 최대 ${limit}회까지 가능합니다.")
        }

        usage.shoppingSearchCount++
        searchUsageRepository.save(usage)

    }

    @Transactional
    fun incrementTrendSearchWithLimit() {
        val user = getCurrentUser()

        // 관리자나 PRO만 사용 가능하도록 제한할 경우
        if (user.role != Role.PRO && user.role != Role.ADMIN) return

        val limit = user.trendSearchLimit ?: 100
        val today = LocalDate.now()

        val usage = searchUsageRepository.findByUserIdAndUsageDate(user.id!!, today)
            ?: searchUsageRepository.save(SearchUsage(user = user, usageDate = today))

        if (usage.trendSearchCount >= limit) {
            throw IllegalStateException("⛔ 트렌드 검색은 하루 최대 ${limit}회까지 가능합니다.")
        }

        usage.trendSearchCount++
        searchUsageRepository.save(usage)
    }


    fun getUserSearchUsageInfo(userId: Long): SearchUsageInfoResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw IllegalArgumentException("유저를 찾을 수 없습니다.")

        val today = LocalDate.now()
        val usage = searchUsageRepository.findByUserIdAndUsageDate(userId, today)
            ?: SearchUsage(
                user = user,
                usageDate = today,
                singleSearchCount = 0,
                rankingSearchCount = 0,
                shoppingSearchCount = 0,
                trendSearchCount = 0 // ✅ 반드시 추가
            )

        val isPro = user.role == Role.PRO
        val singleLimit = if (isPro) user.singleSearchLimit ?: 200 else Int.MAX_VALUE
        val rankingLimit = if (isPro) user.rankingSearchLimit ?: 50 else Int.MAX_VALUE
        val shoppingLimit = if (isPro) user.shoppingSearchLimit ?: 100 else Int.MAX_VALUE
        val trendLimit = if (isPro) user.trendSearchLimit  ?: 100 else Int.MAX_VALUE

        return SearchUsageInfoResponse(
            singleSearchLimit = singleLimit,
            singleSearchUsed = usage.singleSearchCount,
            rankingSearchLimit = rankingLimit,
            rankingSearchUsed = usage.rankingSearchCount,
            shoppingSearchLimit = shoppingLimit,                   // ✅ 추가
            shoppingSearchUsed = usage.shoppingSearchCount,         // ✅ 추가
            trendSearchLimit = trendLimit,
            trendSearchUsed = usage.trendSearchCount,

            canUseSingleSearch = usage.singleSearchCount < singleLimit,
            canUseRankingSearch = usage.rankingSearchCount < rankingLimit,
            canUseShoppingSearch = usage.shoppingSearchCount < shoppingLimit,
            canUseTrendSearch = usage.trendSearchCount < trendLimit
        )
    }

    fun getUsageInfo(user: User): SearchUsageInfoResponse {
        val today = LocalDate.now()
        val usage = searchUsageRepository.findByUserIdAndUsageDate(user.id!!, today)

        val singleUsed = usage?.singleSearchCount ?: 0
        val rankingUsed = usage?.rankingSearchCount ?: 0
        val shoppingUsed = usage?.shoppingSearchCount ?: 0  // ✅ 쇼핑 사용량 추가
        val trendUsed = usage?.trendSearchCount  ?: 0

        val isPro = user.role == Role.PRO
        val singleLimit = if (isPro) user.singleSearchLimit ?: 200 else Int.MAX_VALUE
        val rankingLimit = if (isPro) user.rankingSearchLimit ?: 50 else Int.MAX_VALUE
        val shoppingLimit = if (isPro) user.shoppingSearchLimit ?: 100 else Int.MAX_VALUE  // ✅ 쇼핑 한도 추가
        val trendLimit = if (isPro) user.trendSearchLimit  ?: 100 else Int.MAX_VALUE

        return SearchUsageInfoResponse(
            singleSearchLimit = singleLimit,
            singleSearchUsed = singleUsed,
            rankingSearchLimit = rankingLimit,
            rankingSearchUsed = rankingUsed,
            shoppingSearchLimit = shoppingLimit,     // ✅ 추가
            shoppingSearchUsed = shoppingUsed,       // ✅ 추가
            trendSearchLimit = trendLimit,
            trendSearchUsed = trendUsed,

            canUseSingleSearch = singleUsed < singleLimit,
            canUseRankingSearch = rankingUsed < rankingLimit,
            canUseShoppingSearch = shoppingUsed < shoppingLimit,
            canUseTrendSearch = trendUsed < trendLimit
// ✅ 추가
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
            "shopping" -> usage.shoppingSearchCount = 0
            "trend" -> usage.trendSearchCount = 0// ✅ 쇼핑 검색 카운트도 초기화 추가
            else -> throw IllegalArgumentException("잘못된 타입: $type")
        }

        searchUsageRepository.save(usage)
    }


}
