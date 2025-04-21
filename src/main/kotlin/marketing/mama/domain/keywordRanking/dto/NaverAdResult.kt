package marketing.mama.domain.keywordRanking.dto

data class NaverAdResult(
    val data: List<Map<String, Any>>,
    val failedKeywords: List<String>
)