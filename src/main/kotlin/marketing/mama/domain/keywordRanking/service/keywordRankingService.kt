package marketing.mama.domain.keywordRanking.service

import org.jsoup.Jsoup
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Service
class KeywordRankingService {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd") // 날짜 포맷터
    private val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 2)

    fun getNaverAdData(keywords: List<String>): List<Map<String, Any>> {
        val futures = keywords.map { keyword ->
            CompletableFuture.supplyAsync({ extractData(keyword) }, executor)
        }

        val results = mutableListOf<Map<String, Any>>()
        futures.forEach { future ->
            try {
                results.addAll(future.get())
            } catch (e: Exception) {
                println("데이터 처리 중 오류 발생: ${e.message}")
                e.printStackTrace() // 스택 트레이스 출력
            }
        }

        return results
    }

    private fun extractData(keyword: String): List<Map<String, Any>> {
        val pcUrl = "https://ad.search.naver.com/search.naver?where=ad&query=$keyword"
        val mobileUrl = "https://m.ad.search.naver.com/search.naver?where=m_expd&query=$keyword"
        val pcRows = mutableListOf<Map<String, Any>>()
        val mobileRows = mutableListOf<Map<String, Any>>()

        // PC 데이터 추출
        try {
            val doc = Jsoup.connect(pcUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .get()
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
                    println("URL 리다이렉션 실패: ${e.message}")
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
                        "Main URL" to (originalUrl ?: "No URL")  // null인 경우 기본값 설정
                    )
                )
            }
        } catch (e: Exception) {
            println("키워드 '$keyword'의 PC 데이터 추출 실패: ${e.stackTraceToString()}")  // 전체 스택 트레이스 출력
        }

        // 크롤링 지연 추가 (1초)
        Thread.sleep(1000)

        // 모바일 데이터 추출
        try {
            val doc = Jsoup.connect(mobileUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .get()
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
                    println("URL 리다이렉션 실패: ${e.message}")
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
                        "Main URL" to (originalUrl ?: "No URL")  // null인 경우 기본값 설정
                    )
                )
            }
        } catch (e: Exception) {
            println("키워드 '$keyword'의 Mobile 데이터 추출 실패: ${e.stackTraceToString()}")  // 전체 스택 트레이스 출력
        }

        // 크롤링 지연 추가 (1초)
        Thread.sleep(1000)

        // PC와 모바일 데이터를 합친 후 반환
        return pcRows.plus(mobileRows)
    }
}