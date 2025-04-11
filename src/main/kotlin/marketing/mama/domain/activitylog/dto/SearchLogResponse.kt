package marketing.mama.domain.activitylog.dto

import java.time.LocalDateTime
import java.time.ZonedDateTime

data class SearchLogResponse(
    val userName: String,
    val ipAddress: String?,
    val uuid: String?,
    val actionType: String,
    val keyword: String,
    val searchedAt: LocalDateTime,
)