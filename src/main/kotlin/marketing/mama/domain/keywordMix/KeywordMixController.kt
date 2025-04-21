package marketing.mama.domain.keywordMix

import marketing.mama.domain.user.repository.UserRepository
import marketing.mama.domain.user.service.UserService
import marketing.mama.infra.security.UserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/keyword-mix")
class KeywordMixController(
    private val userRepository: UserRepository,
    private val userService: UserService
) {

    @GetMapping("/validate-device")
    fun validateDeviceForMix(
        @RequestHeader("X-Device-Id") deviceId: String?,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<Any> {
        val user = userRepository.findById(userPrincipal.id).orElseThrow()
        return try {
            userService.validateDevice(user, deviceId)
            ResponseEntity.ok(mapOf("valid" to true))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(403).body(mapOf("error" to e.message))
        }
    }
}
