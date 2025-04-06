package marketing.mama.domain.refreshToken.service

import jakarta.servlet.http.HttpServletResponse
import marketing.mama.domain.refreshToken.model.RefreshToken
import marketing.mama.domain.refreshToken.repository.RefreshTokenRepository
import marketing.mama.domain.user.model.User
import marketing.mama.infra.security.UserPrincipal
import marketing.mama.infra.security.jwt.JwtPlugin
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service



@Service
class RefreshTokenServiceImpl(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtPlugin: JwtPlugin,
) : RefreshTokenService {

    override fun saveRefreshToken(user: User, refreshToken: String) {
        refreshTokenRepository.save(RefreshToken(user = user, token = refreshToken))
    }

    override fun findRefreshToken(user: User): String? {
        val refreshTokenOptional = refreshTokenRepository.findByUser(user)
        return refreshTokenOptional.orElse(null)?.token
    }

    override fun refreshAccessToken(refreshToken: String?, response: HttpServletResponse): String {
        if (refreshToken.isNullOrEmpty()) throw RuntimeException("리프레시 토큰 없음")

        val refreshEntity = refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow { RuntimeException("해당 리프레시 토큰 없음") }

        val user = refreshEntity.user
        val newAccessToken = jwtPlugin.generateAccessToken(user.id.toString(), user.email, user.role.name)

        // 새 access token을 헤더로 추가
        response.setHeader("X-New-Access-Token", newAccessToken)
        return newAccessToken
    }
}


