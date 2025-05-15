package marketing.mama.domain.activitylog.model

enum class ActionType(val label: String) {
    단일검색("단일 검색"),
    랭킹검색("랭킹 검색"),
    키워드조합("키워드 조합기"),
    연관검색("연관 검색"),
    로그인("로그인"),
    쇼핑검색 ("쇼핑검색"),
    트렌드검색 ("트렌드검색")

}