package marketing.mama.infra.security


import marketing.mama.infra.security.jwt.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val authenticationEntryPoint: AuthenticationEntryPoint,

) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .csrf { it.disable() }
            .cors { }
            .headers {

                it.frameOptions { foc -> foc.disable() }
                it.contentSecurityPolicy { csp -> csp.policyDirectives("default-src * 'unsafe-inline' 'unsafe-eval' data: blob:") }
            }


            .authorizeHttpRequests {
                it.requestMatchers(

                    "/api/v1/users/login",
                    "/api/v1/users/signup",
                    "/api/v1/refresh",
                    "/h2-console/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/api/admin/logs/custom",
                    "/api/v1/boards/**",
                    "/api/v1/banner",
                    "/api/keyword-mix/**",
                    "/api/code"
                    ).permitAll()
                    // 위 URI를 제외하곤 모두 인증이 되어야 함.
                    .anyRequest().authenticated()
            }
            // 기존 UsernamePasswordAuthenticationFilter 가 존재하던 자리에 JwtAuthenticationFilter 적용
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint)
            }
            .build()
    }

}
