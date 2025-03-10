package marketing.mama.domain.keyword.service

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class KeywordService {

    private val baseUrl = "https://api.naver.com"
    private val apiKey = "010000000052c2af4a5d6cdbeb92d376975b4ef04b3813214afa7dd4a3eaca3df3d7d75ebe"
    private val secretKey = "AQAAAAC9WaMhLS9PP5Bao8EzL8dgycdW6peurw4EN108IfvRQw=="
    private val customerId = "3399751"

    private val coreCount = Runtime.getRuntime().availableProcessors() // 사용 가능한 코어 개수
    private val executor = Executors.newFixedThreadPool((coreCount / 2).coerceAtLeast(1)) // CPU 절반만 사용

    fun getKeywords(hintKeyword: String): List<Map<String, Any>> {
        val keywords = hintKeyword.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val resultList = ArrayList<Map<String, Any>>(keywords.size) // 메모리 최적화

        val futures = keywords.map { keyword ->
            CompletableFuture.supplyAsync({ fetchKeywordData(keyword) }, executor)
        }

        futures.forEach { future ->
            future.get()?.let { resultList.add(it) }
        }

        return resultList
    }

    private fun fetchKeywordData(keyword: String): Map<String, Any>? {
        val uri = "/keywordstool"
        val method = "GET"
        val url = "$baseUrl$uri?hintKeywords=$keyword&showDetail=1"
        val headers = getHeader(method, uri, apiKey, secretKey, customerId)

        var attempt = 0
        while (attempt < 3) { // 최대 3번 재시도
            val response = makeRequest(url, headers)
            val responseBody = response.body?.string() ?: ""

            if (response.code == 200) {
                val jsonResponse = JSONObject(responseBody)
                val keywordList = jsonResponse.getJSONArray("keywordList")

                if (keywordList.length() > 0) {
                    val firstKeyword = keywordList.getJSONObject(0)
                    return mapOf(
                        "연관키워드" to firstKeyword.getString("relKeyword"),
                        "월간검색수_PC" to firstKeyword.getInt("monthlyPcQcCnt"),
                        "월간검색수_모바일" to firstKeyword.getInt("monthlyMobileQcCnt"),
                        "월평균클릭수_PC" to firstKeyword.getInt("monthlyAvePcClkCnt"),
                        "월평균클릭수_모바일" to firstKeyword.getInt("monthlyAveMobileClkCnt"),
                        "월평균클릭률_PC" to firstKeyword.getDouble("monthlyAvePcCtr"),
                        "월평균클릭률_모바일" to firstKeyword.getDouble("monthlyAveMobileCtr"),
                        "경쟁정도" to firstKeyword.getString("compIdx"),
                        "월평균노출광고수" to firstKeyword.getInt("plAvgDepth")
                    )
                }
                break
            } else if (response.code == 429) { // Too Many Requests
                println("요청이 너무 많음! 1초 대기 후 재시도 ($attempt)")
                Thread.sleep(1000)
                attempt++
            } else {
                println("API 오류 발생: ${response.code}, 응답: $responseBody")
                break
            }
        }

        // API 부하 방지 - 300~500ms 랜덤 딜레이
        Thread.sleep((300..500).random().toLong())
        return null
    }

    private fun getHeader(method: String, uri: String, apiKey: String, secretKey: String, customerId: String): Map<String, String> {
        val timestamp = System.currentTimeMillis().toString()
        val signature = Signature.generate(timestamp, method, uri, secretKey)

        return mapOf(
            "Content-Type" to "application/json; charset=UTF-8",
            "X-Timestamp" to timestamp,
            "X-API-KEY" to apiKey,
            "X-Customer" to customerId,
            "X-Signature" to signature
        )
    }

    private fun makeRequest(url: String, headers: Map<String, String>): okhttp3.Response {
        val client = OkHttpClient()
        val requestBuilder = Request.Builder().url(url)

        for ((key, value) in headers) {
            requestBuilder.addHeader(key, value)
        }

        val request = requestBuilder.build()
        return client.newCall(request).execute()
    }
}

// Signature class to generate the signature
object Signature {
    fun generate(timestamp: String, method: String, uri: String, secretKey: String): String {
        val message = "$timestamp.$method.$uri"
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKeySpec)
        val hash = mac.doFinal(message.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hash)
    }
}
