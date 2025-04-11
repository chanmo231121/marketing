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
        @CookieValue("refresh_token") refreshToken: String?, // ✅ 쿠키에서 읽기
        response: HttpServletResponse
    ): ResponseEntity<String> {
        if (refreshToken.isNullOrBlank()) {
            return ResponseEntity.badRequest().body("리프레시 토큰 없음")
        }

        val newToken = refreshTokenService.refreshAccessToken(refreshToken, response)

        return ResponseEntity.ok()
            .header("X-New-Access-Token", newToken)
            .body("새로운 토큰 발급 완료")
    }

}
