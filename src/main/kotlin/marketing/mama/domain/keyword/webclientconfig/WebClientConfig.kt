/*
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.net.URLEncoder

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

// Function to create headers
fun getHeader(method: String, uri: String, apiKey: String, secretKey: String, customerId: String): Map<String, String> {
    val timestamp = Instant.now().toEpochMilli().toString()
    val signature = Signature.generate(timestamp, method, uri, secretKey)

    return mapOf(
        "Content-Type" to "application/json; charset=UTF-8",
        "X-Timestamp" to timestamp,
        "X-API-KEY" to apiKey,
        "X-Customer" to customerId,
        "X-Signature" to signature
    )
}

// Function to make HTTP GET request
fun makeRequest(url: String, headers: Map<String, String>): Response {
    val client = OkHttpClient()
    val requestBuilder = Request.Builder().url(url)

    // Add headers to the request
    for ((key, value) in headers) {
        requestBuilder.addHeader(key, value)
    }

    val request = requestBuilder.build()
    return client.newCall(request).execute()
}

fun main() {
    val baseUrl = "https://api.naver.com"
    val method = "GET"
    val uri = "/keywordstool"
    val apiKey = "010000000052c2af4a5d6cdbeb92d376975b4ef04b3813214afa7dd4a3eaca3df3d7d75ebe"
    val secretKey = "AQAAAAC9WaMhLS9PP5Bao8EzL8dgycdW6peurw4EN108IfvRQw=="
    val customerId = "3399751"

    // Input from the user for the hint keyword
    print("연관키워드를 조회할 키워드를 입력!!!\n")
    val hintKeyword = readLine()

    // Encode the hint keyword to make it URL-safe
    val encodedHintKeyword = URLEncoder.encode(hintKeyword, "UTF-8")

    // Construct the full URL with the query parameters
    val url = "$baseUrl$uri?hintKeywords=$encodedHintKeyword&showDetail=1"

    // Make the HTTP request
    val response = makeRequest(url, getHeader(method, uri, apiKey, secretKey, customerId))

    // Parse the response
    val jsonResponse = JSONObject(response.body?.string())
    val keywordList = jsonResponse.getJSONArray("keywordList")

    // Process the data and print it (simulating a DataFrame-like output)
    val data = mutableListOf<Map<String, Any>>()
    for (i in 0 until keywordList.length()) {
        val keyword = keywordList.getJSONObject(i)

        // Directly get the "compIdx" value as a string without mapping it to a number
        val compIdx = keyword.getString("compIdx")

        val row = mapOf(
            "연관키워드" to keyword.getString("relKeyword"),
            "월간검색수_PC" to keyword.getInt("monthlyPcQcCnt"),
            "월간검색수_모바일" to keyword.getInt("monthlyMobileQcCnt"),
            "월평균클릭수_PC" to keyword.getInt("monthlyAvePcClkCnt"),
            "월평균클릭수_모바일" to keyword.getInt("monthlyAveMobileClkCnt"),
            "월평균클릭률_PC" to keyword.getDouble("monthlyAvePcCtr"),
            "월평균클릭률_모바일" to keyword.getDouble("monthlyAveMobileCtr"),
            "경쟁정도" to compIdx,  // Use the raw string value for "낮음", "중간", "높음"
            "월평균노출광고수" to keyword.getInt("plAvgDepth")
        )
        data.add(row)
    }

    // Print the processed data (simulating a DataFrame-like output)
    for (row in data) {
        println(row)
    }
}
*/
