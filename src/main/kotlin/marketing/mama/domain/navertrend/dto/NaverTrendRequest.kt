package marketing.mama.domain.navertrend.dto

data class NaverTrendRequest(
    val startDate: String,
    val endDate: String,
    val timeUnit: String,
    val keywordGroups: List<KeywordGroup>,
    val device: String?,
    val ages: List<String>?,
    val gender: String?
)

data class KeywordGroup(
    val groupName: String,
    val keywords: List<String>
)