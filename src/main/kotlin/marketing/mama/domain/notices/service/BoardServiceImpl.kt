package marketing.mama.domain.notices.service

import marketing.mama.domain.notices.dto.BoardResponse
import marketing.mama.domain.notices.dto.BoardRequest
import marketing.mama.domain.notices.model.Board
import marketing.mama.domain.notices.repository.BoardRepository
import marketing.mama.domain.user.model.Role
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

    override fun getAllBoards(): List<BoardResponse> {
        val boards = boardRepository.findAll().sortedWith(
            compareByDescending<Board> { it.isPinned }.thenByDescending { it.createdAt }
        )
        return boards.map { BoardResponse.from(it) }
    }

    // 게시글 수정
    override fun updateBoard(boardId: Long, boardRequest: BoardRequest, userPrincipal: UserPrincipal): BoardResponse {
        val user = userRepository.findByIdOrNull(userPrincipal.id)
            ?: throw ModelNotFoundException("user", userPrincipal.id)

        val board = boardRepository.findById(boardId)
            .orElseThrow { ModelNotFoundException("board", boardId) }

        // ✅ DEV는 모두 수정 가능
        if (user.role == Role.DEV) {
            // 통과
        }
        // ✅ ADMIN은 자기 글만 수정 가능
        else if (user.role == Role.ADMIN) {
            if (board.user.id != user.id) {
                throw UserNotMatchedException("ADMIN은 본인이 작성한 글만 수정할 수 있습니다.")
            }
        }
        // 🚫 그 외는 수정 불가
        else {
            throw UserNotMatchedException("수정 권한이 없습니다.")
        }

        // 게시글 수정
        board.title = boardRequest.title
        board.content = boardRequest.content
        board.isPinned = boardRequest.isPinned

        return BoardResponse.from(boardRepository.save(board))
    }

    // 게시글 삭제
    override fun deleteBoard(boardId: Long, userPrincipal: UserPrincipal): String {
        // 로그인 유저 정보 조회
        val user = userRepository.findByIdOrNull(userPrincipal.id)
            ?: throw ModelNotFoundException("user", userPrincipal.id)

        // 게시글 조회
        val board = boardRepository.findByIdOrNull(boardId)
            ?: throw ModelNotFoundException("board", boardId)

        // DEV는 모든 글 삭제 가능
        if (user.role == Role.DEV) {
            boardRepository.delete(board)
            return "게시글 삭제 완료"
        }

        // ADMIN은 본인 글만 삭제 가능
        if (user.role == Role.ADMIN) {
            if (board.user.id != user.id) {
                throw UserNotMatchedException("ADMIN은 본인이 작성한 글만 삭제할 수 있습니다.")
            }
            boardRepository.delete(board)
            return "게시글 삭제 완료"
        }

        // 그 외 권한 없음
        throw UserNotMatchedException("삭제 권한이 없습니다.")
    }
}