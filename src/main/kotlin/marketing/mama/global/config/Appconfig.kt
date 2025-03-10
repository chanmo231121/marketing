package marketing.mama.global.config;

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate


@Configuration
class AppConfig {

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()  // RestTemplate을 빈으로 등록
    }
}