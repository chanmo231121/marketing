package marketing.mama.domain.notices.dto

import marketing.mama.domain.notices.model.Board
import marketing.mama.domain.user.model.User

data class BoardRequest(
    val title: String,
    val content: String,
    val isPinned: Boolean // 추가

) {
    // userPrincipal에서 받은 user 정보를 기반으로 Board를 생성
    fun to(user: User): Board {
        return Board(
            title = this.title,
            content = this.content,
            user = user,
            name = user.name, // user의 nickname을 board에 담기
            isPinned = this.isPinned // 반영

        )
    }
}