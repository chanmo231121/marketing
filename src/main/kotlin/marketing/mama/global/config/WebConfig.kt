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

/*    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        val converter = MappingJackson2HttpMessageConverter()
        converter.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        converters.add(converter)
        super.configureMessageConverters(converters)
    }*/


    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins(
                "http://localhost:9000",
                "http://localhost:8080",
                "http://87d6-123-214-67-61.ngrok-free.app") // 프론트엔드 도메인 허용
            .allowedMethods("*") // 모든 HTTP 메서드 허용
            .allowedHeaders("*") // 모든 헤더 허용
            .allowCredentials(true) // 자격 증명 허용 (예: 쿠키, 인증)
            .exposedHeaders("Authorization","Date")
    }
}
