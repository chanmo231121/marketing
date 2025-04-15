// BoardResponse.kt

package marketing.mama.domain.notices.dto

import marketing.mama.domain.notices.model.Board
import java.time.ZonedDateTime

data class BoardResponse(
    val id: Long,
    val title: String,
    val content: String,
    val name: String,        // 작성자의 nickname
    val createdAt: ZonedDateTime,  // 수정: creatAt -> createdAt
    val isPinned: Boolean // 추가

) {
    companion object {
        fun from(board: Board): BoardResponse {
            return BoardResponse(
                id = board.id!!,
                title = board.title,
                content = board.content,
                name = board.name,   // 작성자 nickname
                createdAt = board.createdAt, // 게시글 생성일시
                isPinned = board.isPinned // 포함

            )
        }
    }
}
