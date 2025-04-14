package marketing.mama.domain.user.dto.request

data class UpdateFeatureUsageRequest(
    val feature: String,
    val enabled: Boolean
)
