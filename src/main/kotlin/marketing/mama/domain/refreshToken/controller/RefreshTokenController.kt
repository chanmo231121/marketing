package marketing.mama.domain.refreshToken.controller


import jakarta.servlet.http.HttpServletResponse
import marketing.mama.domain.refreshToken.service.RefreshTokenService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*




@RestController
@RequestMapping("/api/v1")
class RefreshTokenController(
    private val refreshTokenService: RefreshTokenService
) {

    @PostMapping("/refresh")
    fun refreshAccessToken(
        @RequestHeader("Authorization") refreshToken: String,
        response: HttpServletResponse
    ): ResponseEntity<String> {
        val tokenOnly = refreshToken.removePrefix("Bearer ").trim()
        val newToken = refreshTokenService.refreshAccessToken(tokenOnly, response)
        return ResponseEntity.ok()
            .header("X-New-Access-Token", newToken)
            .body("새로운 토큰 발급 완료")
    }


}
