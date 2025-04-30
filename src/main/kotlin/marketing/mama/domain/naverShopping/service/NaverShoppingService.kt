package marketing.mama.domain.naverShopping.service

import io.github.bonigarcia.wdm.WebDriverManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.openqa.selenium.PageLoadStrategy

@Service
class NaverShoppingService {

    /**
     * 모바일 + PC 크롤링 통합 (병렬 처리)
     */
    suspend fun crawlAll(keyword: String): Map<String, List<Map<String, Any>>> = coroutineScope {
        val mobileDeferred = async(Dispatchers.IO) { crawlMobileShopping(keyword) }
        val pcDeferred     = async(Dispatchers.IO) { crawlPcShopping(keyword) }
        mapOf(
            "mobile" to mobileDeferred.await(),
            "pc"     to pcDeferred.await()
        )
    }

    /** 모바일 크롤링 */
    private suspend fun crawlMobileShopping(keyword: String): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        WebDriverManager.chromedriver().setup()
        val options = ChromeOptions().apply {
            // ✨ 이렇게 할당이 아니라 메서드 호출
            setPageLoadStrategy(PageLoadStrategy.EAGER)

            addArguments(
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--incognito",
                "--headless=new",
                "--window-size=1920,1080",
                "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/91.0.4472.124 Safari/537.36",
                "--disable-blink-features=AutomationControlled",
                "--blink-settings=imagesEnabled=false",
                "--disable-extensions",
                "--disable-gpu",
                "--disable-software-rasterizer",
                "--disable-fonts",
                "--disable-notifications"
            )
        }

        val driver = ChromeDriver(options)
        val resultList = mutableListOf<Map<String, Any>>()

