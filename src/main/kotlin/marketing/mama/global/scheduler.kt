package marketing.mama.global

import jakarta.transaction.Transactional
import marketing.mama.domain.search.repository.SearchUsageRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class SearchUsageCleanupScheduler(
    private val searchUsageRepository: SearchUsageRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // 매일 자정에 실행
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    fun deleteOldSearchUsage() {
        val yesterday = LocalDate.now().minusDays(1)
        val deleted = searchUsageRepository.deleteByUsageDateBefore(yesterday)
        logger.info("🔄 이전 검색 사용 기록 삭제 완료. 날짜 < $yesterday")
    }

}