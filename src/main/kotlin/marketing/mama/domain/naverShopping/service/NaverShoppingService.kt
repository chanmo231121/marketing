package marketing.mama.domain.naverShopping.service

import io.github.bonigarcia.wdm.WebDriverManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.Proxy
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.PageLoadStrategy
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class NaverShoppingService {

    suspend fun crawlAll(keyword: String): Map<String, List<Map<String, Any>>> = coroutineScope {
        val mobileDeferred = async(Dispatchers.IO) { crawlMobile(keyword) }
        val pcDeferred     = async(Dispatchers.IO) { crawlPcHybrid(keyword) }
        mapOf(
            "mobile" to mobileDeferred.await(),
            "pc"     to pcDeferred.await()
        )
    }

    // Jsoup 전용 모바일 크롤링 (3페이지)
    private suspend fun crawlMobile(keyword: String): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Map<String, Any>>()
        val fmt     = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val base    = "https://msearch.shopping.naver.com/search/all?query=$keyword&sort=rel"
        var rank = 1

        for (page in 1..3) {
            val doc: Document = Jsoup.connect("$base&pagingIndex=$page")
                .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X)")
                .timeout(30000)
                .get()

            // 화면 노출 순서대로 한 번에 선택
            val items = doc.select(
                "div.adProduct_list_item__KlavS," +
                        "div.product_list_item__blfKk," +
                        "div.superSavingProduct_list_item__P9D0G"
            )
            items.forEach { el ->
                parseMobileElement(el, keyword, rank++, fmt)?.let { results.add(it) }
            }
        }
        results
    }

    // Selenium + Jsoup 하이브리드 PC 크롤링
    private suspend fun crawlPcHybrid(keyword: String): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        WebDriverManager.chromedriver().setup()
        val proxy = Proxy().apply {
            httpProxy = "123.214.67.61:8899"
            sslProxy  = "123.214.67.61:8899"
        }
        val options = ChromeOptions().apply {
            setPageLoadStrategy(PageLoadStrategy.NONE)
            setProxy(proxy)
            addArguments(
                "--headless=new", "--no-sandbox", "--disable-gpu", "--window-size=1920,1080",
                "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
            )
        }
        val driver: WebDriver = ChromeDriver(options)
        (driver as JavascriptExecutor)
            .executeScript("Object.defineProperty(navigator, 'webdriver', {get:() => undefined});")

        val results = mutableListOf<Map<String, Any>>()
        val fmt     = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        try {
            driver.get("https://search.shopping.naver.com/search/all?query=$keyword")
            WebDriverWait(driver, Duration.ofSeconds(30))
                .until(
                    ExpectedConditions.presenceOfElementLocated(
                        org.openqa.selenium.By.cssSelector(
                            "div.adProduct_item__T7utB," +
                                    "div.product_item__KQayS," +
                                    "div.superSavingProduct_item__6mR7_"
                        )
                    )
                )
            // 스크롤 내려서 로드
            repeat(3) {
                driver.executeScript("window.scrollTo(0, document.body.scrollHeight);")
                Thread.sleep(300)
            }
            val doc: Document = Jsoup.parse(driver.pageSource)
            var rank = 1

            // 화면 노출 순서대로 한 번에 선택
            val items = doc.select(
                "div.adProduct_item__T7utB," +
                        "div.product_item__KQayS," +
                        "div.superSavingProduct_item__6mR7_"
            )
            items.forEach { el ->
                parsePcElement(el, keyword, rank++, fmt)?.let { results.add(it) }
            }
        } finally {
            driver.quit()
        }
        results
    }

    // 공통 helper
    private fun text(el: Element?, sel: String) = el?.selectFirst(sel)?.text()?.trim().orEmpty()
    private fun attr(el: Element?, sel: String, attr: String) = el?.selectFirst(sel)?.attr(attr)?.trim().orEmpty()



    private fun parseMobileElement(
        el: Element, keyword: String, rank: Int, fmt: DateTimeFormatter
    ): Map<String, Any>? = try {
        val now = LocalDateTime.now().format(fmt)
        val cls = el.className()
        val isAd = cls.contains("adProduct_list_item__KlavS")
        val isSuper = cls.contains("superSavingProduct_list_item__P9D0G")
        val titleSel = when {
            isSuper -> "span.superSavingProduct_info_tit__fu4wy"
            isAd -> "div.adProduct_info_tit__fNwJk"
            else -> "span.product_info_tit__UOCqq"
        }
        val priceSel = when {
            isSuper -> "span.superSavingProduct_num__MZ8AU > strong"
            isAd -> "span.adProduct_num__dzUCr > strong"
            else -> "span.product_num__dWkfq > strong"
        }
        val deliverySel = when {
            isSuper -> "span.superSavingProduct_delivery__UejnS"
            isAd -> "span.adProduct_delivery__hEKiY"
            else -> "span.product_delivery__Ar5TF"
        }
        val title = text(el, titleSel)
        val price = text(el, priceSel)
        val delivery = text(el, deliverySel).replace("배송비", "")
        val seller = when {
            isSuper -> text(el, "span.superSavingProduct_mall__A_QU_")
            isAd -> text(el, "div.adProduct_link_mall__C7WE_ > span.adProduct_mall__UMb11")
            else -> {
                val raw = text(el, "div.product_seller__YUmkW").replace("판매처", "")
                raw.filter(Char::isDigit)
                    .ifEmpty { text(el, "div.product_link_mall___Dpmp > span.product_mall__gUvbk") }
            }
        }
        val rating = when {
            isSuper -> text(el, "span.superSavingProduct_grade__oqBnm > strong")
            isAd -> Regex("[\\d.]+").find(text(el, "span.adProduct_rating__n1sLP"))?.value.orEmpty()
            else -> text(el, "span.product_grade__eU8gY > strong")
        }
        val review = when {
            isSuper -> text(el, "span.superSavingProduct_grade__oqBnm > em")
            isAd -> text(el, "em.adProduct_count__moHgP")
            else -> text(el, "span.product_grade__eU8gY > em")
        }
        val purchase = when {
            isSuper -> text(el, "div.superSavingProduct_info_count__0j84V > span:nth-of-type(2) > em")
            else -> el.select("div.product_info_count__J6ElA > span").firstOrNull { it.text().contains("구매") }
                ?.selectFirst("em")?.text().orEmpty()
        }
        val favorite = when {
            isSuper -> text(el, "div.superSavingProduct_info_count__0j84V > span:nth-of-type(3) > em")
            isAd -> text(el, "span.adProduct_favorite__V_vvh").replace("찜", "")
            else -> el.select("div.product_info_count__J6ElA > span").firstOrNull { it.text().contains("찜") }
                ?.selectFirst("em")?.text().orEmpty()
        }
        mapOf(
            "현재시각" to now, "키워드" to keyword, "기기" to "모바일",
            "광고 구분" to when {
                isSuper -> "슈퍼세이빙"; isAd -> "광고"; else -> "기본"
            }, "노출순위" to rank,
            "타이틀" to title, "가격" to price, "배송비" to delivery,
            "판매처" to seller, "평점" to rating, "리뷰수" to review,
            "구매수" to purchase, "찜" to favorite
        )
    } catch (_: Exception) {
        null
    }



    private fun parsePcElement(
        el: Element,
        keyword: String,
        rank: Int,
        fmt: DateTimeFormatter
    ): Map<String, Any>? = try {
        val now = LocalDateTime.now().format(fmt)
        val cls = el.className()
        val sellers = MutableList(5) { "" }
        val adLabel: String
        var title = ""
        var price = ""
        var delivery = ""
        var rating = ""
        var review = ""
        var regDate = ""
        var zzim = ""
        var purchase = ""

        when {
            // ─── 광고 ─────────────────────────────────────────────────────────────
            cls.contains("adProduct_item__T7utB") -> {
                adLabel  = "광고"
                title    = el.selectFirst("div.adProduct_title__fsQU6 > a")?.attr("title")?.trim().orEmpty()
                price    = el.selectFirst("span.price_num__Y66T7 > em")?.text()?.trim().orEmpty()
                delivery = el.selectFirst("span.price_delivery__0jnYm")?.text()?.replace("배송비","")?.trim().orEmpty()
                // 판매자 1개
                sellers[0] = el.select("div.adProduct_mall_title__Ivl98 > a").firstOrNull()?.text().orEmpty()

                rating   = el.select("span.adProduct_rating__vk1YN")
                    .firstOrNull()?.text()?.trim().orEmpty()
                review   = el.select("em.adProduct_count__J5x57")
                    .firstOrNull()?.text()?.trim().orEmpty()
                regDate  = el.select("span.adProduct_etc__AM_WB")
                    .firstOrNull { it.text().contains("등록일") }
                    ?.text()?.replace("등록일","")?.trim().orEmpty()
                zzim     = el.select("span.adProduct_num__2Sl5g")
                    .firstOrNull()?.text()?.trim().orEmpty()
                purchase = ""
            }

            // ─── 슈퍼세이빙 ───────────────────────────────────────────────────────
            cls.contains("superSavingProduct_item__6mR7_") -> {
                adLabel  = "슈퍼세이빙"
                title    = el.selectFirst("div.superSavingProduct_title__WwZ_b > a")
                    ?.attr("title")?.trim().orEmpty()
                price    = el.selectFirst("span.price_num__Y66T7 > em")?.text()?.trim().orEmpty()
                delivery = el.selectFirst("span.price_delivery__0jnYm")
                    ?.text()?.replace("배송비","")?.trim().orEmpty()
                // 판매자 1개
                sellers[0] = el.select("div.superSavingProduct_mall_title__HQ6yD > a").firstOrNull()?.text().orEmpty()

                rating   = el.selectFirst("span.superSavingProduct_grade__wRr4y")
                    ?.text()?.replace("별점","")?.trim().orEmpty()
                // 리뷰
                el.select("em.superSavingProduct_num__cFGGK").forEach {
                    if (it.text().startsWith("(") && it.text().endsWith(")"))
                        review = it.text().removeSurrounding("(",")")
                }
                purchase = el.select("span span > em.superSavingProduct_num__cFGGK")
                    .firstOrNull()?.text()?.trim().orEmpty()
                // 등록일/찜
                el.select("span.superSavingProduct_etc___cO6c").forEach {
                    val t = it.text().trim()
                    if (t.contains("등록일")) regDate = t.replace("등록일","")
                    if (t.contains("찜"))     zzim     = it.select("span.superSavingProduct_num__cFGGK")
                        .firstOrNull()?.text()?.trim().orEmpty()
                }
            }

            // ─── 기본 ─────────────────────────────────────────────────────────────
            else -> {
                adLabel  = "기본"
                title    = el.selectFirst("a.product_link__aFnaq")?.attr("title")?.trim().orEmpty()
                price    = el.selectFirst("span.price_num__Y66T7 > em")?.text()?.trim().orEmpty()
                delivery = el.selectFirst("span.price_delivery__0jnYm")
                    ?.text()?.replace("배송비","")?.trim().orEmpty()

                // ── 1) span.product_mall_name__DuUQV 만 뽑아보기 ──
                val mallSpans = el.select("ul.product_mall_list__rYuBz span.product_mall_name__DuUQV")
                if (mallSpans.isNotEmpty()) {
                    mallSpans.forEachIndexed { i, span ->
                        if (i < 5) sellers[i] = span.text().trim()
                    }
                } else {
                    // ── 2) 기존 title → img.alt → <a>텍스트 로직 ──
                    el.select("ul.product_mall_list__rYuBz > a").forEachIndexed { i, aElem ->
                        if (i < 5) {
                            val titleAttr = aElem.attr("title").trim()
                            val imgAlt    = aElem.selectFirst("img")?.attr("alt")?.trim().orEmpty()
                            val linkText  = aElem.text().trim()
                            sellers[i] = when {
                                titleAttr.isNotEmpty() -> titleAttr
                                imgAlt.isNotEmpty()    -> imgAlt
                                else                   -> linkText
                            }
                        }
                    }
                    // ── 3) 여전히 비어 있으면 product_mall__0cRyd 링크로 한번 더 ──
                    if (sellers[0].isEmpty()) {
                        el.selectFirst("a.product_mall__0cRyd")?.let { fb ->
                            val titleAttr = fb.attr("title").trim()
                            val imgAlt    = fb.selectFirst("img")?.attr("alt")?.trim().orEmpty()
                            val linkText  = fb.text().trim()
                            sellers[0] = when {
                                titleAttr.isNotEmpty() -> titleAttr
                                imgAlt.isNotEmpty()    -> imgAlt
                                else                   -> linkText
                            }
                        }
                    }
                }

                rating   = el.selectFirst("span.product_grade__O_5f5")
                    ?.text()?.replace("별점","")?.trim().orEmpty()
                review   = el.selectXpath(
                    ".//span[@class='blind' and contains(text(),'리뷰')]/following-sibling::em[@class='product_num__WuH26']"
                ).firstOrNull()?.text()?.replace(Regex("[(),]"),"").orEmpty()
                purchase = el.selectXpath(
                    ".//span[contains(text(),'구매')]/em[@class='product_num__WuH26']"
                ).firstOrNull()?.text()?.replace(",","")?.trim().orEmpty()

                // 등록일/찜
                el.select("span.product_etc__Z7jnS").forEach {
                    val t = it.text().trim()
                    if (t.contains("등록일")) regDate = t.replace("등록일","")
                    if (t.contains("찜"))     zzim     = it.select("span.product_num__WuH26")
                        .firstOrNull()?.text()?.trim().orEmpty()
                }
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
    } catch (_: Exception) {
        null
    }
}
