package marketing.mama.domain.naverShopping.service

import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class ShoppingService {

    fun crawlMobileShopping(keyword: String): List<Map<String, Any>> {
        WebDriverManager.chromedriver().setup()

        val options = ChromeOptions()
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--incognito")
        val driver = ChromeDriver(options)

        val resultList = mutableListOf<Map<String, Any>>()

        try {
            val url = "https://msearch.shopping.naver.com/search/all?query=$keyword"
            driver.get(url)

            val wait = WebDriverWait(driver, Duration.ofSeconds(10))
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.product_btn_link__AhZaM")))

            scrollToBottom(driver)

            val sections = driver.findElements(
                By.cssSelector("div.adProduct_list_item__KlavS, div.product_list_item__blfKk, div.superSavingProduct_list_item__P9D0G")
            )

            var rank = 1
            for (section in sections) {
                val itemType = getItemType(section)
                val item = parseItem(section, keyword, rank++, itemType)
                item?.let { resultList.add(it) }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            driver.quit()
        }

        return resultList
    }

    private fun scrollToBottom(driver: WebDriver) {
        var lastHeight = (driver as JavascriptExecutor).executeScript("return document.body.scrollHeight") as Long
        while (true) {
            driver.executeScript("window.scrollTo(0, document.body.scrollHeight);")
            Thread.sleep(500)
            val newHeight = driver.executeScript("return document.body.scrollHeight") as Long
            if (newHeight == lastHeight) break
            lastHeight = newHeight
        }
    }

    private fun getItemType(section: WebElement): String {
        val cls = section.getAttribute("class")
        return when {
            "adProduct_list_item__KlavS" in cls -> "광고"
            "superSavingProduct_list_item__P9D0G" in cls -> "슈퍼세이빙"
            else -> "기본"
        }
    }

    private fun extractTextSafe(parent: WebElement, selector: String): String {
        return try {
            parent.findElement(By.cssSelector(selector)).text.trim()
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseItem(section: WebElement, keyword: String, rank: Int, itemType: String): Map<String, Any>? {
        return try {
            val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val isAd = itemType == "광고"
            val isSuper = itemType == "슈퍼세이빙"

            val title = extractTextSafe(section, if (isSuper) "span.superSavingProduct_info_tit__fu4wy" else if (isAd) "div.adProduct_info_tit__fNwJk" else "span.product_info_tit__UOCqq")
            val price = extractTextSafe(section, if (isSuper) "span.superSavingProduct_num__MZ8AU strong" else if (isAd) "span.adProduct_num__dzUCr strong" else "span.product_num__dWkfq strong")
            val delivery = extractTextSafe(section, if (isSuper) "span.superSavingProduct_delivery__UejnS" else if (isAd) "span.adProduct_delivery__hEKiY" else "span.product_delivery__Ar5TF").replace("배송비", "").trim()

            // 판매처
            val seller = when {
                isSuper -> extractTextSafe(section, "span.superSavingProduct_mall__A_QU_")
                else -> try {
                    val raw = section.findElement(By.cssSelector("div.product_seller__YUmkW")).text
                    raw.replace("판매처", "").trim().filter { it.isDigit() }
                } catch (e: Exception) {
                    try {
                        section.findElement(By.cssSelector("div.adProduct_link_mall__C7WE_ span.adProduct_mall__UMb11")).text.trim()
                    } catch (ee: Exception) {
                        try {
                            section.findElement(By.cssSelector("div.product_link_mall___Dpmp span.product_mall__gUvbk")).text.trim()
                        } catch (eee: Exception) {
                            ""
                        }
                    }
                }
            }

            // 평점
            val rating = when {
                isSuper -> extractTextSafe(section, "span.superSavingProduct_grade__oqBnm strong")
                isAd -> {
                    val raw = extractTextSafe(section, "span.adProduct_rating__n1sLP")
                    Regex("""[\d.]+""").find(raw)?.value ?: ""
                }
                else -> extractTextSafe(section, "span.product_grade__eU8gY strong")
            }

            // 리뷰 수
            val review = when {
                isSuper -> extractTextSafe(section, "span.superSavingProduct_grade__oqBnm em")
                isAd -> extractTextSafe(section, "em.adProduct_count__moHgP")
                else -> extractTextSafe(section, "span.product_grade__eU8gY em")
            }

            // 구매수
            val purchase = when {
                isSuper -> extractTextSafe(section, "div.superSavingProduct_info_count__0j84V span:nth-of-type(2) em")
                else -> try {
                    val spans = section.findElement(By.cssSelector("div.product_info_count__J6ElA")).findElements(By.tagName("span"))
                    spans.find { it.text.contains("구매") }?.findElement(By.tagName("em"))?.text ?: ""
                } catch (_: Exception) { "" }
            }

            // 찜 수
            val favorite = when {
                isSuper -> extractTextSafe(section, "div.superSavingProduct_info_count__0j84V span:nth-of-type(3) em")
                isAd -> {
                    val raw = extractTextSafe(section, "span.adProduct_favorite__V_vhh")
                    raw.replace("찜", "").trim()
                }
                else -> try {
                    val spans = section.findElement(By.cssSelector("div.product_info_count__J6ElA")).findElements(By.tagName("span"))
                    spans.find { it.text.contains("찜") }?.findElement(By.tagName("em"))?.text ?: ""
                } catch (_: Exception) { "" }
            }

            return mapOf(
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
}