        try {
            val url = "https://msearch.shopping.naver.com/search/all?query=$keyword"
            driver.get(url)

            // 타임아웃 5초로 단축
            WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("a.product_btn_link__AhZaM")
                ))

            // 3회 스크롤, 각 500ms 대기
            limitedScroll(driver, times = 3, sleepMillis = 500)

            val sections = driver.findElements(
                By.cssSelector("div.adProduct_list_item__KlavS, div.product_list_item__blfKk, div.superSavingProduct_list_item__P9D0G")
            )
            var rank = 1
            for (section in sections) {
                val itemType = getItemType(section)
                parseMobileItem(section, keyword, rank++, itemType)?.let { resultList.add(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            driver.quit()
        }

        resultList
    }

    /** PC 크롤링 */
    private suspend fun crawlPcShopping(keyword: String): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        WebDriverManager.chromedriver().setup()
        val options = ChromeOptions().apply {
            setPageLoadStrategy(PageLoadStrategy.EAGER)
            addArguments(
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--incognito",
                "--headless=new",
                "--window-size=1920,1080",
                "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
                "--disable-blink-features=AutomationControlled",
                "--blink-settings=imagesEnabled=false",
                "--disable-extensions",
                "--disable-gpu",
                "--disable-software-rasterizer",
                "--disable-fonts",
                "--disable-notifications"
            )
        }
        val driver = ChromeDriver(options)
        val dataList = mutableListOf<Map<String, Any>>()

        try {
            val url = "https://search.shopping.naver.com/search/all?query=$keyword"
            driver.get(url)

            WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("div.adProduct_item__T7utB, div.product_item__KQayS, div.superSavingProduct_item__6mR7_")
                ))

            // 3회 스크롤, 각 1초 대기
            limitedScroll(driver, times = 3, sleepMillis = 1000)

            val sections = driver.findElements(
                By.cssSelector("div.adProduct_item__T7utB, div.product_item__KQayS, div.superSavingProduct_item__6mR7_")
            )
            var rank = 1
            for (section in sections) {
                parsePcItem(section, keyword, rank++)?.let { dataList.add(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            driver.quit()
        }

        dataList
    }

    /** 제한된 횟수로 스크롤 수행 */
    private fun limitedScroll(driver: WebDriver, times: Int, sleepMillis: Long) {
        repeat(times) {
            (driver as JavascriptExecutor)
                .executeScript("window.scrollTo(0, document.body.scrollHeight);")
            Thread.sleep(sleepMillis)
        }
    }

    private fun getItemType(section: WebElement): String {
        val cls = section.getAttribute("class")
        return when {
            "adProduct_list_item__KlavS"        in cls -> "광고"
            "superSavingProduct_list_item__P9D0G" in cls -> "슈퍼세이빙"
            else                                         -> "기본"
        }
    }

    private fun extractTextSafe(parent: WebElement, selector: String): String {
        return try {
            parent.findElement(By.cssSelector(selector)).text.trim()
        } catch (_: Exception) {
            ""
        }
    }

    /** 모바일 아이템 파싱 */
    private fun parseMobileItem(
        section: WebElement,
        keyword: String,
        rank: Int,
        itemType: String
    ): Map<String, Any>? {
        return try {
            val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val isAd = itemType == "광고"
            val isSuper = itemType == "슈퍼세이빙"

            val title = extractTextSafe(
                section,
                when {
                    isSuper -> "span.superSavingProduct_info_tit__fu4wy"
                    isAd    -> "div.adProduct_info_tit__fNwJk"
                    else    -> "span.product_info_tit__UOCqq"
                }
            )
            val price = extractTextSafe(
                section,
                when {
                    isSuper -> "span.superSavingProduct_num__MZ8AU strong"
                    isAd    -> "span.adProduct_num__dzUCr strong"
                    else    -> "span.product_num__dWkfq strong"
                }
            )
            val delivery = extractTextSafe(
                section,
                when {
                    isSuper -> "span.superSavingProduct_delivery__UejnS"
                    isAd    -> "span.adProduct_delivery__hEKiY"
                    else    -> "span.product_delivery__Ar5TF"
                }
            ).replace("배송비", "").trim()

            val seller = when {
                isSuper -> extractTextSafe(section, "span.superSavingProduct_mall__A_QU_")
                isAd    -> extractTextSafe(section, "div.adProduct_link_mall__C7WE_ span.adProduct_mall__UMb11")
                else    -> {
                    val rawCount = try {
                        section.findElement(By.cssSelector("div.product_seller__YUmkW")).text
                            .replace("판매처", "").trim()
                    } catch (e: Exception) {
                        ""
                    }
                    val digits = rawCount.filter { it.isDigit() }
                    if (digits.isNotEmpty()) digits
                    else extractTextSafe(section, "div.product_link_mall___Dpmp span.product_mall__gUvbk")
                }
            }

            val rating = when {
                isSuper -> extractTextSafe(section, "span.superSavingProduct_grade__oqBnm strong")
                isAd    -> Regex("""[\d.]+""")
                    .find(extractTextSafe(section, "span.adProduct_rating__n1sLP"))?.value ?: ""
                else    -> extractTextSafe(section, "span.product_grade__eU8gY strong")
            }

            val review = when {
                isSuper -> extractTextSafe(section, "span.superSavingProduct_grade__oqBnm em")
                isAd    -> extractTextSafe(section, "em.adProduct_count__moHgP")
                else    -> extractTextSafe(section, "span.product_grade__eU8gY em")
            }

            val purchase = when {
                isSuper -> extractTextSafe(
                    section,
                    "div.superSavingProduct_info_count__0j84V span:nth-of-type(2) em"
                )
                else    -> {
                    try {
                        section.findElements(By.cssSelector("div.product_info_count__J6ElA span"))
                            .firstOrNull { it.text.contains("구매") }
                            ?.findElement(By.tagName("em"))?.text ?: ""
                    } catch (_: Exception) {
                        ""
                    }
                }
            }

            val favorite = when {
                isSuper -> extractTextSafe(
                    section,
                    "div.superSavingProduct_info_count__0j84V span:nth-of-type(3) em"
                )
                isAd    -> extractTextSafe(section, "span.adProduct_favorite__V_vhh").replace("찜", "").trim()
                else    -> {
                    try {
                        section.findElements(By.cssSelector("div.product_info_count__J6ElA span"))
                            .firstOrNull { it.text.contains("찜") }
                            ?.findElement(By.tagName("em"))?.text ?: ""
                    } catch (_: Exception) {
                        ""
                    }
                }
            }

            mapOf(
                "현재시각"   to now,
                "키워드"     to keyword,
                "기기"       to "모바일",
                "광고 구분" to itemType,
                "노출순위"   to rank,
                "타이틀"     to title,
                "가격"       to price,
                "배송비"     to delivery,
                "판매처"     to seller,
                "평점"       to rating,
                "리뷰수"     to review,
                "구매수"     to purchase,
                "찜"        to favorite
            )
        } catch (e: Exception) {
            null
        }
    }

    /** PC 아이템 파싱 */
    private fun parsePcItem(
        section: WebElement,
        keyword: String,
        rank: Int
    ): Map<String, Any>? {
        return try {
            val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val className = section.getAttribute("class")
            val sellers = MutableList(5) { "" }
            val adLabel: String
            lateinit var title: String
            lateinit var price: String
            var delivery = ""
            var rating = ""
            var review = ""
            var regDate = ""
            var zzim = ""
            var purchase = ""

            if ("adProduct_item__T7utB" in className) {
                adLabel = "광고"
                title = section.findElement(By.cssSelector("div.adProduct_title__fsQU6 a"))
                    .getAttribute("title").trim()
                price = section.findElement(By.cssSelector("span.price_num__Y66T7 em")).text.trim()
                delivery = section.findElements(By.cssSelector("span.price_delivery__0jnYm"))
                    .firstOrNull()?.text?.replace("배송비", "")?.trim() ?: ""
                sellers[0] = section.findElements(By.cssSelector("div.adProduct_mall_title__Ivl98 a"))
                    .firstOrNull()?.text?.trim() ?: ""
                rating = section.findElements(By.cssSelector("span.adProduct_rating__vk1YN"))
                    .firstOrNull()?.text?.trim() ?: ""
                review = section.findElements(By.cssSelector("em.adProduct_count__J5x57"))
                    .firstOrNull()?.text?.trim() ?: ""
                regDate = section.findElements(By.cssSelector("span.adProduct_etc__AM_WB"))
                    .firstOrNull { it.text.contains("등록일") }
                    ?.text?.replace("등록일", "")?.trim() ?: ""
                zzim = section.findElements(By.cssSelector("span.adProduct_num__2Sl5g"))
                    .firstOrNull()?.text?.trim() ?: ""
                purchase = ""
            } else if ("superSavingProduct_item__6mR7_" in className) {
                adLabel = "슈퍼세이빙"
                title = section.findElement(By.cssSelector("div.superSavingProduct_title__WwZ_b a"))
                    .getAttribute("title").trim()
                price = section.findElement(By.cssSelector("span.price_num__Y66T7 em")).text.trim()
                delivery = section.findElements(By.cssSelector("span.price_delivery__0jnYm"))
                    .firstOrNull()?.text?.replace("배송비", "")?.trim() ?: ""
                sellers[0] = section.findElements(By.cssSelector("div.superSavingProduct_mall_title__HQ6yD a"))
                    .firstOrNull()?.text?.trim() ?: ""
                rating = section.findElements(By.cssSelector("span.superSavingProduct_grade__wRr4y"))
                    .firstOrNull()?.text?.replace("별점", "")?.trim() ?: ""
                section.findElements(By.cssSelector("em.superSavingProduct_num__cFGGK")).forEach {
                    if (it.text.startsWith("(") && it.text.endsWith(")")) {
                        review = it.text.removeSurrounding("(", ")")
                    }
                }
                purchase = section.findElements(By.cssSelector("span span em.superSavingProduct_num__cFGGK"))
                    .firstOrNull()?.text?.trim() ?: ""
                section.findElements(By.cssSelector("span.superSavingProduct_etc___cO6c")).forEach {
                    val text = it.text.trim()
                    if ("등록일" in text) regDate = text.replace("등록일", "").trim()
                    if ("찜"     in text) zzim     = it.findElements(By.cssSelector("span.superSavingProduct_num__cFGGK"))
                        .firstOrNull()?.text?.trim() ?: ""
                }
            } else {
                adLabel = "기본"
                title = section.findElement(By.cssSelector("a.product_link__aFnaq"))
                    .getAttribute("title").trim()
                price = section.findElement(By.cssSelector("span.price_num__Y66T7 em")).text.trim()
                delivery = section.findElements(By.cssSelector("span.price_delivery__0jnYm"))
                    .firstOrNull()?.text?.replace("배송비", "")?.trim() ?: ""
                section.findElements(By.cssSelector("ul.product_mall_list__rYuBz a"))
                    .forEachIndexed { idx, elem ->
                        if (idx < 5) sellers[idx] = elem.getAttribute("title").trim()
                    }
                if (sellers[0].isEmpty()) {
                    sellers[0] = extractTextSafe(section, "a.product_mall__0cRyd")
                }
                rating = section.findElements(By.cssSelector("span.product_grade__O_5f5"))
                    .firstOrNull()?.text?.replace("별점", "")?.trim() ?: ""
                review = section.findElements(By.xpath(".//span[@class='blind' and text()='리뷰']/following-sibling::em[@class='product_num__WuH26']"))
                    .firstOrNull()?.text?.replace("(", "")?.replace(")", "")?.replace(",", "") ?: ""
                purchase = section.findElements(By.xpath(".//span[contains(text(),'구매')]/em[@class='product_num__WuH26']"))
                    .firstOrNull()?.text?.replace(",", "")?.trim() ?: ""
                section.findElements(By.cssSelector("span.product_etc__Z7jnS")).forEach {
                    val text = it.text.trim()
                    if ("등록일" in text) regDate = text.replace("등록일", "").trim()
                    if ("찜"     in text) zzim     = it.findElements(By.cssSelector("span.product_num__WuH26"))
                        .firstOrNull()?.text?.trim() ?: ""
                }
            }

            mapOf(
                "현재시각"   to now,
                "키워드"     to keyword,
                "기기"       to "PC",
                "광고 구분" to adLabel,
                "노출순위"   to rank,
                "타이틀"     to title,
                "가격"       to price,
                "배송비"     to delivery,
                "판매자명1" to sellers[0],
                "판매자명2" to sellers[1],
                "판매자명3" to sellers[2],
                "판매자명4" to sellers[3],
                "판매자명5" to sellers[4],
                "별점"       to rating,
                "리뷰수"     to review,
                "등록일"     to regDate,
                "찜수"       to zzim,
                "구매수"     to purchase
            )
        } catch (e: Exception) {
            null
        }
    }
}
