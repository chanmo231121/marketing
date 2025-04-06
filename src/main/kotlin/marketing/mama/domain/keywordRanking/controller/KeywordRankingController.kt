package marketing.mama.domain.keywordRanking.controller

import io.swagger.v3.oas.annotations.Operation
import marketing.mama.domain.keywordRanking.service.KeywordRankingService
import marketing.mama.domain.search.service.SearchUsageService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/naver-ads")
@PreAuthorize("isAuthenticated()")
class KeywordRankingController(
    private val keywordRankingService: KeywordRankingService,
    private val searchUsageService: SearchUsageService // ✅ 추가
) {

    @RestController
    @RequestMapping("/api/naver-ads")
    @PreAuthorize("isAuthenticated()")
    class KeywordRankingController(
        private val keywordRankingService: KeywordRankingService
    ) {

        @Operation(summary = "네이버 검색광고 입찰순위")
        @GetMapping("/search")
        fun searchNaverAds(@RequestParam keywords: String): ResponseEntity<Any> {
            return try {
                val decoded = URLDecoder.decode(keywords, StandardCharsets.UTF_8.toString())
                val keywordList = decoded.split("\n", ",").map { it.trim() }.filter { it.isNotEmpty() }

                val results = keywordRankingService.getNaverAdData(keywordList)
                ResponseEntity.ok(results)

            } catch (e: IllegalStateException) {
                ResponseEntity.status(429).body(mapOf("error" to e.message)) // ✅ 동일하게 처리
            } catch (e: Exception) {
                ResponseEntity.internalServerError().body(mapOf("error" to "서버 오류가 발생했습니다. 다시 시도해주세요."))
            }
        }
    }
}
