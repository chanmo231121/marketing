package marketing.mama.domain.post.service

import marketing.mama.domain.post.dto.BoardResponse
import marketing.mama.domain.post.dto.BoardRequest
import marketing.mama.infra.security.UserPrincipal

interface BoardService {

    // 게시글 생성
    fun createBoard(boardRequest: BoardRequest, userPrincipal: UserPrincipal): BoardResponse

    // 게시글 수정
    fun updateBoard(boardId: Long, boardRequest: BoardRequest, userPrincipal: UserPrincipal): BoardResponse

    // 게시글 삭제
    fun deleteBoard(boardId: Long, userPrincipal: UserPrincipal): String

    // 게시글 단건조회
    fun getBoard(boardId: Long): BoardResponse
}
