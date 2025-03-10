package marketing.mama.infra.security


import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import marketing.mama.global.exception.ErrorResponse
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component


@Component
class CustomAuthenticationEntryPoint: AuthenticationEntryPoint{
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
       response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"

        val objectMapper = ObjectMapper()
        val jsonString = objectMapper.writeValueAsString(ErrorResponse("JWT verification failed"))
        response.writer.write(jsonString)

    }
}