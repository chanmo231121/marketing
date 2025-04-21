package marketing.mama.domain.keywordRanking.controller

import io.swagger.v3.oas.annotations.Operation
import marketing.mama.domain.activitylog.model.ActionType
import marketing.mama.domain.activitylog.service.SearchLogService
import marketing.mama.domain.keywordRanking.service.KeywordRankingService
import marketing.mama.domain.user.model.Status
import marketing.mama.domain.user.repository.UserRepository
import marketing.mama.domain.user.service.UserService
import marketing.mama.infra.security.UserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/naver-ads")
@PreAuthorize("isAuthenticated()")
class KeywordRankingController(
    private val keywordRankingService: KeywordRankingService,
    private val userRepository: UserRepository,
    private val searchLogService: SearchLogService,
    private val userService: UserService
) {

    @Operation(summary = "네이버 검색광고 입찰순위 (POST)")
    @PostMapping("/search")
    fun searchNaverAds(
        @RequestBody body: Map<String, String>,
        @RequestHeader("X-Device-Id") deviceId: String?,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<Any> {
        return try {
            val rawKeywords = body["keywords"] ?: ""
            val keywordList = rawKeywords.split("\n", ",").map { it.trim() }.filter { it.isNotEmpty() }

            val user = userRepository.findById(userPrincipal.id).orElseThrow()
            userService.validateDevice(user, deviceId)

            if (user.role.name != "ADMIN") {
                when (user.status) {
                    Status.PENDING_APPROVAL, Status.WAITING ->
                        return ResponseEntity.ok(mapOf("approvalMessage" to "⛔ 오른쪽 상단에 있는 승인요청을 해주세요!"))
                    Status.PENDING_REAPPROVAL ->
                        return ResponseEntity.ok(mapOf("approvalMessage" to "⛔ 기간만료! 재승인을 해주세요."))
                    else -> {
                        if (!user.canUseRankingSearch) {
                            return ResponseEntity.ok(mapOf("approvalMessage" to "⛔ 랭킹검색 기능 사용이 제한된 계정입니다. 관리자에게 문의해주세요."))
                        }
                    }
                }
            }

            searchLogService.logSearch(
                user = user,
                userName = user.name,
                ip = user.ipAddress,
                keyword = keywordList.joinToString(", ").take(255), // ← 여기!
                type = ActionType.랭킹검색,
                uuid = user.deviceId
            )

            val results = keywordRankingService.getNaverAdData(keywordList)
            ResponseEntity.ok(results)

        } catch (e: IllegalStateException) {
            ResponseEntity.status(429).body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf("error" to "서버 오류가 발생했습니다. 다시 시도해주세요."))
        }
    }
}
