package marketing.mama.global.exception

import org.springframework.http.HttpStatus

class CustomException(
    override val message: String,
    val status: HttpStatus = HttpStatus.BAD_REQUEST
) : RuntimeException(message)