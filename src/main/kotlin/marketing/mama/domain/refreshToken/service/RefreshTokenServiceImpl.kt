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

    override fun refreshAccessToken(refreshToken: String?, response: HttpServletResponse) {
        // 리프레시 토큰이 존재하지 않으면 종료
        if (refreshToken.isNullOrEmpty()) {
            return
        }
        // 현재 사용자 정보 가져오기
        val userPrincipal = SecurityContextHolder.getContext().authentication.principal as? UserPrincipal
        // 사용자 정보가 없으면 종료
        if (userPrincipal == null) {
            return
        }
        // 사용자의 정보에서 필요한 정보 추출
        val userId = userPrincipal.id.toString()
        val userEmail = userPrincipal.email
        val userRoles = userPrincipal.authorities.map { it.authority.removePrefix("ROLE_") }
        // 새로운 액세스 토큰 생성
       jwtPlugin.generateAccessToken(userId, userEmail, userRoles.toString())
    }
}


