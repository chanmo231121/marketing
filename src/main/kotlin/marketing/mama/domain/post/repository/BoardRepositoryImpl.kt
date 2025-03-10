/*
package marketing.mama.domain.post.repository

import org.springframework.stereotype.Repository
import org.team.b4.cosmicadventures.domain.community.model.Board
import org.team.b4.cosmicadventures.domain.community.model.QBoard
import org.team.b4.cosmicadventures.global.querydsl.QueryDslSupport

@Repository
class BoardRepositoryImpl : CustomBoardRepository, QueryDslSupport() {
    private val board = QBoard.board

    // 작성일 순서를 오름차순으로 목록조회
    override fun getBoardByCreateAt(): List<Board> {
        return queryFactory.selectFrom(board)
            .orderBy(board.createdAt.asc())
            .fetch()
    }
    // 좋아요 순으로 목록조회
    override fun getBoardByLikeUp(): List<Board> {
        return queryFactory.selectFrom(board)
            .limit(10)
            .orderBy(board.likeCount.desc())
            .fetch()
    }

}*/
