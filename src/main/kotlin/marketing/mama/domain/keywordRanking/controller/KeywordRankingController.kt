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
        // 디코딩된 키워드 확인을 위한 로그 추가
        val decodedKeywords = URLDecoder.decode(keywords, StandardCharsets.UTF_8.toString())
        println("디코딩된 키워드: $decodedKeywords") // 디버깅 로그

        val keywordList = decodedKeywords.split(",").map { it.trim() }

        // 실제로 키워드 리스트 확인
        println("최종 키워드 리스트: $keywordList")  // 디버깅 로그

        return keywordRankingService.getNaverAdData(keywordList)
    }
}