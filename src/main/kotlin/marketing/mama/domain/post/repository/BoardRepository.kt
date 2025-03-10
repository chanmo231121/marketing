package marketing.mama.domain.post.repository

import marketing.mama.domain.post.model.Board
import marketing.mama.domain.user.model.User
import org.springframework.data.jpa.repository.JpaRepository

interface BoardRepository : JpaRepository<Board, Long> {
    // 특정 userId와 boardId로 게시글 조회
    fun findByIdAndUserId(boardId: Long, userId: Long): Board?

    // 특정 유저가 작성한 게시글 목록 조회
    fun findByUser(user: User): List<Board>

    // 특정 유저Id로 작성한 게시글 목록 조회
    fun findByUserId(userId: Long): List<Board>
}
