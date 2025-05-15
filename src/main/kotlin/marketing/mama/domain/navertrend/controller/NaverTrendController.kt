package marketing.mama.domain.navertrend.controller

import io.swagger.v3.oas.annotations.Operation
import marketing.mama.domain.activitylog.model.ActionType
import marketing.mama.domain.activitylog.service.SearchLogService
import marketing.mama.domain.navertrend.dto.NaverTrendRequest
import marketing.mama.domain.navertrend.service.NaverTrendService
import marketing.mama.domain.search.service.SearchUsageService
import marketing.mama.domain.user.model.Status
import marketing.mama.domain.user.repository.UserRepository
import marketing.mama.domain.user.service.UserService
import marketing.mama.infra.security.UserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/naver")
class NaverTrendController(
    private val naverTrendService: NaverTrendService,
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val searchUsageService: SearchUsageService,
    private val searchLogService: SearchLogService
) {

    @Operation(summary = "네이버 트렌드 검색")
    @PostMapping("/trend")
    @PreAuthorize("isAuthenticated()")
    fun getTrend(
        @RequestBody request: NaverTrendRequest,
        @RequestHeader("X-Device-Id") deviceId: String?,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<Any> {
        val user = userRepository.findById(userPrincipal.id).orElseThrow()
        userService.validateDevice(user, deviceId)

        if (user.role.name != "ADMIN") {
            when (user.status) {
                Status.PENDING_APPROVAL, Status.WAITING ->
                    return ResponseEntity.ok(mapOf("approvalMessage" to "⛔ 오른쪽 상단에 있는 승인요청을 해주세요! 하셨다면 대기해주세요!"))
                Status.PENDING_REAPPROVAL ->
                    return ResponseEntity.ok(mapOf("approvalMessage" to "⛔ 기간만료! 재승인을 해주세요."))
                else -> {
                    if (!user.canUseSingleSearch) {
                        return ResponseEntity.ok(mapOf("approvalMessage" to "⛔ 네이버 트렌드 기능 사용이 제한된 계정입니다. 관리자에게 문의해주세요."))
                    }
                }
            }
        }

        val keywords = request.keywordGroups.flatMap { it.keywords }

        // ✅ 검색 기록 로깅
        searchLogService.logSearch(
            user = user,
            userName = user.name,
            ip = user.ipAddress,
            keyword = keywords.joinToString(", "),
            type = ActionType.트렌드검색,
            uuid = user.deviceId
        )

        // ✅ 여기에서 첫 batch인지 확인하고 사용량 카운트
        if (request.isFirstBatch == true) {
            searchUsageService.incrementTrendSearchWithLimit()
        }

        return try {
            val result = naverTrendService.getTrend(request)
            ResponseEntity.ok(result)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(429).body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(500).body(mapOf("error" to "서버 오류가 발생했습니다. 다시 시도해주세요."))
        }
    }
}
