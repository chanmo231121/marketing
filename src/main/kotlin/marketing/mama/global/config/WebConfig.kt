package marketing.mama.global.config

import com.fasterxml.jackson.databind.SerializationFeature
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
class WebConfig(
): WebMvcConfigurer {


    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins(
                "http://localhost:9000",
                "http://localhost:8080",
                "http://lohosttest.s3-website.ap-northeast-2.amazonaws.com",
                "http://maglo4.s3-website.ap-northeast-2.amazonaws.com",
                "http://maglo6.s3-website.ap-northeast-2.amazonaws.com",
                "http://43.203.93.162:8080",
                "https://maglo.kr",
                "https://www.maglo.kr",
                "https://api.maglo.kr"
                )

            .allowedMethods("*") // 모든 HTTP 메서드 허용
            .allowedHeaders("*") // 모든 헤더 허용
            .allowCredentials(true) // 자격 증명 허용 (예: 쿠키, 인증)
            .exposedHeaders("Authorization","Date", "X-New-Access-Token")
    }
}
