package marketing.mama.domain.naverShopping.controller

import io.swagger.v3.oas.annotations.Operation
import marketing.mama.domain.activitylog.model.ActionType
import marketing.mama.domain.activitylog.service.SearchLogService
import marketing.mama.domain.naverShopping.service.NaverShoppingService
import marketing.mama.domain.search.service.SearchUsageService
import marketing.mama.domain.user.model.Status
import marketing.mama.domain.user.repository.UserRepository
import marketing.mama.domain.user.service.UserService
import marketing.mama.infra.security.UserPrincipal
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/shopping")
class NaverShoppingController(
    private val naverShoppingService: NaverShoppingService,
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val searchUsageService: SearchUsageService,
    private val searchLogService: SearchLogService,
) {

    @Operation(summary = "네이버 쇼핑 크롤링 (다중 키워드 처리, 관리자/승인 상태 예외 처리 포함)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun crawlMultiple(
        @RequestParam("keywords") keywords: List<String>,
        @RequestHeader("X-Device-Id") deviceId: String?,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<Any> {
        val user = userRepository.findById(userPrincipal.id)
            .orElseThrow { IllegalArgumentException("유효하지 않은 사용자입니다.") }
        userService.validateDevice(user, deviceId)

        if (user.role.name != "ADMIN") {
            when (user.status) {
                Status.PENDING_APPROVAL, Status.WAITING ->
                    return ResponseEntity.ok(mapOf("approvalMessage" to "⛔ 오른쪽 상단에 있는 승인요청을 해주세요! 하셨다면 대기해주세요!"))
                Status.PENDING_REAPPROVAL ->
                    return ResponseEntity.ok(mapOf("approvalMessage" to "⛔ 기간만료! 재승인을 해주세요."))
                else -> {
                    if (!user.canUseShoppingSearch) {
                        return ResponseEntity.ok(mapOf("approvalMessage" to "⛔ 쇼핑순위 검색기능 사용이 제한된 계정입니다. 관리자에게 문의해주세요."))
                    }
                }
            }
        }

        // 다중 키워드일 때도 키워드당 1회로 간주하여 사용량은 1회만 증가
        searchLogService.logSearch(
            user = user,
            userName = user.name,
            ip = user.ipAddress,
            keyword = keywords.joinToString(", "),
            type = ActionType.쇼핑검색,
            uuid = user.deviceId
        )
        searchUsageService.incrementShoppingSearchWithLimit()

        return try {
            val result = runBlocking { naverShoppingService.crawlMultipleKeywords(keywords) }
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            ResponseEntity.internalServerError()
                .body(mapOf("error" to "서버 오류가 발생했습니다. 다시 시도해주세요."))
        }
    }
}
