package marketing.mama.domain.search.controller

import marketing.mama.domain.search.service.SearchUsageService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/search")
class SearchController(
    private val searchUsageService: SearchUsageService
) {

    @GetMapping("/single")
    fun singleSearch(@RequestParam keyword: String): ResponseEntity<Any> {
        return try {
            // 🔒 단일 검색 제한 + 사용량 증가
            searchUsageService.incrementSingleSearchWithLimit(200)

            // ✅ 검색 로직 실행
            val result = performSingleSearch(keyword)
            ResponseEntity.ok(result)
        } catch (e: IllegalStateException) {
            // ⛔ 제한 초과 시
            ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(mapOf("error" to e.message))
        }
    }

    @GetMapping("/ranking")
    fun rankingSearch(@RequestParam keyword: String): ResponseEntity<Any> {
        return try {
            // 🔒 랭킹 검색 제한 + 사용량 증가
            searchUsageService.incrementRankingSearchWithLimit(50)

            // ✅ 검색 로직 실행
            val result = performRankingSearch(keyword)
            ResponseEntity.ok(result)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(mapOf("error" to e.message))
        }
    }

    // ✅ 실제 검색 로직은 별도로 처리 (실제 구현부 대체 가능)
    private fun performSingleSearch(keyword: String): Any {
        return "단일 검색 결과: $keyword"
    }

    private fun performRankingSearch(keyword: String): Any {
        return "랭킹 순위 검색 결과: $keyword"
    }
}
