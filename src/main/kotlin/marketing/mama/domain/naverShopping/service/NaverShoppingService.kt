package marketing.mama.domain.naverShopping.service

import io.github.bonigarcia.wdm.WebDriverManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jsoup.nodes.Element
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

    data class CrawlConfig(
        val urlTemplate: String,
        val sectionSelector: String,
        val parseItem: (Element, String, Int) -> Map<String, Any>?
    )

    private val mobileConfig = CrawlConfig(
        urlTemplate = "https://msearch.shopping.naver.com/search/all?query=%s",
        sectionSelector = "div.adProduct_list_item__KlavS, div.product_list_item__blfKk, div.superSavingProduct_list_item__P9D0G",
        parseItem = { section, keyword, rank -> parseMobileItem(section, keyword, rank, getItemType(section)) }
    )

    private val pcConfig = CrawlConfig(
        urlTemplate = "https://search.shopping.naver.com/search/all?query=%s",
        sectionSelector = "div.adProduct_item__T7utB, div.product_item__KQayS, div.superSavingProduct_item__6mR7_",
        parseItem = ::parsePcItem
    )

    suspend fun crawlAll(keyword: String): Map<String, List<Map<String, Any>>> = coroutineScope {
        val mobileDeferred = async(Dispatchers.IO) { crawlByConfig(keyword, mobileConfig, true) }
        val pcDeferred     = async(Dispatchers.IO) { crawlByConfig(keyword, pcConfig, false) }
        mapOf(
            "mobile" to mobileDeferred.await(),
            "pc"     to pcDeferred.await()
        )
    }

    private suspend fun crawlByConfig(
        keyword: String,
        config: CrawlConfig,
        isMobile: Boolean
    ): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        WebDriverManager.chromedriver().setup()
        val driver = createDriver(isMobile)
        val results = mutableListOf<Map<String, Any>>()

        try {
            val url = config.urlTemplate.format(java.net.URLEncoder.encode(keyword, "UTF-8"))
            driver.get(url)

            WebDriverWait(driver, Duration.ofSeconds(3))
                .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body")))

            limitedScroll(driver, times = 3, sleepMillis = 300)

            val html = driver.pageSource
            val doc = org.jsoup.Jsoup.parse(html)

            val sections = doc.select(config.sectionSelector)
            var rank = 1
            for (section in sections) {
                config.parseItem(section, keyword, rank)?.let {
                    results.add(it)
                }
                rank++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            driver.quit()
        }

        results
    }

    private fun createDriver(isMobile: Boolean): WebDriver {
        val options = ChromeOptions().apply {
            setPageLoadStrategy(PageLoadStrategy.EAGER)
            // 공통 Proxy 설정
            val proxyIp = "123.214.67.61"
            val proxyPort = 8899
            setProxy(Proxy().apply {
                httpProxy = "$proxyIp:$proxyPort"
                sslProxy  = "$proxyIp:$proxyPort"
            })
            addArguments(
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--incognito",
                if (isMobile) "--headless=new" else "--headless=chrome",
                "--window-size=1920,1080",
                "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/91.0.4472.124 Safari/537.36",
                "--disable-blink-features=AutomationControlled",
                "--disable-extensions",
                "--disable-gpu",
                "--disable-software-rasterizer",
                "--disable-fonts",
                "--disable-notifications",
                "--disable-images",
                "--disable-javascript"
            )
            if (!isMobile) {
                addArguments("--lang=ko-KR")
                // webdriver 속성 감추기
                setExperimentalOption("excludeSwitches", listOf("enable-automation"))
                setExperimentalOption("useAutomationExtension", false)
            }
        }
        return ChromeDriver(options).also { driver ->
            if (!isMobile) {
                (driver as JavascriptExecutor).executeScript("""
                    Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
                    window.navigator.chrome = { runtime: {} };
                    Object.defineProperty(navigator, 'languages', { get: () => ['ko-KR', 'ko'] });
                    Object.defineProperty(navigator, 'plugins', { get: () => [1, 2, 3, 4, 5] });
                """.trimIndent())
            }
        }
    }

    private fun limitedScroll(driver: WebDriver, times: Int, sleepMillis: Long) {
        repeat(times) {
            (driver as JavascriptExecutor)
                .executeScript("window.scrollTo(0, document.body.scrollHeight);")
            Thread.sleep(sleepMillis)
        }
    }

    private fun getItemType(section: Element): String {
        val cls = section.className()
        return when {
            "adProduct_list_item__KlavS" in cls -> "광고"
            "superSavingProduct_list_item__P9D0G" in cls -> "슈퍼세이빙"
            else -> "기본"
        }
    }

    private fun extractTextSafe(parent: Element, selector: String): String =
        try { parent.selectFirst(selector)?.text()?.trim().orEmpty() } catch (_: Exception) { "" }




    private fun parseMobileItem(
        section: Element,
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
                    val rawCount = section.select("div.product_seller__YUmkW").text().replace("판매처", "").trim()
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
                isSuper -> extractTextSafe(section, "div.superSavingProduct_info_count__0j84V span:nth-of-type(2) em")
                else    -> section.select("div.product_info_count__J6ElA span")
                    .firstOrNull { it.text().contains("구매") }
                    ?.selectFirst("em")?.text()?.trim() ?: ""
            }

            val favorite = when {
                isSuper -> extractTextSafe(section, "div.superSavingProduct_info_count__0j84V span:nth-of-type(3) em")
                isAd    -> extractTextSafe(section, "span.adProduct_favorite__V_vhh").replace("찜", "").trim()
                else    -> section.select("div.product_info_count__J6ElA span")
                    .firstOrNull { it.text().contains("찜") }
                    ?.selectFirst("em")?.text()?.trim() ?: ""
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
                "찜"         to favorite
            )
        } catch (e: Exception) {
            null
        }
    }




    private fun parsePcItem(
        section: Element,
        keyword: String,
        rank: Int
    ): Map<String, Any>? {
        return try {
            val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val cls = section.className()
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

            if ("adProduct_item__T7utB" in cls) {
                adLabel = "광고"
                title = section.selectFirst("div.adProduct_title__fsQU6 a")?.attr("title")?.trim() ?: ""
                price = section.selectFirst("span.price_num__Y66T7 em")?.text()?.trim() ?: ""
                delivery = section.select("span.price_delivery__0jnYm")
                    .firstOrNull()?.text()?.replace("배송비", "")?.trim() ?: ""

                val sellerElement = section.selectFirst("div.adProduct_mall_title__Ivl98 a")
                val sellerText = sellerElement?.text()?.trim()
                sellers[0] = if (!sellerText.isNullOrEmpty()) {
                    sellerText
                } else {
                    sellerElement?.selectFirst("img")?.attr("alt")?.trim() ?: ""
                }

                rating = section.selectFirst("span.adProduct_rating__vk1YN")?.text()?.trim() ?: ""
                review = section.selectFirst("em.adProduct_count__J5x57")?.text()?.trim() ?: ""
                regDate = section.select("span.adProduct_etc__AM_WB")
                    .firstOrNull { it.text().contains("등록일") }
                    ?.text()?.replace("등록일", "")?.trim() ?: ""
                zzim = section.selectFirst("span.adProduct_num__2Sl5g")?.text()?.trim() ?: ""
                purchase = ""

            } else if ("superSavingProduct_item__6mR7_" in cls || "superSavingProduct_info_area__9mVo7" in cls) {
                adLabel = "슈퍼세이빙"
                title = section.selectFirst("div.superSavingProduct_title__WwZ_b a")?.attr("title")?.trim() ?: ""
                price = section.selectFirst("span.price_num__Y66T7 em")?.text()?.trim() ?: ""
                delivery = section.select("span.price_delivery__0jnYm")
                    .firstOrNull()?.text()?.replace("배송비", "")?.trim() ?: ""
                sellers[0] = section.selectFirst("div.superSavingProduct_mall_title__HQ6yD a")?.text()?.trim() ?: ""
                rating = section.selectFirst("span.superSavingProduct_grade__wRr4y")
                    ?.text()?.replace("별점", "")?.trim() ?: ""

                section.select("em.superSavingProduct_num__cFGGK").forEach {
                    if (it.text().startsWith("(") && it.text().endsWith(")")) {
                        review = it.text().removeSurrounding("(", ")")
                    }
                }

                purchase = section.select("span span em.superSavingProduct_num__cFGGK")
                    .firstOrNull()?.text()?.trim() ?: ""

                section.select("span.superSavingProduct_etc___cO6c").forEach {
                    val text = it.text().trim()
                    if ("등록일" in text) regDate = text.replace("등록일", "").trim()
                    if ("찜" in text) zzim = it.selectFirst("span.superSavingProduct_num__cFGGK")
                        ?.text()?.trim() ?: ""
                }

            } else {
                adLabel = "기본"
                title = section.selectFirst("a.product_link__aFnaq")?.attr("title")?.trim() ?: ""
                price = section.selectFirst("span.price_num__Y66T7 em")?.text()?.trim() ?: ""
                delivery = section.select("span.price_delivery__0jnYm")
                    .firstOrNull()?.text()?.replace("배송비", "")?.trim() ?: ""

                section.select("ul.product_mall_list__rYuBz a").forEachIndexed { idx, elem ->
                    if (idx < 5) sellers[idx] = elem.attr("title").trim()
                }

                if (sellers[0].isEmpty()) {
                    val sellerTag = section.selectFirst("a.product_mall__0cRyd")
                    val sellerText = sellerTag?.text()?.trim().orEmpty()
                    sellers[0] = if (sellerText.isNotEmpty()) {
                        sellerText
                    } else {
                        sellerTag?.selectFirst("img")?.attr("alt")?.trim().orEmpty()
                    }
                }

                rating = section.selectFirst("span.product_grade__O_5f5")
                    ?.text()?.replace("별점", "")?.trim() ?: ""

                review = section.select("span.blind")
                    .firstOrNull { it.text() == "리뷰" }
                    ?.nextElementSibling()?.text()?.replace("(", "")?.replace(")", "")?.replace(",", "") ?: ""

                purchase = section.select("span").firstOrNull { it.text().contains("구매") }
                    ?.selectFirst("em.product_num__WuH26")?.text()?.replace(",", "")?.trim() ?: ""

                section.select("span.product_etc__Z7jnS").forEach {
                    val text = it.text().trim()
                    if ("등록일" in text) regDate = text.replace("등록일", "").trim()
                    if ("찜" in text) zzim = it.selectFirst("span.product_num__WuH26")
                        ?.text()?.trim() ?: ""
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
