package marketing.mama.domain.keywordRanking.controller

import io.swagger.v3.oas.annotations.Operation
import marketing.mama.domain.activitylog.service.SearchLogService
import marketing.mama.domain.keywordRanking.service.KeywordRankingService
import marketing.mama.domain.search.service.SearchUsageService
import marketing.mama.domain.user.repository.UserRepository
import marketing.mama.infra.security.UserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*
import marketing.mama.domain.activitylog.model.ActionType

@RestController
@RequestMapping("/api/naver-ads")
@PreAuthorize("isAuthenticated()")
class KeywordRankingController(
    private val keywordRankingService: KeywordRankingService,
    private val userRepository: UserRepository,
    private val searchLogService: SearchLogService
) {

    @Operation(summary = "네이버 검색광고 입찰순위")
    @GetMapping("/search")
    fun searchNaverAds(
        @RequestParam keywords: String,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<Any> {
        return try {
            val decoded = URLDecoder.decode(keywords, StandardCharsets.UTF_8.toString())
            val keywordList = decoded.split("\n", ",").map { it.trim() }.filter { it.isNotEmpty() }

            val user = userRepository.findById(userPrincipal.id).orElseThrow()

            searchLogService.logSearch(
                user = user,
                userName = user.name,
                uuid = user.deviceId,
                ip = user.ipAddress,
                keyword = keywordList.joinToString(", "),
                type = ActionType.랭킹검색,
            )

            val results = keywordRankingService.getNaverAdData(keywordList)
            ResponseEntity.ok(results)

        } catch (e: IllegalStateException) {
            ResponseEntity.status(429).body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf("error" to "서버 오류가 발생했습니다. 다시 시도해주세요."))
        }
    }
}

