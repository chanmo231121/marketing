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
    private val client = OkHttpClient()

    private val threadCount = Runtime.getRuntime().availableProcessors().coerceIn(2, 6)
    private val executor = Executors.newFixedThreadPool(threadCount)

    fun getKeywords(hintKeyword: String): List<Map<String, Any>> {
        val keywords = hintKeyword.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val resultList = Collections.synchronizedList(mutableListOf<Map<String, Any>>())

        val futures = keywords.map { keyword ->
            executor.submit {
                fetchKeywordData(keyword)?.let {
                    resultList.add(it)
                }
            }
        }

        futures.forEach {
            try {
                it.get()
            } catch (e: Exception) {
                println("오류 발생: ${e.message}")
            }
        }

        return resultList
    }

    private fun fetchKeywordData(keyword: String): Map<String, Any>? {
        val uri = "/keywordstool"
        val method = "GET"
        val url = "$baseUrl$uri?hintKeywords=$keyword&showDetail=1"
        val headers = getHeader(method, uri, apiKey, secretKey, customerId)

        repeat(3) { attempt ->
            makeRequest(url, headers).use { response ->
                when (response.code) {
                    429 -> {
                        println("API 호출 제한 발생, 재시도: ${attempt + 1}")
                        Thread.sleep(1000L * (attempt + 1))
                    }
                    200 -> {
                        val jsonResponse = JSONObject(response.body?.string() ?: "")
                        if (!jsonResponse.has("keywordList") || jsonResponse.getJSONArray("keywordList").length() == 0) {
                            println("검색 결과 없음: $keyword")
                            return null
                        }

                        val firstKeyword = jsonResponse.getJSONArray("keywordList").getJSONObject(0)
                        return mapOf(
                            "연관키워드" to firstKeyword.getString("relKeyword"),
                            "월간검색수_PC" to firstKeyword.optInt("monthlyPcQcCnt", 0),
                            "월간검색수_모바일" to firstKeyword.optInt("monthlyMobileQcCnt", 0),
                            "월평균클릭수_PC" to firstKeyword.optInt("monthlyAvePcClkCnt", 0),
                            "월평균클릭수_모바일" to firstKeyword.optInt("monthlyAveMobileClkCnt", 0),
                            "월평균클릭률_PC" to firstKeyword.optDouble("monthlyAvePcCtr", 0.0),
                            "월평균클릭률_모바일" to firstKeyword.optDouble("monthlyAveMobileCtr", 0.0),
                            "경쟁정도" to firstKeyword.getString("compIdx"),
                            "월평균노출광고수" to firstKeyword.optInt("plAvgDepth", 0)
                        )
                    }
                    else -> {
                        println("API 요청 실패: ${response.code}")
                        return null
                    }
                }
            }
        }
        println("최대 재시도 초과: $keyword")
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
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }
        return client.newCall(requestBuilder.build()).execute()
    }
}

object Signature {
    fun generate(timestamp: String, method: String, uri: String, secretKey: String): String {
        val message = "$timestamp.$method.$uri"
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKeySpec)
        return Base64.getEncoder().encodeToString(mac.doFinal(message.toByteArray(Charsets.UTF_8)))
    }
}
