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
    fun refreshAccessToken(@RequestHeader("Authorization") refreshToken: String, response: HttpServletResponse): ResponseEntity<String> {
        refreshTokenService.refreshAccessToken(refreshToken, response)
        return ResponseEntity.ok().build()
    }


}
