package marketing.mama.domain.keywordRanking.controller

import io.swagger.v3.oas.annotations.Operation
import marketing.mama.domain.keywordRanking.service.KeywordRankingService
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/naver-ads")
class keywordRankingController(private val keywordRankingService: KeywordRankingService) {



    @Operation(summary = "네이버 검색광고 입찰순위")
    @GetMapping("/search")
    fun searchNaverAds(@RequestParam keywords: String): List<Map<String, Any>> {
        val keywordList = keywords.split(",").map { it.trim() }
        return keywordRankingService.getNaverAdData(keywordList)
    }
}