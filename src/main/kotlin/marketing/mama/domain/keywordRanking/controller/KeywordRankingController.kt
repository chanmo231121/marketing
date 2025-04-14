package marketing.mama.domain.keywordRanking.controller

import io.swagger.v3.oas.annotations.Operation
import marketing.mama.domain.activitylog.model.ActionType
import marketing.mama.domain.activitylog.service.SearchLogService
import marketing.mama.domain.keywordRanking.service.KeywordRankingService
import marketing.mama.domain.search.service.SearchUsageService
import marketing.mama.domain.user.model.Status
import marketing.mama.domain.user.repository.UserRepository
import marketing.mama.domain.user.service.UserService
import marketing.mama.infra.security.UserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*

@RestController
@RequestMapping("/api/naver-ads")
@PreAuthorize("isAuthenticated()")
class KeywordRankingController(
    private val keywordRankingService: KeywordRankingService,
    private val userRepository: UserRepository,
    private val searchLogService: SearchLogService,
    private val userService: UserService // ✅ 추가

) {

    @Operation(summary = "네이버 검색광고 입찰순위")
    @GetMapping("/search")
    fun searchNaverAds(
        @RequestParam keywords: String,
        @RequestHeader("X-Device-Id") deviceId: String?, // ✅ 추가
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<Any> {
        return try {
            val decoded = URLDecoder.decode(keywords, StandardCharsets.UTF_8.toString())
            val keywordList = decoded.split("\n", ",").map { it.trim() }.filter { it.isNotEmpty() }

            val user = userRepository.findById(userPrincipal.id).orElseThrow()
            userService.validateDevice(user, deviceId)

            // 사용자 상태가 PENDING_APPROVAL이면, 승인 메시지를 반환함
            if (user.role.name != "ADMIN") {
                if (user.status == Status.PENDING_APPROVAL) {
                    return ResponseEntity.ok(mapOf("approvalMessage" to "⛔ 오른쪽 상단에 있는 승인요청을 해주세요!"))
                }

                if (user.status == Status.PENDING_REAPPROVAL) {
                    return ResponseEntity.ok(mapOf("approvalMessage" to "⛔ 기간만료! 재승인을 해주세요."))
                }

                if (user.status == Status.WAITING) {
                    return ResponseEntity.ok(mapOf("approvalMessage" to "⛔ 오른쪽 상단에 있는 승인요청을 해주세요!"))
                }
                if (!user.canUseRankingSearch) {
                    return ResponseEntity.ok(mapOf("approvalMessage" to "⛔ 랭킹검색 기능 사용이 제한된 계정입니다. 관리자에게 문의해주세요."))
                }
            }

            searchLogService.logSearch(
                user = user,
                userName = user.name,
                ip = user.ipAddress,
                keyword = keywordList.joinToString(", "),
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
