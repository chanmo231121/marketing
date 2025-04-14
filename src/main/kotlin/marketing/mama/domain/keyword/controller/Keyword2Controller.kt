package marketing.mama.domain.keyword.controller

import io.swagger.v3.oas.annotations.Operation
import marketing.mama.domain.activitylog.model.ActionType
import marketing.mama.domain.activitylog.service.SearchLogService
import marketing.mama.domain.keyword.service.Keyword2Service
import marketing.mama.domain.user.model.Status
import marketing.mama.domain.user.repository.UserRepository
import marketing.mama.domain.user.service.UserService
import marketing.mama.infra.security.UserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
class Keyword2Controller(
    private val keyword2Service: Keyword2Service,
    private val userRepository: UserRepository,
    private val searchLogService: SearchLogService,
    private val userService: UserService // ✅ 추가

) {

    @Operation(summary = "키워드 연관검색")
    @CrossOrigin
    @GetMapping("/api/keyword2")
    @PreAuthorize("isAuthenticated()")
    fun getKeywords(
        @RequestParam("hintKeyword") hintKeyword: String,
        @RequestHeader("X-Device-Id") deviceId: String?, // ✅ 추가
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<Any> {
        val keywordList = hintKeyword
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (keywordList.isEmpty()) {
            return ResponseEntity.badRequest().body(emptyList<Map<String, Any>>())
        }

        val user = userRepository.findById(userPrincipal.id).orElseThrow()
        userService.validateDevice(user, deviceId) // ✅ 여기서 기기 검사

        // 사용자 상태가 PENDING_APPROVAL이면, 승인 요청 메시지를 반환함
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
        }

        searchLogService.logSearch(
            user = user,
            userName = user.name,
            ip = user.ipAddress,
            keyword = keywordList.joinToString(", "),
            type = ActionType.연관검색,
            uuid = user.deviceId

        )

        val result = keyword2Service.getKeywords(keywordList)
        return ResponseEntity.ok(result)
    }
}
