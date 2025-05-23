package marketing.mama.domain.keyword.controller

import io.swagger.v3.oas.annotations.Operation
import marketing.mama.domain.activitylog.model.ActionType
import marketing.mama.domain.activitylog.service.SearchLogService
import marketing.mama.domain.keyword.service.KeywordService
import marketing.mama.domain.search.service.SearchUsageService
import marketing.mama.domain.user.model.Status
import marketing.mama.domain.user.repository.UserRepository
import marketing.mama.domain.user.service.UserService
import marketing.mama.infra.security.UserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class KeywordController(
    private val keywordService: KeywordService,
    private val searchUsageService: SearchUsageService,
    private val userRepository: UserRepository,
    private val searchLogService: SearchLogService, // 👉 이 줄은 그대로 유지!
    private val userService: UserService
) {

    @Operation(summary = "키워드 단일검색")
    @GetMapping("/api/keywords")
    @PreAuthorize("isAuthenticated()")
    fun getKeywords(
        @RequestParam("hintKeyword") hintKeyword: String,
        @RequestParam(name = "isFirst", required = false, defaultValue = "false") isFirst: Boolean, // ✅ 추가
        @RequestHeader("X-Device-Id") deviceId: String?,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<Any> {
        val hintKeywords = hintKeyword.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        if (hintKeywords.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "키워드를 최소 1개 이상 입력해야 합니다."))
        }

        if (hintKeywords.size > 5) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "최대 5개의 키워드까지 조회할 수 있습니다."))
        }

        val user = userRepository.findById(userPrincipal.id).orElseThrow()
        userService.validateDevice(user, deviceId)

        if (user.role.name != "ADMIN") {
            when (user.status) {
                Status.PENDING_APPROVAL, Status.WAITING ->
                    return ResponseEntity.ok(mapOf("approvalMessage" to "⛔ 오른쪽 상단에 있는 승인요청을 해주세요! 하셨다면 대기해주세요! "))
                Status.PENDING_REAPPROVAL ->
                    return ResponseEntity.ok(mapOf("approvalMessage" to "⛔ 기간만료! 재승인을 해주세요."))
                else -> {
                    if (!user.canUseSingleSearch) {
                        return ResponseEntity.ok(mapOf("approvalMessage" to "⛔ 단일검색 기능 사용이 제한된 계정입니다. 관리자에게 문의해주세요."))
                    }
                }
            }
        }

        // ✅ isFirst=true일 때만 로그 저장
        if (isFirst) {
            searchLogService.logSearch(
                user = user,
                userName = user.name,
                ip = user.ipAddress,
                keyword = hintKeywords.joinToString(", ").take(255),
                type = ActionType.단일검색,
                uuid = user.deviceId
            )
        }

        val results = try {
            keywordService.getKeywords(hintKeywords)
        } catch (e: Exception) {
            return ResponseEntity.internalServerError()
                .body(mapOf("error" to "서버 오류가 발생했습니다. 다시 시도해주세요."))
        }

        return ResponseEntity.ok(mapOf("results" to results))
    }

    @Operation(summary = "검색 사용량 증가 (프론트에서 최초 1회 호출)")
    @GetMapping("/api/keywords/increment-usage")
    @PreAuthorize("isAuthenticated()")
    fun incrementSearchUsage(): ResponseEntity<Any> {
        return try {
            searchUsageService.incrementSingleSearchWithLimit()
            ResponseEntity.ok().body(mapOf("success" to true))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(429).body(mapOf("error" to e.message))
        }
    }
}
