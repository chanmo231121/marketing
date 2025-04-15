package marketing.mama.domain.notices.controller

import io.swagger.v3.oas.annotations.Operation
import marketing.mama.domain.notices.dto.BoardResponse
import marketing.mama.domain.notices.dto.BoardRequest
import marketing.mama.domain.notices.service.BoardService
import marketing.mama.infra.security.UserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("/api/v1/boards")
class BoardController(
    private val boardService: BoardService
) {

    @Operation(summary = "게시글 단건조회")
    @GetMapping("/{boardId}")
    fun getBoard(@PathVariable boardId: Long): ResponseEntity<BoardResponse> {
        val board = boardService.getBoard(boardId) // boardId로 게시글 조회
        return ResponseEntity.ok().body(board)
    }

    @Operation(summary = "전체 게시글 조회")
    @GetMapping("/all-boards") // 전체 게시글 조회 엔드포인트
    fun getAllBoards(): ResponseEntity<List<BoardResponse>> {
        val boards = boardService.getAllBoards() // 서비스에서 전체 게시글을 조회
        return ResponseEntity.ok(boards) // 조회된 게시글 반환
    }


    @Operation(summary = "게시글 작성")
    @PostMapping("/create")
    fun createBoard(
        @RequestBody boardRequest: BoardRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal // 로그인된 유저 정보 사용
    ): ResponseEntity<BoardResponse> {
        val createdBoard = boardService.createBoard(boardRequest, userPrincipal)
        val location = URI.create("/boards/${createdBoard.id}")
        return ResponseEntity.created(location).body(createdBoard)
    }

    @Operation(summary = "게시글 수정")
    @PutMapping("/{boardId}")
    fun updateBoard(
        @PathVariable boardId: Long,
        @RequestBody boardRequest: BoardRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<BoardResponse> {
        val updatedBoard = boardService.updateBoard(boardId, boardRequest, userPrincipal)
        return ResponseEntity.ok(updatedBoard)
    }

    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/{boardId}")
    fun deleteBoard(
        @PathVariable boardId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<Void> {
        boardService.deleteBoard(boardId, userPrincipal)
        return ResponseEntity.noContent().build()
    }
}
