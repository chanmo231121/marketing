package marketing.mama.domain.keyword.controller

import io.swagger.v3.oas.annotations.Operation
import marketing.mama.domain.keyword.service.Keyword2Service
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@RestController
class Keyword2Controller(val keyword2Service: Keyword2Service) {

    @CrossOrigin(origins = ["https://87d6-123-214-67-61.ngrok-free.app", "http://localhost:9000"])
    @Operation(summary = "키워드 연관검색")
    @GetMapping("/api/keyword2")
    fun getKeywords(
        @RequestParam("hintKeyword") hintKeyword: String
    ): ResponseEntity<List<Map<String, Any>>> {

        // 쉼표로 구분된 키워드를 분리
        val hintKeyword2 = hintKeyword.split(",").map { it.trim() }

        // 각 키워드에 대해 검색 수행
        val results = mutableListOf<Map<String, Any>>()
        for (keyword in hintKeyword2) {
            val encodedHintKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString())
            val keywords = keyword2Service.getKeywords(encodedHintKeyword)
            results.addAll(keywords)
        }

        return ResponseEntity.ok(results)
    }
}
