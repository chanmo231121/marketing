package marketing.mama.domain.naverShopping.service

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.WaitForSelectorState
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class NaverShoppingService {

    /**
     * 모바일 + PC 크롤링 통합
     */
    fun crawlAll(keyword: String): Map<String, List<Map<String, Any>>> {
        val mobile = crawlMobileShopping(keyword)
        val pc = crawlPcShopping(keyword)
        return mapOf("mobile" to mobile, "pc" to pc)
    }

    /** 모바일 크롤링 */
    fun crawlMobileShopping(keyword: String): List<Map<String, Any>> {
        val playwright = Playwright.create()
        val browser = playwright.chromium().launch(
            BrowserType.LaunchOptions()
                .setHeadless(false) // ✅ headless 모드에서 동작 가능하도록 유지
                .setArgs(listOf(
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-blink-features=AutomationControlled",
                    "--disable-gpu",
                    "--disable-extensions",
                    "--mute-audio"
                ))
        )

        val context = browser.newContext(
            Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1")
                .setViewportSize(390, 844) // ✅ 모바일 해상도
                .setLocale("ko-KR")
                .setBypassCSP(true)
        )

        val page = context.newPage()
        page.setDefaultTimeout(15000.0)

        // ✅ 봇 탐지 우회
        page.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")

        val resultList = mutableListOf<Map<String, Any>>()

        try {
            val url = "https://msearch.shopping.naver.com/search/all?query=$keyword"
            page.navigate(url)

            // ✅ 요소가 존재할 때까지 기다림 (보이진 않아도 됨)
            page.waitForSelector("a.product_btn_link__AhZaM", Page.WaitForSelectorOptions().setState(WaitForSelectorState.ATTACHED))

            // ✅ 스크롤 다운
            scrollToBottom(page)

            // ✅ 항목 수집
            val sections = page.querySelectorAll(
                "div.adProduct_list_item__KlavS, div.product_list_item__blfKk, div.superSavingProduct_list_item__P9D0G"
            )
            var rank = 1
            for (section in sections) {
                val itemType = getItemType(section)
                parseMobileItem(section, keyword, rank++, itemType)?.let { resultList.add(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            browser.close()
            playwright.close()
        }

        return resultList
    }

    /** PC 크롤링 */
    fun crawlPcShopping(keyword: String): List<Map<String, Any>> {
        val playwright = Playwright.create()
        val browser = playwright.chromium().launch(
            BrowserType.LaunchOptions()
                .setHeadless(false)
                .setArgs(listOf(
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--incognito",
                    "--disable-blink-features=AutomationControlled",
                    "--disable-gpu",
                    "--disable-extensions",
                    "--disable-software-rasterizer",
                    "--mute-audio"
                ))
        )

        val context = browser.newContext(
            Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .setLocale("ko-KR")
                .setBypassCSP(true)
        )

        val page = context.newPage()
        page.setDefaultTimeout(20000.0)
        val dataList = mutableListOf<Map<String, Any>>()

        try {
            val url = "https://search.shopping.naver.com/search/all?query=$keyword"
            page.navigate(url)

            // 브라우저 환경 변수 숨기기
            page.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")

            // 페이지 로드 대기
            page.waitForLoadState()
            page.waitForSelector("div.adProduct_item__T7utB, div.product_item__KQayS, div.superSavingProduct_item__6mR7_")

            // 스크롤 다운 반복
            repeat(6) {
                page.evaluate("window.scrollTo(0, document.body.scrollHeight);")
                page.waitForTimeout(1500.0)
            }

            // 데이터 추출
            val sections = page.querySelectorAll(
                "div.adProduct_item__T7utB, div.product_item__KQayS, div.superSavingProduct_item__6mR7_"
            )
            var rank = 1
            for (section in sections) {
                parsePcItem(section, keyword, rank++)?.let { dataList.add(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            browser.close()
            playwright.close()
        }

        return dataList
    }

    private fun scrollToBottom(page: Page) {
        var lastHeight = page.evaluate("document.body.scrollHeight").toString().toDouble()
        while (true) {
            page.evaluate("window.scrollTo(0, document.body.scrollHeight);")
            page.waitForTimeout(500.0)
            val newHeight = page.evaluate("document.body.scrollHeight").toString().toDouble()
            if (newHeight == lastHeight) break
            lastHeight = newHeight
        }
    }

    private fun getItemType(element: com.microsoft.playwright.ElementHandle): String {
        val cls = element.getAttribute("class") ?: ""
        return when {
            "adProduct_list_item__KlavS" in cls -> "광고"
            "superSavingProduct_list_item__P9D0G" in cls -> "슈퍼세이빙"
            else -> "기본"
        }
    }

    private fun extractTextSafe(parent: com.microsoft.playwright.ElementHandle, selector: String): String {
        return try {
            parent.querySelector(selector)?.textContent()?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /** 모바일 아이템 파싱 */
    private fun parseMobileItem(
        section: com.microsoft.playwright.ElementHandle,
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
                    isAd -> "div.adProduct_info_tit__fNwJk"
                    else -> "span.product_info_tit__UOCqq"
                }
            )
            val price = extractTextSafe(
                section,
                when {
                    isSuper -> "span.superSavingProduct_num__MZ8AU strong"
                    isAd -> "span.adProduct_num__dzUCr strong"
                    else -> "span.product_num__dWkfq strong"
                }
            )
            val delivery = extractTextSafe(
                section,
                when {
                    isSuper -> "span.superSavingProduct_delivery__UejnS"
                    isAd -> "span.adProduct_delivery__hEKiY"
                    else -> "span.product_delivery__Ar5TF"
                }
            ).replace("배송비", "").trim()

            val seller = when {
                isSuper -> extractTextSafe(section, "span.superSavingProduct_mall__A_QU_")
                isAd -> extractTextSafe(section, "div.adProduct_link_mall__C7WE_ span.adProduct_mall__UMb11")
                else -> {
                    val rawCount = try {
                        section.querySelector("div.product_seller__YUmkW")?.textContent()
                            ?.replace("판매처", "")?.trim() ?: ""
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
                isAd -> Regex("""[\d.]+""")
                    .find(extractTextSafe(section, "span.adProduct_rating__n1sLP"))?.value ?: ""
                else -> extractTextSafe(section, "span.product_grade__eU8gY strong")
            }

            val review = when {
                isSuper -> extractTextSafe(section, "span.superSavingProduct_grade__oqBnm em")
                isAd -> extractTextSafe(section, "em.adProduct_count__moHgP")
                else -> extractTextSafe(section, "span.product_grade__eU8gY em")
            }

            val purchase = when {
                isSuper -> extractTextSafe(
                    section,
                    "div.superSavingProduct_info_count__0j84V span:nth-of-type(2) em"
                )
                else -> {
                    try {
                        section.querySelectorAll("div.product_info_count__J6ElA span")
                            .firstOrNull { it.textContent()?.contains("구매") == true }
                            ?.querySelector("em")?.textContent() ?: ""
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
                isAd -> extractTextSafe(section, "span.adProduct_favorite__V_vhh").replace("찜", "").trim()
                else -> {
                    try {
                        section.querySelectorAll("div.product_info_count__J6ElA span")
                            .firstOrNull { it.textContent()?.contains("찜") == true }
                            ?.querySelector("em")?.textContent() ?: ""
                    } catch (_: Exception) {
                        ""
                    }
                }
            }

            mapOf(
                "현재시각" to now,
                "키워드" to keyword,
                "기기" to "모바일",
                "광고 구분" to itemType,
                "노출순위" to rank,
                "타이틀" to title,
                "가격" to price,
                "배송비" to delivery,
                "판매처" to seller,
                "평점" to rating,
                "리뷰수" to review,
                "구매수" to purchase,
                "찜" to favorite
            )
        } catch (e: Exception) {
            null
        }
    }

    
    /** PC 아이템 파싱 */
    private fun parsePcItem(
        section: com.microsoft.playwright.ElementHandle,
        keyword: String,
        rank: Int
    ): Map<String, Any>? {
        return try {
            val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val className = section.getAttribute("class") ?: ""
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
                title = section.querySelector("div.adProduct_title__fsQU6 a")
                    ?.getAttribute("title")?.trim() ?: ""
                price = section.querySelector("span.price_num__Y66T7 em")?.textContent()?.trim() ?: ""
                delivery = section.querySelectorAll("span.price_delivery__0jnYm")
                    .firstOrNull()?.textContent()?.replace("배송비", "")?.trim() ?: ""
                sellers[0] = section.querySelectorAll("div.adProduct_mall_title__Ivl98 a")
                    .firstOrNull()?.textContent()?.trim() ?: ""
                rating = section.querySelectorAll("span.adProduct_rating__vk1YN")
                    .firstOrNull()?.textContent()?.trim() ?: ""
                review = section.querySelectorAll("em.adProduct_count__J5x57")
                    .firstOrNull()?.textContent()?.trim() ?: ""
                regDate = section.querySelectorAll("span.adProduct_etc__AM_WB")
                    .firstOrNull { it.textContent()?.contains("등록일") == true }
                    ?.textContent()?.replace("등록일", "")?.trim() ?: ""
                zzim = section.querySelectorAll("span.adProduct_num__2Sl5g")
                    .firstOrNull()?.textContent()?.trim() ?: ""
                purchase = ""
            } else if ("superSavingProduct_item__6mR7_" in className) {
                adLabel = "슈퍼세이빙"
                title = section.querySelector("div.superSavingProduct_title__WwZ_b a")
                    ?.getAttribute("title")?.trim() ?: ""
                price = section.querySelector("span.price_num__Y66T7 em")?.textContent()?.trim() ?: ""
                delivery = section.querySelectorAll("span.price_delivery__0jnYm")
                    .firstOrNull()?.textContent()?.replace("배송비", "")?.trim() ?: ""
                sellers[0] = section.querySelectorAll("div.superSavingProduct_mall_title__HQ6yD a")
                    .firstOrNull()?.textContent()?.trim() ?: ""
                rating = section.querySelectorAll("span.superSavingProduct_grade__wRr4y")
                    .firstOrNull()?.textContent()?.replace("별점", "")?.trim() ?: ""
                section.querySelectorAll("em.superSavingProduct_num__cFGGK").forEach {
                    val text = it.textContent() ?: ""
                    if (text.startsWith("(") && text.endsWith(")")) {
                        review = text.removeSurrounding("(", ")")
                    }
                }
                purchase = section.querySelectorAll("span span em.superSavingProduct_num__cFGGK")
                    .firstOrNull()?.textContent()?.trim() ?: ""
                section.querySelectorAll("span.superSavingProduct_etc___cO6c").forEach {
                    val text = it.textContent()?.trim() ?: ""
                    if ("등록일" in text) regDate = text.replace("등록일", "").trim()
                    if ("찜" in text) zzim = it.querySelectorAll("span.superSavingProduct_num__cFGGK")
                        .firstOrNull()?.textContent()?.trim() ?: ""
                }
            } else {
                adLabel = "기본"
                title = section.querySelector("a.product_link__aFnaq")
                    ?.getAttribute("title")?.trim() ?: ""
                price = section.querySelector("span.price_num__Y66T7 em")?.textContent()?.trim() ?: ""
                delivery = section.querySelectorAll("span.price_delivery__0jnYm")
                    .firstOrNull()?.textContent()?.replace("배송비", "")?.trim() ?: ""
                section.querySelectorAll("ul.product_mall_list__rYuBz a")
                    .forEachIndexed { idx, elem ->
                        if (idx < 5) sellers[idx] = elem.getAttribute("title")?.trim() ?: ""
                    }
                if (sellers[0].isEmpty()) {
                    sellers[0] = extractTextSafe(section, "a.product_mall__0cRyd")
                }
                rating = section.querySelectorAll("span.product_grade__O_5f5")
                    .firstOrNull()?.textContent()?.replace("별점", "")?.trim() ?: ""
                review = section.querySelector("""xpath=.//span[@class='blind' and text()='리뷰']/following-sibling::em[@class='product_num__WuH26']""")
                    ?.textContent()?.replace("(", "")?.replace(")", "")?.replace(",", "") ?: ""
                purchase = section.querySelector("""xpath=.//span[contains(text(),'구매')]/em[@class='product_num__WuH26']""")
                    ?.textContent()?.replace(",", "")?.trim() ?: ""
                section.querySelectorAll("span.product_etc__Z7jnS").forEach {
                    val text = it.textContent()?.trim() ?: ""
                    if ("등록일" in text) regDate = text.replace("등록일", "").trim()
                    if ("찜" in text) zzim = it.querySelectorAll("span.product_num__WuH26")
                        .firstOrNull()?.textContent()?.trim() ?: ""
                }
            }

            mapOf(
                "현재시각" to now,
                "키워드" to keyword,
                "기기" to "PC",
                "광고 구분" to adLabel,
                "노출순위" to rank,
                "타이틀" to title,
                "가격" to price,
                "배송비" to delivery,
                "판매자명1" to sellers[0],
                "판매자명2" to sellers[1],
                "판매자명3" to sellers[2],
                "판매자명4" to sellers[3],
                "판매자명5" to sellers[4],
                "별점" to rating,
                "리뷰수" to review,
                "등록일" to regDate,
                "찜수" to zzim,
                "구매수" to purchase
            )
        } catch (e: Exception) {
            null
        }
    }
}
