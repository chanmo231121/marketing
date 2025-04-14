package marketing.mama.domain.search.controller

import marketing.mama.domain.search.dto.SearchUsageInfoResponse
import marketing.mama.domain.search.service.SearchUsageService
import marketing.mama.domain.user.repository.UserRepository
import marketing.mama.infra.security.UserPrincipal
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/search")
class SearchUsageController(
    private val searchUsageService: SearchUsageService,
    private val userRepository: UserRepository
) {

    @GetMapping("/single")
    fun singleSearch(@RequestParam keyword: String): ResponseEntity<Any> {
        return try {
            // 🔒 단일 검색 제한 + 사용량 증가
            searchUsageService.incrementSingleSearchWithLimit()

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
            searchUsageService.incrementRankingSearchWithLimit()

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


    @GetMapping("/usage-info/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')") // 관리자나 개발자만 조회 가능
    fun getUserSearchUsageInfo(@PathVariable userId: Long): ResponseEntity<SearchUsageInfoResponse> {
        return ResponseEntity.ok(searchUsageService.getUserSearchUsageInfo(userId))
    }

    @PostMapping("/{userId}/usage/reset")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    fun resetSearchUsage(
        @PathVariable userId: Long,
        @RequestBody payload: Map<String, String>
    ): ResponseEntity<Void> {
        val type = payload["type"]
        if (type.isNullOrBlank()) {
            return ResponseEntity.badRequest().build()
        }

        searchUsageService.resetTodayUsage(userId, type)
        return ResponseEntity.ok().build()
    }



}
