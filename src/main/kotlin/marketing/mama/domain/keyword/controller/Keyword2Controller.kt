package marketing.mama.domain.keyword.controller

import io.swagger.v3.oas.annotations.Operation
import marketing.mama.domain.keyword.service.Keyword2Service
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class Keyword2Controller(
    private val keyword2Service: Keyword2Service
) {

    @Operation(summary = "키워드 연관검색")
    @CrossOrigin
    @GetMapping("/api/keyword2")
    fun getKeywords(
        @RequestParam("hintKeyword") hintKeyword: String
    ): ResponseEntity<List<Map<String, Any>>> {

        val keywordList = hintKeyword
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (keywordList.isEmpty()) {
            return ResponseEntity.badRequest().body(emptyList())
        }

        val result = keyword2Service.getKeywords(keywordList)
        return ResponseEntity.ok(result)
    }
}
