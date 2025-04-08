package marketing.mama.domain.activitylog.dto

import marketing.mama.domain.activitylog.model.ActionType

data class FrontLogRequest(
    val uuid: String,
    val ip: String?,
    val keyword: String,
    val actionType: ActionType
)