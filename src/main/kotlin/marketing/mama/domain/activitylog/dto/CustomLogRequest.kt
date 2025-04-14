package marketing.mama.domain.activitylog.dto

data class CustomLogRequest(
    val keyword: String,
    val uuid: String?
)