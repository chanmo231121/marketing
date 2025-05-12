package marketing.mama.domain.navertrend.service

import com.fasterxml.jackson.databind.ObjectMapper
import marketing.mama.domain.navertrend.dto.NaverTrendRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class NaverTrendService(
    private val restTemplate: RestTemplate = RestTemplate(),
    private val objectMapper: ObjectMapper = ObjectMapper() // ✅ 추가
) {

    @Value("\${naver.client-id}")
    lateinit var clientId: String

    @Value("\${naver.client-secret}")
    lateinit var clientSecret: String

    private val apiUrl = "https://openapi.naver.com/v1/datalab/search"

    fun getTrend(request: NaverTrendRequest): String {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("X-Naver-Client-Id", clientId)
            set("X-Naver-Client-Secret", clientSecret)
        }

        val jsonBody = objectMapper.writeValueAsString(request) // ✅ 수동 직렬화
        val entity = HttpEntity(jsonBody, headers)

        return try {
            val response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                String::class.java  // ✅ response type도 명시
            )
            response.body ?: throw RuntimeException("응답 없음")
        } catch (e: Exception) {
            throw RuntimeException("API 호출 실패: ${e.message}", e)
        }
    }
}
