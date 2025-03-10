package marketing.mama.infra.security.jwt


import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import marketing.mama.global.exception.TokenExpiredException
import marketing.mama.infra.security.UserPrincipal
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter


@Component
class JwtAuthenticationFilter(
    private val jwtPlugin: JwtPlugin
) : OncePerRequestFilter() {

    companion object {
        private val BEARER_PATTERN = Regex("^Bearer (.+?)$")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val jwt = request.getBearerToken()

        if (jwt != null) {
            try {
                jwtPlugin.validateToken(jwt)
                    .onSuccess {
                        val userId = it.payload.subject.toLong()
                        val role = it.payload.get("role", String::class.java)
                        val email = it.payload.get("email", String::class.java)

                        val principal = UserPrincipal(
                            id = userId,
                            email = email,
                            roles = setOf(role)
                        )
                        // Authentication 구현체 생성
                        val authentication = JwtAuthenticationToken(
                            principal = principal,
                            // request로 부터 요청 상세정보 생성
                            details =  WebAuthenticationDetailsSource().buildDetails(request)
                        )
                        // SecurityContext에 authentication 객체 저장
                        SecurityContextHolder.getContext().authentication = authentication
                    }
            } catch (e: TokenExpiredException) {
                // 토큰 만료 시 로그인을 다시 요청하라는 401 에러를 반환
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "로그인을 다시 해주세요.")
                return
            } catch (e: Exception) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증 오류가 발생했습니다.")
                return
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun HttpServletRequest.getBearerToken(): String? {
        val headerValue = this.getHeader(HttpHeaders.AUTHORIZATION) ?: return null
        return BEARER_PATTERN.find(headerValue)?.groupValues?.get(1)
    }
}