package marketing.mama.domain.activitylog.service

import marketing.mama.domain.activitylog.dto.SearchLogResponse
import marketing.mama.domain.activitylog.model.ActionType
import marketing.mama.domain.activitylog.model.SearchLog
import marketing.mama.domain.activitylog.repository.SearchLogRepository
import marketing.mama.domain.user.model.User
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class SearchLogService(
    private val searchLogRepository: SearchLogRepository
) {

    fun logSearch(
        user: User,
        userName: String,
        uuid: String,
        ip: String?,
        keyword: String,
        type: ActionType
    ) {
        val log = SearchLog(
            user = user,
            userName = userName,
            uuid = uuid,
            ipAddress = ip,
            keyword = keyword,
            actionType = type
        )
        searchLogRepository.save(log)
    }

    fun logLogin(user: User, uuid: String, ip: String?) {
        val now = LocalDateTime.now()
        val log = SearchLog(
            user = user,
            userName = user.name,
            uuid = uuid,
            ipAddress = ip,
            keyword = "-",
            actionType = ActionType.로그인,
            searchedAt = now,
            loggedInAt = now
        )
        searchLogRepository.save(log)
    }

    fun getLogsByUserIdAndDate(userId: Long, date: LocalDate): List<SearchLogResponse> {
        val startDateTime = date.atStartOfDay()
        val endDateTime = date.plusDays(1).atStartOfDay()

        val logs = searchLogRepository.findAllByUserIdAndSearchedAtBetweenOrderBySearchedAtDesc(userId, startDateTime, endDateTime)
        return logs.map { it.toResponse() }
    }

    fun getLogsByUserId(userId: Long, limit: Int? = null, dateStr: String? = null): List<SearchLogResponse> {
        val logs = when {
            dateStr != null -> {
                val start = LocalDateTime.parse("${dateStr}T00:00:00")
                val end = LocalDateTime.parse("${dateStr}T23:59:59")
                searchLogRepository.findAllByUserIdAndSearchedAtBetweenOrderBySearchedAtDesc(userId, start, end)
            }
            limit != null -> {
                searchLogRepository.findTop30ByUserIdOrderBySearchedAtDesc(userId) // ✅ 여기가 문제없으면 됨
            }
            else -> {
                searchLogRepository.findAllByUserIdOrderBySearchedAtDesc(userId)
            }
        }

        return logs.map { it.toResponse() }
    }

    private fun getDateRange(dateStr: String): Pair<LocalDateTime, LocalDateTime> {
        val parsedDate = LocalDate.parse(dateStr)
        val start = parsedDate.atStartOfDay()
        val end = parsedDate.plusDays(1).atStartOfDay()
        return start to end
    }
}

fun SearchLog.toResponse(): SearchLogResponse {
    return SearchLogResponse(
        userName = this.userName,
        ipAddress = this.ipAddress,
        uuid = this.uuid,
        actionType = this.actionType.name,
        keyword = this.keyword,
        searchedAt = this.searchedAt
    )

}