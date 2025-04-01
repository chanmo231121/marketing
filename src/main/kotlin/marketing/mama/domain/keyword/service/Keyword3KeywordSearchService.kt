package marketing.mama.domain.keyword.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.bonigarcia.wdm.WebDriverManager
import jakarta.annotation.PostConstruct
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

    @PostConstruct
    fun cleanOldChromeProfiles() {
        val tmpDir = File("/tmp")
        tmpDir.listFiles { file ->
            file.isDirectory && file.name.startsWith("chrome-profile-") &&
                    file.lastModified() < System.currentTimeMillis() - 1000 * 60 * 60
        }?.forEach {
            try {
                println("🧹 오래된 디렉토리 삭제: ${it.absolutePath}")
                it.deleteRecursively()
            } catch (e: Exception) {
                println("⚠️ 삭제 실패: ${e.message}")
            }
        }
    }

    private fun simulateTyping(element: WebElement, text: String, driver: WebDriver) {
        val actions = Actions(driver)
        actions.moveToElement(element).click().perform()
        Thread.sleep(Random.nextLong(300, 600))
        for (ch in text) {
            if (Random.nextDouble() < 0.1) {
                val wrongChar = (('a'..'z') + ('0'..'9')).random().toString()
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
            println("▶ 광고 관리 페이지 접속 중...")
            driver.get("https://manage.searchad.naver.com/front")
            wait.until { (driver as JavascriptExecutor).executeScript("return document.readyState") == "complete" }
            println("✅ 페이지 로딩 완료: ${driver.currentUrl}")

            Thread.sleep(Random.nextLong(1000, 6000))
            println("▶ 네이버 로그인 버튼 대기 중...")
            val naverLoginBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.naver_login_btn")))
            println("✅ 네이버 로그인 버튼 클릭")
            naverLoginBtn.click()

            println("▶ 로그인 페이지 로딩 중...")
            val idElem = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("id")))
            simulateTyping(idElem, username, driver)

            val pwElem = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("pw")))
            simulateTyping(pwElem, password, driver)

            val loginSubmitBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("log.login")))
            Thread.sleep(Random.nextLong(500, 1000))
            println("✅ 로그인 버튼 클릭")
            loginSubmitBtn.click()

            Thread.sleep(Random.nextLong(4000, 5000))
            println("▶ 로그인 후 광고관리 페이지 재접속 중...")
            driver.get("https://manage.searchad.naver.com/front")
            wait.until { (driver as JavascriptExecutor).executeScript("return document.readyState") == "complete" }
            println("✅ 재접속 완료: ${driver.currentUrl}")

            val accountIdElem = try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("span.account_id em")))
            } catch (e: TimeoutException) {
                println("❌ 로그인 실패 또는 계정 식별자 로딩 실패")
                throw IllegalStateException("로그인에 실패했거나 계정 정보를 불러오지 못했습니다.")
            }
            val accountId = accountIdElem.text
            println("✅ 로그인 성공, accountId: $accountId")

            val js = driver as JavascriptExecutor
            val localStorage = js.executeScript("return window.localStorage.getItem('tokens');") as? String
            val mapper = jacksonObjectMapper()
            val tokens = mapper.readTree(localStorage)
            val firstKey = tokens.fieldNames().asSequence().firstOrNull()
            val token = tokens[firstKey]?.get("bearer")?.asText()
            println("✅ 토큰 추출 완료")

            return mapOf("token" to token.orEmpty(), "accountId" to accountId.orEmpty())
        } finally {
            println("🧹 드라이버 종료 및 프로필 삭제")
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
