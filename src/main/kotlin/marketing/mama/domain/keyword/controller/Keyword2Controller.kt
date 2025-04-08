package marketing.mama.domain.keyword.controller

import io.swagger.v3.oas.annotations.Operation
import marketing.mama.domain.activitylog.model.ActionType
import marketing.mama.domain.activitylog.service.SearchLogService
import marketing.mama.domain.keyword.service.Keyword2Service
import marketing.mama.domain.user.repository.UserRepository
import marketing.mama.infra.security.UserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class Keyword2Controller(
    private val keyword2Service: Keyword2Service,
    private val userRepository: UserRepository,
    private val searchLogService: SearchLogService

) {

    @Operation(summary = "키워드 연관검색")
    @CrossOrigin
    @GetMapping("/api/keyword2")
    @PreAuthorize("isAuthenticated()")
    fun getKeywords(
        @RequestParam("hintKeyword") hintKeyword: String,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<List<Map<String, Any>>> {
        val keywordList = hintKeyword
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (keywordList.isEmpty()) {
            return ResponseEntity.badRequest().body(emptyList())
        }

        val user = userRepository.findById(userPrincipal.id).orElseThrow()

        searchLogService.logSearch(
            user = user,
            userName = user.name,
            uuid = user.deviceId,
            ip = user.ipAddress,
            keyword = keywordList.joinToString(", "),
            type = ActionType.연관검색
        )

        val result = keyword2Service.getKeywords(keywordList)
        return ResponseEntity.ok(result)
    }
}
