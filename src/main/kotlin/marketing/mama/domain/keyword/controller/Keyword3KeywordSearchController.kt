package marketing.mama.domain.keyword.controller

import io.swagger.v3.oas.annotations.Operation
import marketing.mama.domain.keyword.dto.Keyword3KeywordRequest
import marketing.mama.domain.keyword.dto.KeywordResult
import marketing.mama.domain.keyword.dto.LoginRequest
import marketing.mama.domain.keyword.service.Keyword3KeywordSearchService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/keyword3")
class Keyword3KeywordSearchController(
    private val keywordSearchService: Keyword3KeywordSearchService
) {
    @Operation(summary = "자동로그인")
    // 프론트에서 아이디/비밀번호를 전달받아 자동 로그인 수행 후 토큰 및 계정ID 반환
    @PostMapping("/login")
    fun login(@RequestBody loginRequest: LoginRequest): ResponseEntity<Map<String, String>> {
        val result = keywordSearchService.loginWithCredentials(loginRequest.username, loginRequest.password)
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "키워드 연간검색")
    @PostMapping("/keywords")
    fun searchKeywords(
        @RequestHeader("Authorization") authorization: String,
        @RequestBody request: Keyword3KeywordRequest
    ): ResponseEntity<List<KeywordResult>> {
        val token = authorization.removePrefix("Bearer ").trim()
        val result = keywordSearchService.fetchKeywordData(token, request.keywords)
        return ResponseEntity.ok(result)
    }
}
