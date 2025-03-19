package marketing.mama.domain.keywordRanking.controller

import io.swagger.v3.oas.annotations.Operation
import marketing.mama.domain.keywordRanking.service.KeywordRankingService
import org.springframework.web.bind.annotation.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/naver-ads")
class KeywordRankingController(private val keywordRankingService: KeywordRankingService) {

    @Operation(summary = "네이버 검색광고 입찰순위")
    @GetMapping("/search")
    fun searchNaverAds(@RequestParam keywords: String): List<Map<String, Any>> {
        println("✅ 요청 받은 키워드: $keywords")

        val decodedKeywords = URLDecoder.decode(keywords, StandardCharsets.UTF_8.toString())
        val keywordList = decodedKeywords.split(",").map { it.trim() }

        println("✅ 디코딩된 키워드 리스트: $keywordList")

        return keywordRankingService.getNaverAdData(keywordList)
    }
}
