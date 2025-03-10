package marketing.mama

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient

@SpringBootApplication
class MamaApplication


fun main(args: Array<String>) {
    runApplication<MamaApplication>(*args)
}
