package marketing.mama.domain.activitylog.repository

import marketing.mama.domain.activitylog.model.SearchLog
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface SearchLogRepository : JpaRepository<SearchLog, Long> {
    fun findAllBySearchedAtAfter(start: LocalDateTime): List<SearchLog>
    fun findAllByUserIdOrderBySearchedAtDesc(userId: Long): List<SearchLog>
    fun findTop30ByUserIdOrderBySearchedAtDesc(userId: Long): List<SearchLog>

    // 날짜 검색용
    fun findAllByUserIdAndSearchedAtBetweenOrderBySearchedAtDesc(
        userId: Long,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<SearchLog>
    fun findAllBySearchedAtBetween(start: LocalDateTime, end: LocalDateTime): List<SearchLog>
    fun deleteAllBySearchedAtBetween(start: LocalDateTime, end: LocalDateTime)

}