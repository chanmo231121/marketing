package marketing.mama.domain.search.dto

data class SearchUsageInfoResponse(
    val singleSearchLimit: Int,
    val singleSearchUsed: Int,
    val rankingSearchLimit: Int,
    val rankingSearchUsed: Int,
    val canUseSingleSearch: Boolean,
    val canUseRankingSearch: Boolean
)