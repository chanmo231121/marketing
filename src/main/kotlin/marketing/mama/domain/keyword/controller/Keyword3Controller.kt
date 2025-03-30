package marketing.mama.domain.keyword.controller




import marketing.mama.domain.keyword.dto.KeywordTrendResponse
import marketing.mama.domain.keyword.service.Keyword3Service
import org.springframework.http.ResponseEntity

import org.springframework.web.bind.annotation.*



@RestController
@RequestMapping("/api/keyword3")
class Keyword3Controller(
    private val keyword3Service: Keyword3Service
) {

    @PostMapping("/open-login")
    fun openLoginWindow(): ResponseEntity<String> {
        keyword3Service.launchLoginBrowser()
        return ResponseEntity.ok("브라우저 로그인창을 열었습니다.")
    }

    @PostMapping("/confirm-login")
    fun confirmLogin(): ResponseEntity<String> {
        keyword3Service.extractTokenAndAccountId()
        return ResponseEntity.ok("로그인 정보 추출 완료")
    }

    @GetMapping("/search")
    fun getKeywordTrend(@RequestParam keyword: String): List<KeywordTrendResponse> {
        return keyword3Service.fetchKeywordTrend(keyword)
    }

    @GetMapping("/token-valid")
    fun isTokenValid(): Boolean {
        return keyword3Service.isTokenValidForFront()
    }
}