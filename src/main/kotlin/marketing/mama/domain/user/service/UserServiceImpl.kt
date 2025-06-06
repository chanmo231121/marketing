package marketing.mama.domain.user.service


import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.transaction.Transactional
import marketing.mama.domain.refreshToken.model.RefreshToken
import marketing.mama.domain.refreshToken.repository.RefreshTokenRepository
import marketing.mama.domain.user.dto.request.LoginRequest
import marketing.mama.domain.user.dto.request.SignUpRequest
import marketing.mama.domain.user.dto.request.UpdateUserProfileRequest
import marketing.mama.domain.user.dto.response.LoginResponse
import marketing.mama.domain.user.dto.response.UserResponse
import marketing.mama.domain.user.model.Role
import marketing.mama.domain.user.model.Status
import marketing.mama.domain.user.model.User
import marketing.mama.domain.user.repository.UserRepository
import marketing.mama.global.exception.CustomException
import marketing.mama.global.exception.ModelNotFoundException
import marketing.mama.global.exception.WithdrawalCancellationException
import marketing.mama.infra.security.UserPrincipal
import marketing.mama.infra.security.jwt.JwtPlugin
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseCookie
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service


@Service
class UserServiceImpl(

    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val refreshTokenRepository: RefreshTokenRepository,
//    private val userRecentPasswordsRepository: UserRecentPasswordsRepository,
    private val jwtPlugin: JwtPlugin,
    /*    private val slangFilterService: SlangFilterService,
        private val s3Service: S3Service,
        private val emailService: EmailService,

        private val smsSender: SMSSender,
        private val boardRepository: BoardRepository,
        private val commentRepository:CommentRepository,
        private val boardLikeUpUserRepository: BoardLikeUpUserRepository*/

) : UserService {

    override fun login(
        request: LoginRequest,
        response: HttpServletResponse
    ): LoginResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("이메일 또는 비밀번호를 확인해주세요.")

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw IllegalArgumentException("이메일 또는 비밀번호를 확인해주세요.")
        }

        if (user.role == Role.ADMIN && user.status == Status.WAITING) {
            throw IllegalStateException("관리자는 별도 승인이 필요합니다. 로그인할 수 없습니다.")
        }

        val accessToken = jwtPlugin.generateAccessToken(
            subject = user.id.toString(),
            email = user.email,
            role = user.role.name
        )
        jwtPlugin.removeTokenFromBlacklist(accessToken)

        val refreshToken = jwtPlugin.generateRefreshToken(
            subject = user.id.toString(),
            email = user.email,
            role = user.role.name
        )
        refreshTokenRepository.save(RefreshToken(user = user, token = refreshToken))

        // ✅ 보안 쿠키로 refresh token 저장
        val cookie = ResponseCookie.from("refresh_token", refreshToken)
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .domain("maglo.kr") // ✅ 명시 필요!
            .path("/")
            .maxAge(7 * 24 * 60 * 60)
            .build()

        response.addHeader("Set-Cookie", cookie.toString())
        response.setHeader("Authorization", "Bearer $accessToken")

        return LoginResponse(
            name = user.name,
            role = user.role,
            status = user.status,
            approvedUntil = user.approvedUntil,
            id = user.id!!,
            canUseSingleSearch = user.canUseSingleSearch,
            canUseRankingSearch = user.canUseRankingSearch,
            canUseKeywordMix = user.canUseKeywordMix,
            canUseRelatedSearch = user.canUseRelatedSearch,
            canUseShoppingSearch = user.canUseShoppingSearch,
            canUseTrendSearch = user.canUseTrendSearch

        )
    }

    override fun logout(response: HttpServletResponse, request: HttpServletRequest) {
        val accessToken = jwtPlugin.extractAccessTokenFromRequest(request)
        val refreshToken = request.cookies?.firstOrNull { it.name == "refresh_token" }?.value

        // ✅ access token 블랙리스트 등록
        jwtPlugin.invalidateToken(accessToken)

        // ✅ access token 쿠키 삭제
        jwtPlugin.deleteAccessTokenCookie(response)

        // ✅ refresh token 쿠키 삭제
        val refreshTokenCookie = Cookie("refresh_token", null)
        refreshTokenCookie.path = "/"
        refreshTokenCookie.maxAge = 0
        response.addCookie(refreshTokenCookie)

        // ✅ refresh token DB에서 제거
        refreshToken?.let {
            refreshTokenRepository.findByToken(it).ifPresent { entity ->
                refreshTokenRepository.delete(entity)
            }
        }
    }
    
    override fun getUserByEmail(email: String): User {
        return userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("해당 이메일로 사용자를 찾을 수 없습니다: $email")
    }

