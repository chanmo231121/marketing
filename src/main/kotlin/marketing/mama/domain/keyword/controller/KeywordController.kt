package marketing.mama.domain.keyword.controller

import io.swagger.v3.oas.annotations.Operation
import marketing.mama.domain.keyword.service.KeywordService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@RestController
class KeywordController(val keywordService: KeywordService) {

    @Operation(summary = "키워드 단일검색")
    @GetMapping("/api/keywords")
    fun getKeywords(@RequestParam("hintKeyword") hintKeyword: String): ResponseEntity<Any> {
        val hintKeywords = hintKeyword.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        if (hintKeywords.isEmpty()) {
            return ResponseEntity.badRequest().body(mapOf("error" to "키워드를 최소 1개 이상 입력해야 합니다."))
        }

        if (hintKeywords.size > 1000) {
            return ResponseEntity.badRequest().body(mapOf("error" to "최대 100개의 키워드만 입력할 수 있습니다. 현재 입력한 키워드 개수: ${hintKeywords.size}"))
        }

        val results = mutableListOf<Map<String, Any>>()

        try {
            val keywordBatches = hintKeywords.chunked(5)
            for (batch in keywordBatches) {
                val encodedKeywords = batch.joinToString(",") { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) }
                val keywordsList = keywordService.getKeywords(encodedKeywords)
                results.addAll(keywordsList)
            }
        } catch (e: Exception) {
            return ResponseEntity.internalServerError().body(mapOf("error" to "서버 오류가 발생했습니다. 다시 시도해주세요."))
        }

        return ResponseEntity.ok(results)
    }
}