package marketing.mama.domain.naverShopping.service

import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class ShoppingPcService {

    fun crawlPcShopping(keyword: String): List<Map<String, Any>> {
        WebDriverManager.chromedriver().setup()

        val options = ChromeOptions()
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--start-maximized", "--incognito")
        val driver = ChromeDriver(options)

        val dataList = mutableListOf<Map<String, Any>>()

        try {
            val url = "https://search.shopping.naver.com/search/all?query=$keyword"
            driver.get(url)

            val wait = WebDriverWait(driver, Duration.ofSeconds(10))
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.adProduct_item__T7utB, div.product_item__KQayS, div.superSavingProduct_item__6mR7_")))

            repeat(6) {
                (driver as JavascriptExecutor).executeScript("window.scrollTo(0, document.body.scrollHeight);")
                Thread.sleep(1500)
            }

            val sections = driver.findElements(By.cssSelector("div.adProduct_item__T7utB, div.product_item__KQayS, div.superSavingProduct_item__6mR7_"))
            var rank = 1

            for (section in sections) {
                try {
                    val className = section.getAttribute("class")
                    val adLabel: String
                    val sellers = MutableList(5) { "" }

                    val title: String
                    val price: String
                    val delivery: String
                    val seller: String
                    var rating = ""
                    var review = ""
                    var regDate = ""
                    var zzim = ""
                    var purchase = ""

                    if ("adProduct_item__T7utB" in className) {
                        adLabel = "광고"
                        title = section.findElement(By.cssSelector("div.adProduct_title__fsQU6 a")).getAttribute("title").trim()
                        price = section.findElement(By.cssSelector("span.price_num__Y66T7 em")).text.trim()
                        delivery = section.findElements(By.cssSelector("span.price_delivery__0jnYm")).firstOrNull()?.text?.replace("배송비", "")?.trim() ?: ""
                        seller = section.findElements(By.cssSelector("div.adProduct_mall_title__Ivl98 a")).firstOrNull()?.text?.trim() ?: ""
                        sellers[0] = seller

                        rating = section.findElements(By.cssSelector("span.adProduct_rating__vk1YN")).firstOrNull()?.text?.trim() ?: ""
                        review = section.findElements(By.cssSelector("em.adProduct_count__J5x57")).firstOrNull()?.text?.trim() ?: ""
                        regDate = section.findElements(By.cssSelector("span.adProduct_etc__AM_WB"))
                            .firstOrNull { it.text.contains("등록일") }?.text?.replace("등록일", "")?.trim() ?: ""
                        zzim = section.findElements(By.cssSelector("span.adProduct_num__2Sl5g")).firstOrNull()?.text?.trim() ?: ""

                    } else if ("superSavingProduct_item__6mR7_" in className) {
                        adLabel = "슈퍼세이빙"
                        title = section.findElement(By.cssSelector("div.superSavingProduct_title__WwZ_b a")).getAttribute("title").trim()
                        price = section.findElement(By.cssSelector("span.price_num__Y66T7 em")).text.trim()
                        delivery = section.findElements(By.cssSelector("span.price_delivery__0jnYm")).firstOrNull()?.text?.replace("배송비", "")?.trim() ?: ""
                        seller = section.findElements(By.cssSelector("div.superSavingProduct_mall_title__HQ6yD a")).firstOrNull()?.text?.trim() ?: ""
                        sellers[0] = seller

                        rating = section.findElements(By.cssSelector("span.superSavingProduct_grade__wRr4y")).firstOrNull()?.text?.replace("별점", "")?.trim() ?: ""
                        section.findElements(By.cssSelector("em.superSavingProduct_num__cFGGK")).forEach {
                            if (it.text.startsWith("(") && it.text.endsWith(")")) review = it.text.removeSurrounding("(", ")")
                        }

                        purchase = section.findElements(By.cssSelector("span span em.superSavingProduct_num__cFGGK")).firstOrNull()?.text?.trim() ?: ""

                        section.findElements(By.cssSelector("span.superSavingProduct_etc___cO6c")).forEach {
                            val text = it.text.trim()
                            if ("등록일" in text) regDate = text.replace("등록일", "").trim()
                            if ("찜" in text) {
                                zzim = it.findElements(By.cssSelector("span.superSavingProduct_num__cFGGK")).firstOrNull()?.text?.trim() ?: ""
                            }
                        }

                    } else {
                        adLabel = "기본"
                        title = section.findElement(By.cssSelector("a.product_link__aFnaq")).getAttribute("title").trim()
                        price = section.findElement(By.cssSelector("span.price_num__Y66T7 em")).text.trim()
                        delivery = section.findElements(By.cssSelector("span.price_delivery__0jnYm")).firstOrNull()?.text?.replace("배송비", "")?.trim() ?: ""

                        val sellerElems = section.findElements(By.cssSelector("ul.product_mall_list__rYuBz a"))
                        for (i in 0 until minOf(5, sellerElems.size)) {
                            sellers[i] = sellerElems[i].getAttribute("title").trim()
                        }

                        rating = section.findElements(By.cssSelector("span.product_grade__O_5f5")).firstOrNull()?.text?.replace("별점", "")?.trim() ?: ""
                        review = section.findElements(By.xpath(".//span[@class='blind' and text()='리뷰']/following-sibling::em[@class='product_num__WuH26']")).firstOrNull()?.text?.replace("(", "")?.replace(")", "")?.replace(",", "") ?: ""
                        purchase = section.findElements(By.xpath(".//span[contains(text(),'구매')]/em[@class='product_num__WuH26']")).firstOrNull()?.text?.replace(",", "")?.trim() ?: ""

                        section.findElements(By.cssSelector("span.product_etc__Z7jnS")).forEach {
                            val text = it.text.trim()
                            if ("등록일" in text) regDate = text.replace("등록일", "").trim()
                            if ("찜" in text) {
                                zzim = it.findElements(By.cssSelector("span.product_num__WuH26")).firstOrNull()?.text?.trim() ?: ""
                            }
                        }
                    }

                    val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    dataList.add(
                        mapOf(
                            "현재시각" to now,
                            "키워드" to keyword,
                            "기기" to "PC",
                            "광고 구분" to adLabel,
                            "노출순위" to rank++,
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
                    )
                } catch (e: Exception) {
                    println("상품 파싱 실패: ${e.message}")
                }
            }

        } finally {
            driver.quit()
        }

        return dataList
    }
}