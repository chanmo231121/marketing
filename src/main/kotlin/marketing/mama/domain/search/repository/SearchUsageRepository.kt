package marketing.mama.domain.search.repository

import marketing.mama.domain.search.model.SearchUsage
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface SearchUsageRepository : JpaRepository<SearchUsage, Long> {
    fun findByUserIdAndUsageDate(userId: Long, usageDate: LocalDate): SearchUsage?
    fun deleteByUsageDateBefore(date: LocalDate)
}