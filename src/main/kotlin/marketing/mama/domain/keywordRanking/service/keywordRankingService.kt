package marketing.mama.domain.keywordRanking.service

import org.jsoup.Jsoup
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

@Service
class KeywordRankingService {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd") // 날짜 포맷터

    fun getNaverAdData(keywords: List<String>): List<Map<String, Any>> {
        val results = mutableListOf<Map<String, Any>>()
        val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 2)

        val futures = keywords.map { keyword ->
            executor.submit<List<Map<String, Any>>> {
                try {
                    extractData(keyword)
                } catch (e: Exception) {
                    println("키워드 '$keyword' 처리 중 오류 발생: ${e.message}")
                    listOf(mapOf("error" to "크롤링 실패: ${e.message}"))
                }
            }
        }

        futures.forEach { future ->
            try {
                results.addAll(future.get())
            } catch (e: Exception) {
                println("데이터 처리 중 오류 발생: ${e.message}")
            }
        }

        executor.shutdown()
        return results
    }

    private fun extractData(keyword: String): List<Map<String, Any>> {
        val pcUrl = "https://ad.search.naver.com/search.naver?where=ad&query=$keyword"
        val mobileUrl = "https://m.ad.search.naver.com/search.naver?where=m_expd&query=$keyword"
        val pcRows = mutableListOf<Map<String, Any>>()
        val mobileRows = mutableListOf<Map<String, Any>>()

        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
            "Accept-Language" to "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
            "Connection" to "keep-alive",
            "Referer" to "https://ad.search.naver.com/",
            "Origin" to "https://ad.search.naver.com"
        )

        // PC 데이터 추출
        try {
            val response = Jsoup.connect(pcUrl)
                .headers(headers)
                .timeout(10000)
                .execute()

            val doc = response.parse()
            val items = doc.select("#content > div > ol > li > div.inner")

            items.forEachIndexed { index, item ->
                val titleElement = item.selectFirst("a.tit_wrap")
                val title = titleElement?.text()?.trim() ?: "No Title"

                val subtitleElement = item.selectFirst(".link_desc")
                val subtitle = subtitleElement?.text()?.trim() ?: "No Subtitle"

                val encryptedUrl = titleElement?.attr("href") ?: ""
                val originalUrl = try {
                    val redirectResponse = Jsoup.connect(encryptedUrl)
                        .headers(headers)
                        .followRedirects(false)
                        .timeout(10000)
                        .execute()
                    redirectResponse.header("Location") ?: encryptedUrl
                } catch (e: Exception) {
                    null
                }

                val sellerNameElement = item.selectFirst("a.site")
                val sellerName = sellerNameElement?.text()?.trim() ?: "No Seller Name"

                val periodElement = item.selectFirst(".period_area .txt")
                val period = periodElement?.text()?.trim() ?: "No Period"

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
            println("키워드 '$keyword'의 PC 데이터 추출 실패: ${e.message}")
        }

        // 모바일 데이터 추출
        try {
            val response = Jsoup.connect(mobileUrl)
                .headers(headers)
                .timeout(10000)
                .execute()

            val doc = response.parse()
            val items = doc.select("#contentsList > li")

            items.forEachIndexed { index, item ->
                val titleElement = item.selectFirst("div.tit_wrap div.tit_area")
                val title = titleElement?.text()?.trim() ?: "No Title"

                val subtitleElement = item.selectFirst(".desc")
                val subtitle = subtitleElement?.text()?.trim() ?: "No Subtitle"

                val encryptedUrl = item.selectFirst("a")?.attr("href") ?: ""
                val originalUrl = try {
                    val redirectResponse = Jsoup.connect(encryptedUrl)
                        .headers(headers)
                        .followRedirects(false)
                        .timeout(10000)
                        .execute()
                    redirectResponse.header("Location") ?: encryptedUrl
                } catch (e: Exception) {
                    null
                }

                val sellerNameElement = item.selectFirst("span.site")
                val sellerName = sellerNameElement?.text()?.trim() ?: "No Seller Name"

                val periodElement = item.selectFirst(".period_area .txt")
                val period = periodElement?.text()?.trim() ?: "No Period"

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
            println("키워드 '$keyword'의 Mobile 데이터 추출 실패: ${e.message}")
        }

        // PC와 모바일 데이터를 합친 후 반환
        return pcRows.plus(mobileRows).filter { it["Main URL"] != null }
    }
}