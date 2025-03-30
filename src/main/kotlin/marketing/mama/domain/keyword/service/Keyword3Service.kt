package marketing.mama.domain.keyword.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.bonigarcia.wdm.WebDriverManager
import marketing.mama.domain.keyword.dto.KeywordTrendResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class Keyword3Service {

    private var loginDriver: WebDriver? = null
    private var bearerToken: String? = null
    private var accountId: String? = null
    private var tokenCreatedAt: Long? = null // 🔹 토큰 생성 시간 기록

    var baseApiUrl: String = "https://manage.searchad.naver.com"

    fun launchLoginBrowser() {
        val options = ChromeOptions().apply {
            addArguments("window-size=1200,800")
        }

        WebDriverManager.chromedriver().setup()
        loginDriver = ChromeDriver(options)
        loginDriver!!.get("https://searchad.naver.com/login")
    }

    fun extractTokenAndAccountId() {
        val driver = loginDriver ?: throw IllegalStateException("로그인 브라우저가 아직 실행되지 않았습니다.")
        val wait = WebDriverWait(driver, Duration.ofSeconds(30))

        driver.get("https://manage.searchad.naver.com/front")
        val accountIdElem = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("span.account_id em")))
        accountId = accountIdElem.text
        bearerToken = getBearerTokenFromLocalStorage(driver)
        tokenCreatedAt = System.currentTimeMillis() // 🔹 토큰 발급 시간 기록

        driver.quit()
        loginDriver = null
    }

    fun isTokenValidForFront(): Boolean {
        return bearerToken != null && tokenCreatedAt != null &&
                System.currentTimeMillis() - tokenCreatedAt!! < 10 * 60 * 1000
    }

    fun fetchKeywordTrend(keyword: String): List<KeywordTrendResponse> {
        if (!isTokenValid()) {
            throw IllegalStateException("토큰이 만료되었거나 존재하지 않습니다. 다시 로그인해주세요.")
        }

        val token = bearerToken!!
        val accId = accountId!!

        val url = "$baseApiUrl/keywordstool" +
                "?format=json&siteId=&month=&biztpId=&event=&includeHintKeywords=0&showDetail=1&keyword=${keyword}"

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .addHeader("Referer", "https://manage.searchad.naver.com/customers/$accId/tool/keyword-planner")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RuntimeException("API 호출 실패: ${response.code}")
            val body = response.body?.string() ?: throw RuntimeException("응답 없음")
            return parseKeywordJson(keyword, body)
        }
    }

    private fun parseKeywordJson(keyword: String, body: String): List<KeywordTrendResponse> {
        val mapper = jacksonObjectMapper()
        val root = mapper.readTree(body)
        val keywordNode = root["keywordList"].firstOrNull {
            it["relKeyword"]?.asText()?.equals(keyword, ignoreCase = true) == true
        } ?: return emptyList()

        val months = keywordNode["monthlyProgressList"]["monthlyLabel"]
        val pcList = keywordNode["monthlyProgressList"]["monthlyProgressPcQcCnt"]
        val mobileList = keywordNode["monthlyProgressList"]["monthlyProgressMobileQcCnt"]

        return (0 until months.size()).map {
            KeywordTrendResponse(
                month = months[it].asText(),
                pc = pcList[it].asText(),
                mobile = mobileList[it].asText()
            )
        }
    }

    private fun getBearerTokenFromLocalStorage(driver: WebDriver): String? {
        val js = driver as JavascriptExecutor
        val localStorage = js.executeScript("return window.localStorage.getItem('tokens');") as? String ?: return null
        val tokens = jacksonObjectMapper().readTree(localStorage)
        val firstKey = tokens.fieldNames().asSequence().firstOrNull() ?: return null
        return tokens[firstKey]?.get("bearer")?.asText()
    }

    // 🔹 토큰 유효성 검사 (10분 유지)
    private fun isTokenValid(): Boolean {
        return bearerToken != null && tokenCreatedAt != null &&
                System.currentTimeMillis() - tokenCreatedAt!! < 10 * 60 * 1000
    }
}
