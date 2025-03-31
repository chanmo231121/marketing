package marketing.mama.domain.keyword.dto

data class KeywordResult(
    val keyword: String,
    val trend: List<KeywordTrend>
)
