package marketing.mama.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {

    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JavaTimeModule()) // ✅ JavaTime 모듈 직접 등록
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // ✅ 날짜를 ISO 포맷으로 직렬화
    }

    @Bean
    fun jacksonCustomizer(): Jackson2ObjectMapperBuilderCustomizer {
        return Jackson2ObjectMapperBuilderCustomizer { builder ->
            builder.modules(JavaTimeModule())
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}
