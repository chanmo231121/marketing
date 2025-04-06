package marketing.mama.domain.keyword.controller

import io.swagger.v3.oas.annotations.Operation
import marketing.mama.domain.keyword.service.KeywordService
import marketing.mama.domain.search.service.SearchUsageService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@RestController
class KeywordController(
    private val keywordService: KeywordService,
    private val searchUsageService: SearchUsageService
) {

    @Operation(summary = "키워드 단일검색")
    @GetMapping("/api/keywords")
    @PreAuthorize("isAuthenticated()")
    fun getKeywords(@RequestParam("hintKeyword") hintKeyword: String): ResponseEntity<Any> {
        val hintKeywords = hintKeyword.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        if (hintKeywords.isEmpty()) {
            return ResponseEntity.badRequest().body(mapOf("error" to "키워드를 최소 1개 이상 입력해야 합니다."))
        }

        if (hintKeywords.size > 5) {
            return ResponseEntity.badRequest().body(mapOf("error" to "최대 5개의 키워드까지 조회할 수 있습니다."))
        }

        val results = try {
            keywordService.getKeywords(hintKeywords)
        } catch (e: Exception) {
            return ResponseEntity.internalServerError().body(mapOf("error" to "서버 오류가 발생했습니다. 다시 시도해주세요."))
        }

        return ResponseEntity.ok(results)
    }

    @Operation(summary = "검색 사용량 증가 (프론트에서 최초 1회 호출)")
    @GetMapping("/api/keywords/increment-usage")
    @PreAuthorize("isAuthenticated()")
    fun incrementSearchUsage(): ResponseEntity<Any> {
        return try {
            searchUsageService.incrementSingleSearchWithLimit(200)
            ResponseEntity.ok().body(mapOf("success" to true))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(429).body(mapOf("error" to e.message))
        }
    }
}

