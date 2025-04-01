package marketing.mama.domain.keyword.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.bonigarcia.wdm.WebDriverManager
import marketing.mama.domain.keyword.dto.KeywordResult
import marketing.mama.domain.keyword.dto.KeywordTrend
import okhttp3.OkHttpClient
import okhttp3.Request
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Service
import java.io.File
import java.time.Duration
import kotlin.random.Random

@Service
class Keyword3KeywordSearchService {
    private var token: String? = null
    private var accountId: String? = null

    /**
     * 입력 필드에 실제 사용자가 타이핑하는 것처럼 문자 하나씩 입력하는 함수.
     * Selenium Actions를 사용해 요소에 마우스 이동 및 클릭 후 타이핑하고,
     * 각 문자 사이에 0.5초 ~ 1초 사이의 랜덤 지연과 10% 확률로 오타 후 수정 동작을 추가합니다.
     */
    private fun simulateTyping(element: WebElement, text: String, driver: WebDriver) {
        val actions = Actions(driver)
        // 요소로 마우스 이동 후 클릭하여 포커스 맞춤
        actions.moveToElement(element).click().perform()
        Thread.sleep(Random.nextLong(300, 600))
        for (ch in text) {
            if (Random.nextDouble() < 0.1) {
                val wrongChar = (('a'..'z').toList() + ('0'..'9').toList()).random().toString()
                actions.sendKeys(wrongChar).perform()
                Thread.sleep(Random.nextLong(500, 1000))
                actions.sendKeys(Keys.BACK_SPACE).perform()
                Thread.sleep(Random.nextLong(300, 600))
            }
            actions.sendKeys(ch.toString()).perform()
            Thread.sleep(Random.nextLong(500, 1000))
        }
    }

    fun loginWithCredentials(username: String, password: String): Map<String, String> {
        // 고유한 user-data-dir 생성
        val uniqueUserDataDir = File("/tmp/chrome-profile-${System.currentTimeMillis()}").apply { mkdirs() }

        val options = ChromeOptions().apply {
            addArguments(
                "--headless",
                "--disable-gpu",
                "--window-size=1920,1080",
                "--no-sandbox",
                "--remote-allow-origins=*",
                "--disable-dev-shm-usage",
                "--disable-blink-features=AutomationControlled",
                "--user-data-dir=${uniqueUserDataDir.absolutePath}"
            )
        }

        WebDriverManager.chromedriver().setup()
        val driver: WebDriver = ChromeDriver(options)
        val wait = WebDriverWait(driver, Duration.ofSeconds(30))

        try {
            // 광고 관리 페이지에 접속하여 "네이버 아이디로 로그인" 버튼이 보이도록 합니다.
            driver.get("https://manage.searchad.naver.com/front")
            Thread.sleep(8000)
            wait.until { (driver as JavascriptExecutor).executeScript("return document.readyState").toString() == "complete" }
            Thread.sleep(Random.nextLong(4000, 5000))
            val naverLoginBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.naver_login_btn")))
            naverLoginBtn.click()

            // 네이버 로그인창에서 아이디와 비밀번호 입력
            val idElem = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("id")))
            simulateTyping(idElem, username, driver)
            val pwElem = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("pw")))
            simulateTyping(pwElem, password, driver)

            val loginSubmitBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("log.login")))
            Thread.sleep(Random.nextLong(4000, 5000))
            loginSubmitBtn.click()

            Thread.sleep(Random.nextLong(4000, 5000))

            // 로그인 후 광고 관리 페이지 로딩
            driver.get("https://manage.searchad.naver.com/front")
            wait.until { (driver as JavascriptExecutor).executeScript("return document.readyState").toString() == "complete" }
            val accountIdElem = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("span.account_id em")))
            val accountId = accountIdElem.text

            val js = driver as JavascriptExecutor
            val localStorage = js.executeScript("return window.localStorage.getItem('tokens');") as? String
            val mapper = jacksonObjectMapper()
            val tokens = mapper.readTree(localStorage)
            val firstKey = tokens.fieldNames().asSequence().firstOrNull()
            val token = tokens[firstKey]?.get("bearer")?.asText()

            return mapOf("token" to token.orEmpty(), "accountId" to accountId.orEmpty())
        } finally {
            try {
                driver.quit()
            } catch (e: Exception) {
                println("⚠️ driver 종료 중 예외: ${e.message}")
            }
            try {
                uniqueUserDataDir.deleteRecursively()
            } catch (e: Exception) {
                println("⚠️ 임시 디렉토리 삭제 실패: ${e.message}")
            }
        }
    }


    fun fetchKeywordData(token: String, keywords: List<String>): List<KeywordResult> {
        val client = OkHttpClient()
        val mapper = jacksonObjectMapper()
        val resultList = mutableListOf<KeywordResult>()
        for (keyword in keywords) {
            val url = "https://manage.searchad.naver.com/keywordstool" +
                    "?format=json&siteId=&month=&biztpId=&event=&includeHintKeywords=0&showDetail=1&keyword=${keyword}"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("Referer", "https://manage.searchad.naver.com/customers/$accountId/tool/keyword-planner")
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (response.isSuccessful && !body.isNullOrEmpty()) {
                    val json = mapper.readTree(body)
                    val keywordNode = json["keywordList"]?.find {
                        it["relKeyword"]?.asText()?.equals(keyword, ignoreCase = true) == true
                    }
                    if (keywordNode != null) {
                        val trendList = mutableListOf<KeywordTrend>()
                        val months = keywordNode["monthlyProgressList"]["monthlyLabel"]
                        val pcList = keywordNode["monthlyProgressList"]["monthlyProgressPcQcCnt"]
                        val mobileList = keywordNode["monthlyProgressList"]["monthlyProgressMobileQcCnt"]
                        for (i in 0 until months.size()) {
                            trendList.add(
                                KeywordTrend(
                                    month = months[i].asText(),
                                    pc = pcList[i].asText(),
                                    mobile = mobileList[i].asText()
                                )
                            )
                        }
                        resultList.add(KeywordResult(keyword, trendList))
                    }
                }
            }
        }
        return resultList
    }
}
