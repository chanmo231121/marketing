package marketing.mama.domain.search.dto

import marketing.mama.domain.user.dto.response.UserResponse

data class UserWithUsageResponse(
    val user: UserResponse,
    val usage: SearchUsageInfoResponse,

)