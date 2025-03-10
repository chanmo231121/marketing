package marketing.mama.domain.refreshToken.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import marketing.mama.domain.user.model.User



interface RefreshTokenService {

    fun saveRefreshToken(user: User, refreshToken: String)
    fun findRefreshToken(user: User): String?
    fun refreshAccessToken(refreshToken: String?, response: HttpServletResponse)
}