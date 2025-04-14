package marketing.mama.domain.keywordRanking.service

import marketing.mama.domain.search.service.SearchUsageService
import org.jsoup.Jsoup
import org.jsoup.Connection
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
class KeywordRankingService(
    private val searchUsageService: SearchUsageService
) {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val logger = LoggerFactory.getLogger(KeywordRankingService::class.java)

    fun getNaverAdData(keywords: List<String>): List<Map<String, Any>> {
        // 🔒 사용량 체크 및 증가
        searchUsageService.incrementRankingSearchWithLimit()

        val results = mutableListOf<Map<String, Any>>()
        val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

        val futures = keywords.map { keyword ->
            executor.submit<List<Map<String, Any>>> {
                extractData(keyword)
            }
        }

        futures.forEach { future ->
            try {
                results.addAll(future.get())
            } catch (e: Exception) {
                logger.error("데이터 처리 중 오류 발생: ${e.message}", e)
            }
        }

        executor.shutdown()
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
        return results
    }

    private fun extractData(keyword: String): List<Map<String, Any>> {
        val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
        val pcUrl = "https://ad.search.naver.com/search.naver?where=ad&query=$encodedKeyword"
        val mobileUrl = "https://m.ad.search.naver.com/search.naver?where=m_expd&query=$encodedKeyword"
        val pcRows = mutableListOf<Map<String, Any>>()
        val mobileRows = mutableListOf<Map<String, Any>>()

        // PC 광고 추출
        try {
            val doc = createJsoupConnection(pcUrl).get()
            val items = doc.select("#content > div > ol > li > div.inner")

            items.forEachIndexed { index, item ->
                val titleElement = item.selectFirst("a.tit_wrap")
                val title = titleElement?.text()?.trim() ?: "No Title"
                val subtitle = item.selectFirst(".link_desc")?.text()?.trim() ?: "No Subtitle"
                val encryptedUrl = titleElement?.attr("href") ?: ""
                val originalUrl = try {
                    Jsoup.connect(encryptedUrl).followRedirects(false).execute().header("Location") ?: encryptedUrl
                } catch (e: Exception) {
                    null
                }
                val sellerName = item.selectFirst("a.site")?.text()?.trim() ?: "No Seller Name"
                val period = item.selectFirst(".period_area .txt")?.text()?.trim() ?: "No Period"

                pcRows.add(
                    mapOf(
                        "Date" to LocalDateTime.now().format(dateFormatter),
                        "Keyword" to keyword,
                        "Rank" to index + 1,
                        "Platform" to "PC",
                        "Title" to title,
                        "Subtitle" to subtitle,
                        "SellerName" to sellerName,
                        "Period" to period,
                        "Main URL" to (originalUrl ?: "")
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("키워드 '$keyword'의 PC 데이터 추출 실패: ${e.message}", e)
        }

        // Mobile 광고 추출
        try {
            val doc = createJsoupConnection(mobileUrl).get()
            val items = doc.select("#contentsList > li")

            items.forEachIndexed { index, item ->
                val titleElement = item.selectFirst("div.tit_wrap div.tit_area")
                val title = titleElement?.text()?.trim() ?: "No Title"
                val subtitle = item.selectFirst(".desc")?.text()?.trim() ?: "No Subtitle"
                val encryptedUrl = item.selectFirst("a")?.attr("href") ?: ""
                val originalUrl = try {
                    Jsoup.connect(encryptedUrl).followRedirects(false).execute().header("Location") ?: encryptedUrl
                } catch (e: Exception) {
                    null
                }
                val sellerName = item.selectFirst("span.site")?.text()?.trim() ?: "No Seller Name"
                val period = item.selectFirst(".period_area .txt")?.text()?.trim() ?: "No Period"

                mobileRows.add(
                    mapOf(
                        "Date" to LocalDateTime.now().format(dateFormatter),
                        "Keyword" to keyword,
                        "Rank" to index + 1,
                        "Platform" to "Mobile",
                        "Title" to title,
                        "Subtitle" to subtitle,
                        "SellerName" to sellerName,
                        "Period" to period,
                        "Main URL" to (originalUrl ?: "")
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("키워드 '$keyword'의 Mobile 데이터 추출 실패: ${e.message}", e)
        }

        return pcRows + mobileRows
    }

    private fun createJsoupConnection(url: String): Connection {
        return Jsoup.connect(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Connection", "keep-alive")
            .header("Cookie", "your_cookie_here")
    }
}