/*


    override fun withdrawal(userId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("해당 회원을 찾을 수 없습니다.") }
        user.status = Status.WITHDRAWAL
        userRepository.save(user)
    }

    override fun cancelWithdrawal(userId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("해당 회원을 찾을 수 없습니다.") }
        user.status = Status.NORMAL
        userRepository.save(user)
    }

    @Transactional
    override fun updatePassword(request: UpdateUserPasswordRequest) {
        val authenticatedUserId: Long = (SecurityContextHolder.getContext().authentication.principal as? UserPrincipal)?.id
            ?: throw AuthenticationCredentialsNotFoundException("사용자 인증이 필요합니다.")
        // 기존 비밀번호 확인
        val user = userRepository.findById(authenticatedUserId)
            .orElseThrow { ModelNotFoundException("user", authenticatedUserId) }
        val provider = user.provider
        if (user.status == Status.WITHDRAWAL) {
            throw IllegalStateException("탈퇴한 사용자는 프로필을 수정할 수 없습니다.")
        }
        if (isSocialUser(provider)) {
            throw IllegalStateException("소셜 로그인 사용자는 비밀번호를 수정할 수 없습니다.")
        }
        if (!passwordEncoder.matches(request.userPassword, user.password)) {
            throw InvalidCredentialException("기존 비밀번호가 일치하지 않습니다.")
        }
        val newPasswordHash = passwordEncoder.encode(request.userNewPassword)
        val recentPasswords = userRecentPasswordsRepository.findTop3ByUserOrderByIdDesc(user)
        if (recentPasswords.any { passwordEncoder.matches(request.userNewPassword, it.password) }) {
            throw IllegalArgumentException("최근 3번 사용한 비밀번호는 사용할 수 없습니다.")
        }
        val recentPassword = UserRecentPasswords(newPasswordHash, user)
        userRecentPasswordsRepository.save(recentPassword)
        user.password = newPasswordHash
        userRepository.save(user)
    }
    private fun isSocialUser(provider: OAuth2Provider?): Boolean {
        return provider != null && (provider == OAuth2Provider.KAKAO || provider == OAuth2Provider.NAVER || provider == OAuth2Provider.GOOGLE)
    }*/


    @Transactional
    override fun signUp(request: SignUpRequest): UserResponse {

        if (userRepository.existsByEmail(request.email)) {
            throw IllegalStateException("이메일이 이미 사용중입니다.")
        }
        if (request.password != request.confirmpassword) {
            throw IllegalArgumentException("비밀번호와 확인 비밀번호가 일치하지 않습니다.")
        }

        val hashedPassword = passwordEncoder.encode(request.password)

        val user = request.to(hashedPassword)

        val savedUser = userRepository.save(user)
        return UserResponse.from(savedUser)
    }



    override fun getUserProfile(userId: Long): UserResponse {
        // 여기서는 이미 컨트롤러에서 인증된 사용자의 ID를 넘겨받았으므로 추가 인증이 필요 없습니다.

        // 사용자 조회 및 상태 체크
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("해당 사용자를 찾을 수 없습니다.") }
        if (user.status == Status.WITHDRAWAL) {
            throw WithdrawalCancellationException("탈퇴한 사용자입니다. 탈퇴취소를 해주세요.")
        }
        return UserResponse.from(user)
    }

    @Transactional
    override fun updateUserProfile(
        request: UpdateUserProfileRequest
    ): UserResponse {
        val authenticatedId: Long = (SecurityContextHolder.getContext().authentication.principal as? UserPrincipal)?.id
            ?: throw IllegalStateException("로그인을 먼저 하세요.")

        val user = userRepository.findByIdOrNull(authenticatedId)
            ?: throw ModelNotFoundException("User", authenticatedId)

        if (user.status == Status.WITHDRAWAL) {
            throw IllegalStateException("탈퇴한 사용자는 프로필을 수정할 수 없습니다.")
        }

        user.name = request.name
        user.introduction = request.introduction
        user.tlno = request.tlno

        return UserResponse.from(user)
    }

    @Transactional
    override fun updateReceiveLogEmail(userId: Long, receive: Boolean) {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("존재하지 않는 유저입니다.") }
        user.receiveLogEmail = receive
    }

    @Transactional
    override fun requestDeviceApproval(userId: Long, deviceId: String): String {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }

        // 👉 다른 유저가 이미 이 deviceId를 가지고 있는 경우만 막기 (자기 자신이면 OK)
        val exists = userRepository.findByDeviceId(deviceId)
        if (exists != null && exists.id != user.id) {
            throw IllegalStateException("이미 다른 계정에서 승인된 기기입니다. 관리자에게 문의해주세요.")
        }

        // ✅ 기기ID가 없거나 다르다면 업데이트 + 상태를 대기상태로
        if (user.deviceId != deviceId) {
            user.deviceId = deviceId
            user.status = Status.PENDING_APPROVAL
            userRepository.save(user)
            return "기기 승인 요청이 완료되었습니다. 관리자 승인을 기다려주세요."
        }

        // ✅ 이미 승인 요청한 상태라면 안내 메시지
        return when (user.status) {
            Status.PENDING_APPROVAL -> "이미 기기 승인 요청을 보낸 상태입니다."
            Status.NORMAL -> "이 기기는 이미 승인된 상태입니다."
            else -> {
                user.status = Status.PENDING_APPROVAL
                userRepository.save(user)
                "기기 승인 요청이 완료되었습니다. 관리자 승인을 기다려주세요."
            }
        }
    }

    override fun validateDevice(user: User, currentDeviceId: String?) {
        if (user.role == Role.ADMIN || user.role == Role.DEV) return

        if (user.deviceId == null || currentDeviceId.isNullOrBlank()) {
            throw IllegalStateException("⛔ 기기 승인 요청이 필요합니다.")
        }

        // ② header 값이 있고, 기기 ID가 불일치할 때
        if (user.deviceId != currentDeviceId) {
            throw IllegalStateException("⚠️ 기기 불일치. 재승인을 요청해주세요.")
        }

    }
