package marketing.mama.domain.user.controller

import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import marketing.mama.domain.user.dto.request.LoginRequest
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
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserServiceImpl,
    private val userRepository: UserRepository,
) {


    @Operation(summary = "회원가입")
    @PostMapping("/signup",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun signUp(
        @Valid
        @ModelAttribute signUpRequest: SignUpRequest
    ): ResponseEntity<UserResponse> {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(userService.signUp(signUpRequest))
    }


    @Operation(summary = "로그인")
    @PostMapping("/login")
    fun login(
        @Valid
        @RequestBody loginRequest: LoginRequest, response: HttpServletResponse
    ): ResponseEntity<LoginResponse> {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(userService.login(loginRequest,response))
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
    fun logout(request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<String> {
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

}