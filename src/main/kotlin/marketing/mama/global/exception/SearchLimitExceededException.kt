package marketing.mama.global.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
class SearchLimitExceededException(message: String) : RuntimeException(message)