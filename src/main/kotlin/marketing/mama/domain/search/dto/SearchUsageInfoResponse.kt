package marketing.mama.domain.search.dto

data class SearchUsageInfoResponse(
    val singleSearchLimit: Int,
    val singleSearchUsed: Int,
    val rankingSearchLimit: Int,
    val rankingSearchUsed: Int,
    val shoppingSearchLimit: Int,
    val shoppingSearchUsed: Int,
    val trendSearchLimit: Int,
    val trendSearchUsed: Int,
    val canUseSingleSearch: Boolean,
    val canUseRankingSearch: Boolean,
    val canUseShoppingSearch: Boolean,
    val canUseTrendSearch: Boolean,

)