/*




    override fun sendPasswordResetCode(email: String, phoneNumber: String): Boolean {
        val user = userRepository.findByEmailAndTlno(email, phoneNumber)
            ?: throw IllegalArgumentException("이메일 혹은 핸드폰번호가 일치하지 않습니다.")
        val passwordCode = UUID.randomUUID().toString().substring(0, 6)
        val internationalPhoneNumber = "82" + phoneNumber.replace("-", "")
        val message = "인증코드🗝️ $passwordCode\n 코드를 이용하여 임시비밀번호를 받으세용"
        smsSender.sendSMS(internationalPhoneNumber, message)
        userRepository.save(user.apply { this.passwordCode = passwordCode })
        return true
    }

    override fun temporaryPassword(email: String, phoneNumber: String, passwordCode: String): String {
        val user = userRepository.findByEmailAndTlno(email, phoneNumber)
            ?: throw IllegalArgumentException("유효하지 않은 인증 코드입니다.")
        if (user.passwordCode != passwordCode) {
            throw IllegalArgumentException("유효하지 않은 인증 코드입니다.")
        }
        val passwordLength = 10
        val specials = "!@#$%^&*("
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val specialChar = specials.random()
        val lowerChar = chars.filter { it.isLowerCase() }.random()
        val upperChar = chars.filter { it.isUpperCase() }.random()
        val digitChar = chars.filter { it.isDigit() }.random()
        val passwordChars = buildString {
            append(specialChar)
            append(lowerChar)
            append(upperChar)
            append(digitChar)
            repeat(passwordLength - 4) {
                append(chars.random())
            }
        }
        val savedUser = userRepository.save(user.apply {
            verificationCode = UUID.randomUUID().toString().substring(0, 6)
            password = passwordEncoder.encode(passwordChars)
        })
        savedUser.verificationCode?.let { emailService.sendVerificationEmail(savedUser.email, it, passwordChars) }
        return passwordChars
    }

    override fun getUserBoards(authenticatedId: Long): List<BoardDto> {
        val boards = boardRepository.findByUserId(authenticatedId)
        return boards.map { BoardDto.from(it) }
    }
    override fun getUserComments(authenticatedId: Long): List<CommentDto> {
        val comments = commentRepository.findByUserId(authenticatedId)
        return comments.map { CommentDto.from(it) }
    }
    override fun getLikedBoardsByUserId(authenticatedId: Long): List<LikedBoardDto> {
        val likedBoardUsers = boardLikeUpUserRepository.findByUserId(authenticatedId)
        return likedBoardUsers.map { LikedBoardDto.from(it.board) }
    }

    override fun getUserBoardDetails(authenticatedId: Long, boardId: Long): BoardDto {
        val board = boardRepository.findById(boardId)
            .orElseThrow { IllegalArgumentException("게시글을 찾을 수 없습니다. ID: $boardId") }
        return BoardDto.from(board)
    }

    override  fun getUserProfilePic(userId: Long): MutableList<String>{
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("해당 ID의 사용자를 찾을 수 없습니다.") }
        return user.profilePicUrl
    }


*/


}

