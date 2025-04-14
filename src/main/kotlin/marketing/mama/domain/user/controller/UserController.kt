package marketing.mama.domain.user.controller

import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import marketing.mama.domain.activitylog.service.SearchLogService
import marketing.mama.domain.user.dto.request.LoginRequest
import marketing.mama.domain.user.dto.request.ReceiveLogEmailRequest
import marketing.mama.domain.user.dto.request.SignUpRequest
import marketing.mama.domain.user.dto.request.UpdateUserProfileRequest
import marketing.mama.domain.user.dto.response.LoginResponse
import marketing.mama.domain.user.dto.response.UserResponse
import marketing.mama.domain.user.repository.UserRepository
import marketing.mama.domain.user.service.UserServiceImpl
import marketing.mama.infra.security.UserPrincipal
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.*


@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserServiceImpl,
    private val userRepository: UserRepository,
    private val searchLogService: SearchLogService
) {


    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    fun signUp(@Valid @RequestBody request: SignUpRequest): ResponseEntity<UserResponse> {
        val user = userService.signUp(request)
        return ResponseEntity.ok(user)
    }


    @Operation(summary = "로그인")
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody loginRequest: LoginRequest,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<LoginResponse> {
        val loginResponse = userService.login(loginRequest, response)

        val user = userRepository.findByEmail(loginRequest.email)
            ?: throw IllegalArgumentException("사용자를 찾을 수 없습니다.")

        searchLogService.logLogin(
            user = user,
            ip = request.remoteAddr,
        )

        return ResponseEntity.status(HttpStatus.OK).body(loginResponse)
    }

    @Operation(summary = "프로필 조회")
    @GetMapping("/profile")
    fun getProfile(): ResponseEntity<UserResponse> {
        // SecurityContext에서 인증된 사용자 정보 가져오기
        val authenticatedId: Long = (SecurityContextHolder.getContext().authentication.principal as? UserPrincipal)?.id
            ?: throw AuthenticationCredentialsNotFoundException("사용자 인증이 필요합니다.") // 인증되지 않은 경우

        // 인증된 사용자 ID로 사용자 프로필 조회
        val userProfile = userService.getUserProfile(authenticatedId)

        return ResponseEntity.ok(userProfile)
    }


    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @AuthenticationPrincipal principal: UserPrincipal // ✅ 인증 사용자 주입
    ): ResponseEntity<String> {
        userService.logout(response, request)
        return ResponseEntity.ok("로그아웃 되었습니다.")
    }

    @Operation(summary = "프로필 수정")
    @PutMapping("/profile")
    fun updateProfile(
        @Valid @RequestBody request: UpdateUserProfileRequest
    ): ResponseEntity<UserResponse> {
        val updatedUser = userService.updateUserProfile(request)
        return ResponseEntity.ok(updatedUser)
    }

    @PutMapping("/{id}/email-log-setting")
    fun updateReceiveLogEmail(
        @PathVariable id: Long,
        @RequestBody request: ReceiveLogEmailRequest
    ): ResponseEntity<String> {
        userService.updateReceiveLogEmail(id, request.receiveLogEmail)
        val message = if (request.receiveLogEmail) {
            "이제 이 관리자는 메일을 받습니다."
        } else {
            "이제 이 관리자는 메일을 받지 않습니다."
        }
        return ResponseEntity.ok(message)
    }

    @Operation(summary = "기기 승인 요청")
    @PostMapping("/device-approval/request")
    fun requestDeviceApproval(
        @RequestBody payload: Map<String, String>,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<Map<String, String>> {
        val deviceId = payload["deviceId"]
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to "deviceId가 필요합니다."))

        val message = userService.requestDeviceApproval(principal.id, deviceId)
        return ResponseEntity.ok(mapOf("message" to message))
    }

    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<UserResponse> {
        val email = principal.email  // username 말고 email로 접근
        val user = userService.getUserByEmail(email)
        return ResponseEntity.ok(UserResponse.from(user))
    }

}

