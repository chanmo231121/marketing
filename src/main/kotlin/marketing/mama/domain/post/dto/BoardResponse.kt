// BoardResponse.kt

package marketing.mama.domain.post.dto

import marketing.mama.domain.post.model.Board
import java.time.ZonedDateTime

data class BoardResponse(
    val id: Long,
    val title: String,
    val content: String,
    val name: String,        // 작성자의 nickname
    val createdAt: ZonedDateTime,  // 수정: creatAt -> createdAt
) {
    companion object {
        fun from(board: Board): BoardResponse {
            return BoardResponse(
                id = board.id!!,
                title = board.title,
                content = board.content,
                name = board.nickName,   // 작성자 nickname
                createdAt = board.createdAt // 게시글 생성일시
            )
        }
    }
}
