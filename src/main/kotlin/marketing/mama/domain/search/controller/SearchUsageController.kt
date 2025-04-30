package marketing.mama.domain.search.controller

import kotlinx.coroutines.runBlocking
import marketing.mama.domain.keyword.service.KeywordService
import marketing.mama.domain.keywordRanking.service.KeywordRankingService
import marketing.mama.domain.naverShopping.service.NaverShoppingService
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
    private val keywordService: KeywordService,
    private val keywordRankingService: KeywordRankingService,
    private val naverShoppingService: NaverShoppingService
) {

    @GetMapping("/single")
    fun singleSearch(
        @RequestParam keyword: String
    ): ResponseEntity<Any> {
        return try {
            searchUsageService.incrementSingleSearchWithLimit()
            val result = keywordService.getKeywords(listOf(keyword)) // ✅ 실제 네이버 키워드 API 호출
            ResponseEntity.ok(result)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(mapOf("error" to e.message))
        }
    }

    @GetMapping("/ranking")
    fun rankingSearch(
        @RequestParam keyword: String
    ): ResponseEntity<Any> {
        return try {
            searchUsageService.incrementRankingSearchWithLimit()
            val result = keywordRankingService.getNaverAdData(listOf(keyword), isFirst = false) // ✅ 여기 연결
            ResponseEntity.ok(result)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "서버 오류가 발생했습니다. 다시 시도해주세요."))
        }
    }


    @GetMapping("/shopping")
    fun shoppingSearch(
        @RequestParam keyword: String
    ): ResponseEntity<Any> {
        return try {
            // 1) 사용량 체크
            searchUsageService.incrementShoppingSearchWithLimit()

            // 2) suspend fun 호출을 runBlocking으로 감싸기
            val result = runBlocking {
                naverShoppingService.crawlAll(keyword)
            }

            ResponseEntity.ok(result)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "서버 오류가 발생했습니다. 다시 시도해주세요."))
        }
    }


    @GetMapping("/usage-info/{userId}")
    fun getUserSearchUsageInfo(@PathVariable userId: Long): ResponseEntity<SearchUsageInfoResponse> {
        return ResponseEntity.ok(searchUsageService.getUserSearchUsageInfo(userId))
    }

    @PostMapping("/{userId}/usage/reset")
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
