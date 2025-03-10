package marketing.mama.domain.post.service

import marketing.mama.domain.post.dto.BoardResponse
import marketing.mama.domain.post.dto.BoardRequest
import marketing.mama.domain.post.repository.BoardRepository
import marketing.mama.domain.user.repository.UserRepository
import marketing.mama.global.exception.ModelNotFoundException
import marketing.mama.global.exception.UserNotMatchedException
import marketing.mama.infra.security.UserPrincipal
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class BoardServiceImpl(
    private val boardRepository: BoardRepository,
    private val userRepository: UserRepository
) : BoardService {

    // 게시글 단건조회
    override fun getBoard(boardId: Long): BoardResponse {
        val board = boardRepository.findByIdOrNull(boardId) ?: throw ModelNotFoundException("board", boardId)
        return BoardResponse.from(board)
    }

    // 게시글 작성
    override fun createBoard(boardRequest: BoardRequest, userPrincipal: UserPrincipal): BoardResponse {
        // 사용자 정보 조회
        val user = userRepository.findByIdOrNull(userPrincipal.id)
            ?: throw ModelNotFoundException("user", userPrincipal.id) // 사용자 없으면 예외 처리

        // boardRequest를 Board로 변환 (user 정보를 포함)
        val board = boardRequest.to(user) // 이 메소드가 boardRequest를 Board로 변환

        // 게시글 저장
        val savedBoard = boardRepository.save(board)

        // 저장된 게시글을 BoardResponse로 변환하여 반환
        return BoardResponse.from(savedBoard) // 이 메소드가 savedBoard를 BoardResponse로 변환
    }

    // 게시글 수정
    override fun updateBoard(boardId: Long, boardRequest: BoardRequest, userPrincipal: UserPrincipal): BoardResponse {
        // 로그인 유저가 디비에 있는지 확인
        val user = userRepository.findByIdOrNull(userPrincipal.id) ?: throw ModelNotFoundException("user", userPrincipal.id)

        // 작성자가 맞는지 확인
        val board = boardRepository.findByIdAndUserId(boardId, userPrincipal.id)
            ?: throw UserNotMatchedException()

        // 게시글 수정
        board.title = boardRequest.title
        board.content = boardRequest.content
        board.nickName = user.nickname // 게시글 수정 시 사용자의 닉네임도 갱신
        val updatedBoard = boardRepository.save(board)

        return BoardResponse.from(updatedBoard)
    }

    // 게시글 삭제
    override fun deleteBoard(boardId: Long, userPrincipal: UserPrincipal): String {
        // 로그인 유저가 디비에 있는지 확인
        val user = userRepository.findByIdOrNull(userPrincipal.id) ?: throw ModelNotFoundException("user", userPrincipal.id)

        // 작성자가 맞는지 확인
        val board = boardRepository.findByIdAndUserId(boardId, userPrincipal.id)
            ?: throw UserNotMatchedException()

        // 게시글 삭제
        boardRepository.delete(board)
        return "게시글 삭제 완료"
    }
}
