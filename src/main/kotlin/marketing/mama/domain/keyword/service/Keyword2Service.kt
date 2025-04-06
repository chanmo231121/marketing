package marketing.mama.domain.keyword.service

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class Keyword2Service {

    @Value("\${naverapi.base-url}")
    private lateinit var baseUrl: String

    @Value("\${naverapi.api-key}")
    private lateinit var apiKey: String

    @Value("\${naverapi.secret-key}")
    private lateinit var secretKey: String

    @Value("\${naverapi.customer-id}")
    private lateinit var customerId: String

    private val client = OkHttpClient() // 재사용 클라이언트

    fun getKeywords(hintKeywords: List<String>): List<Map<String, Any>> {
        val uri = "/keywordstool"
        val method = "GET"
        val joinedKeywords = hintKeywords.joinToString(",")
        val url = "$baseUrl$uri?hintKeywords=$joinedKeywords&showDetail=1"

        val headers = getHeader(method, uri, apiKey, secretKey, customerId)

        return try {
            val response = makeRequest(url, headers)
            val responseBody = response.body?.string() ?: return emptyList()
            val jsonResponse = JSONObject(responseBody)

            if (!jsonResponse.has("keywordList")) return emptyList()
            val keywordList = jsonResponse.getJSONArray("keywordList")

            val result = mutableListOf<Map<String, Any>>()
            val maxResults = minOf(keywordList.length(), 1000)

            for (i in 0 until maxResults) {
                val keyword = keywordList.getJSONObject(i)
                result.add(
                    mapOf(
                        "연관키워드" to keyword.getString("relKeyword"),
                        "월간검색수_PC" to keyword.optInt("monthlyPcQcCnt", 0),
                        "월간검색수_모바일" to keyword.optInt("monthlyMobileQcCnt", 0),
                        "월평균클릭수_PC" to keyword.optInt("monthlyAvePcClkCnt", 0),
                        "월평균클릭수_모바일" to keyword.optInt("monthlyAveMobileClkCnt", 0),
                        "월평균클릭률_PC" to keyword.optDouble("monthlyAvePcCtr", 0.0),
                        "월평균클릭률_모바일" to keyword.optDouble("monthlyAveMobileCtr", 0.0),
                        "경쟁정도" to keyword.optString("compIdx", "없음"),
                        "월평균노출광고수" to keyword.optInt("plAvgDepth", 0)
                    )
                )
            }

            result
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun getHeader(method: String, uri: String, apiKey: String, secretKey: String, customerId: String): Map<String, String> {
        val timestamp = System.currentTimeMillis().toString()
        val signature = Signature2.generate(timestamp, method, uri, secretKey)

        return mapOf(
            "Content-Type" to "application/json; charset=UTF-8",
            "X-Timestamp" to timestamp,
            "X-API-KEY" to apiKey,
            "X-Customer" to customerId,
            "X-Signature" to signature
        )
    }

    private fun makeRequest(url: String, headers: Map<String, String>): okhttp3.Response {
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }
        return client.newCall(requestBuilder.build()).execute()
    }
}

object Signature2 {
    fun generate(timestamp: String, method: String, uri: String, secretKey: String): String {
        val message = "$timestamp.$method.$uri"
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKeySpec)
        return Base64.getEncoder().encodeToString(mac.doFinal(message.toByteArray(Charsets.UTF_8)))
    }
}
