package marketing.mama.infra.security.jwt


import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.util.WebUtils
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant

@Component
class JwtPlugin(
    @Value("\${auth.jwt.issuer}")
    private val issuer: String,
    @Value("\${auth.jwt.secret}")
    private val secret: String,
    @Value("\${auth.jwt.accessTokenExpirationMinutes}")
    private val accessTokenExpirationMinutes: Long,
    @Value("\${auth.jwt.refreshTokenExpirationDays}")
    private val refreshTokenExpirationDays: Long,
    private val blacklistedTokens: MutableSet<String> = HashSet(),
) {


    fun validateToken(jwt: String): Result<Jws<Claims>> {
        return kotlin.runCatching {
            val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
            Jwts.parser().verifyWith(key).build().parseSignedClaims(jwt)
        }
    }

    fun generateAccessToken(subject: String, email: String, role: String): String {
        return generateToken(subject, email, role, Duration.ofMinutes(accessTokenExpirationMinutes))
    }

    fun generateRefreshToken(subject: String, email: String, role: String): String {
        return generateToken(subject, email, role, Duration.ofDays(refreshTokenExpirationDays))
    }

    fun extractAccessTokenFromRequest(request: HttpServletRequest): String {
        val authorizationHeader = request.getHeader("Authorization")
        return if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            authorizationHeader.substring(7)
        } else {
            val accessTokenCookie = WebUtils.getCookie(request, "access_token")
            accessTokenCookie?.value ?: throw RuntimeException("Access token not found in request.")
        }
    }




    fun removeTokenFromBlacklist(token: String) {
        synchronized(blacklistedTokens) {
            blacklistedTokens.remove(token)
        }
    }


    // 쿠키 삭제
    fun deleteAccessTokenCookie(response: HttpServletResponse) {
        val accessTokenCookie = Cookie("access_token", null)
        accessTokenCookie.path = "/"
        accessTokenCookie.maxAge = 0
        response.addCookie(accessTokenCookie)
    }

    // 주어진 토큰을 블랙리스트에 추가
    fun invalidateToken(token: String) {
        synchronized(blacklistedTokens) {
            blacklistedTokens.add(token)
        }
    }

    //테스트용으로 로그아웃 후 블랙리스트에 토큰이 저장되었는지 확인용
    fun getBlacklistedTokens(): Set<String> {
        synchronized(blacklistedTokens) {
            return blacklistedTokens.toSet()
        }
    }


    private fun generateToken(subject: String, email: String, role: String, expirationPeriod: Duration): String {
        val claims: Claims = Jwts.claims()
            .add(mapOf("role" to role, "email" to email))
            .build()

        val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
        val now = Instant.now()

        return Jwts.builder()
            .subject(subject)
            .issuer(issuer)
            .issuedAt(java.util.Date.from(now))
            .expiration(java.util.Date.from(now.plus(expirationPeriod)))
            .claims(claims)
            .signWith(key)
            .compact()
    }
}

