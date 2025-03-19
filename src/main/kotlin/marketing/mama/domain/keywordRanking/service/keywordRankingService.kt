package marketing.mama.domain.keywordRanking.service

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
class KeywordRankingService {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd") // 날짜 포맷터
    private val logger = LoggerFactory.getLogger(KeywordRankingService::class.java) // 로깅 프레임워크

    fun getNaverAdData(keywords: List<String>): List<Map<String, Any>> {
        val results = mutableListOf<Map<String, Any>>()
        val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()) // CPU 코어 수에 맞게 스레드 풀 설정

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
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS) // 모든 작업이 완료될 때까지 대기
        return results
    }

    private fun extractData(keyword: String): List<Map<String, Any>> {
        val encodedKeyword = URLEncoder.encode(keyword, "UTF-8") // URL 인코딩 추가
        val pcUrl = "https://ad.search.naver.com/search.naver?where=ad&query=$encodedKeyword"
        val mobileUrl = "https://m.ad.search.naver.com/search.naver?where=m_expd&query=$encodedKeyword"
        val pcRows = mutableListOf<Map<String, Any>>()
        val mobileRows = mutableListOf<Map<String, Any>>()

        // PC 데이터 추출
        try {
            val doc = createJsoupConnection(pcUrl).get()
            val items = doc.select("#content > div > ol > li > div.inner")

            items.forEachIndexed { index, item ->
                val titleElement = item.selectFirst("a.tit_wrap")
                val title = titleElement?.text()?.trim() ?: "No Title"

                val subtitleElement = item.selectFirst(".link_desc")  // subtitle에 해당하는 요소 선택
                val subtitle = subtitleElement?.text()?.trim() ?: "No Subtitle"  // subtitle 추출

                val encryptedUrl = titleElement?.attr("href") ?: ""
                val originalUrl = try {
                    val response = Jsoup.connect(encryptedUrl).followRedirects(false).execute()
                    response.header("Location") ?: encryptedUrl
                } catch (e: Exception) {
                    null
                }

                // 판매자명 추출 (PC는 a.site)
                val sellerNameElement = item.selectFirst("a.site") // 판매자명이 있는 <a> 태그 선택자
                val sellerName = sellerNameElement?.text()?.trim() ?: "No Seller Name"

                // period 추출 (PC에서)
                val periodElement = item.selectFirst(".period_area .txt")  // period에 해당하는 요소 선택
                val period = periodElement?.text()?.trim() ?: "No Period"  // period 추출

                pcRows.add(
                    mapOf(
                        "Date" to LocalDateTime.now().format(dateFormatter),  // 날짜 포맷팅
                        "Keyword" to keyword,
                        "Rank" to index + 1,
                        "Platform" to "PC",
                        "Title" to title,
                        "Subtitle" to subtitle,  // subtitle 추가
                        "SellerName" to sellerName, // 판매자명 추가
                        "Period" to period,  // period 추가
                        "Main URL" to (originalUrl ?: "")
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("키워드 '$keyword'의 PC 데이터 추출 실패: ${e.message}", e)
        }

        // 모바일 데이터 추출
        try {
            val doc = createJsoupConnection(mobileUrl).get()
            val items = doc.select("#contentsList > li")

            items.forEachIndexed { index, item ->
                val titleElement = item.selectFirst("div.tit_wrap div.tit_area")
                val title = titleElement?.text()?.trim() ?: "No Title"

                val subtitleElement = item.selectFirst(".desc")  // subtitle에 해당하는 요소 선택
                val subtitle = subtitleElement?.text()?.trim() ?: "No Subtitle"  // subtitle 추출

                val encryptedUrl = item.selectFirst("a")?.attr("href") ?: ""
                val originalUrl = try {
                    val response = Jsoup.connect(encryptedUrl).followRedirects(false).execute()
                    response.header("Location") ?: encryptedUrl
                } catch (e: Exception) {
                    null
                }

                // 모바일에서 판매자명 추출 (span.site에서 텍스트 추출)
                val sellerNameElement = item.selectFirst("span.site") // 모바일에서 판매자명이 있는 <span> 태그 선택자
                val sellerName = sellerNameElement?.text()?.trim() ?: "No Seller Name"

                // period 추출 (Mobile에서)
                val periodElement = item.selectFirst(".period_area .txt")  // period에 해당하는 요소 선택
                val period = periodElement?.text()?.trim() ?: "No Period"  // period 추출

                mobileRows.add(
                    mapOf(
                        "Date" to LocalDateTime.now().format(dateFormatter),  // 날짜 포맷팅
                        "Keyword" to keyword,
                        "Rank" to index + 1,
                        "Platform" to "Mobile",
                        "Title" to title,
                        "Subtitle" to subtitle,  // subtitle 추가
                        "SellerName" to sellerName, // 판매자명 추가
                        "Period" to period,  // period 추가
                        "Main URL" to (originalUrl ?: "")
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("키워드 '$keyword'의 Mobile 데이터 추출 실패: ${e.message}", e)
        }

        // PC와 모바일 데이터를 합친 후 반환
        return pcRows.plus(mobileRows).filter { it["Main URL"] != null }
    }

    private fun createJsoupConnection(url: String): Connection {
        return Jsoup.connect(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
            .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
            .header("Connection", "keep-alive")
            .header("Cookie", "nid_inf=1965910477; page_uid=i9kgHdpzL8VsstOs8Sdsssssted-056711; _naver_usersession_=Ky7LKvojsDp+oBZOPI1LEX1F")
    }
}