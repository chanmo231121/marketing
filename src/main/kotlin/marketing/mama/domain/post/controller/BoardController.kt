package marketing.mama.domain.post.controller

import io.swagger.v3.oas.annotations.Operation
import marketing.mama.domain.post.dto.BoardResponse
import marketing.mama.domain.post.dto.BoardRequest
import marketing.mama.domain.post.service.BoardService
import marketing.mama.infra.security.UserPrincipal
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("/boards")
class BoardController(
    private val boardService: BoardService
) {

    @Operation(summary = "게시글 단건조회")
    @GetMapping("/{boardId}")
    fun getBoard(
        @PathVariable boardId: Long
    ): ResponseEntity<BoardResponse> =
        ResponseEntity.ok().body(boardService.getBoard(boardId))

    @Operation(summary = "게시글 작성")
    @PostMapping(
    )
    fun createBoard(
        @RequestBody boardRequest: BoardRequest,
        authentication: Authentication // Authentication 객체 사용
    ): ResponseEntity<BoardResponse> {
        val authenticatedId: Long = (SecurityContextHolder.getContext().authentication.principal as? UserPrincipal)?.id
            ?: throw AuthenticationCredentialsNotFoundException("사용자 인증이 필요합니다.")

        val userPrincipal = UserPrincipal(authenticatedId, "", emptyList())

        val createdBoard = boardService.createBoard(boardRequest, userPrincipal)
        val location = URI.create("/boards/${createdBoard.id}")
        return ResponseEntity.created(location).body(createdBoard)
    }


    @Operation(summary = "게시글 수정")
    @PutMapping(
        "/{boardId}",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun updateBoard(
        @PathVariable boardId: Long,
        @ModelAttribute boardRequest: BoardRequest,
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
