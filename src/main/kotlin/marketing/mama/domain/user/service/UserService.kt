package marketing.mama.domain.user.service


import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import marketing.mama.domain.user.dto.request.LoginRequest
import marketing.mama.domain.user.dto.request.SignUpRequest
import marketing.mama.domain.user.dto.request.UpdateUserProfileRequest
import marketing.mama.domain.user.dto.response.LoginResponse
import marketing.mama.domain.user.dto.response.UserResponse


interface UserService {

        fun logout(response: HttpServletResponse, request: HttpServletRequest)
        fun signUp(request: SignUpRequest): UserResponse
        fun updateUserProfile(request: UpdateUserProfileRequest): UserResponse
        fun getUserProfile(userId: Long):UserResponse
        fun login(request: LoginRequest, response: HttpServletResponse): LoginResponse
/*
        fun updatePassword(request: UpdateUserPasswordRequest)

        fun withdrawal(userId: Long)

        fun sendPasswordResetCode(email: String, phoneNumber: String): Boolean
        fun temporaryPassword(email: String, phoneNumber: String, passwordCode: String): String
        fun getUserBoards(authenticatedId: Long): List<BoardDto>
        fun getUserComments(authenticatedId: Long): List<CommentDto>
        fun getLikedBoardsByUserId(authenticatedId: Long): List<LikedBoardDto>
        fun getUserBoardDetails(authenticatedId: Long, boardId: Long): BoardDto
        fun getUserProfilePic(userId: Long): MutableList<String>?

        fun cancelWithdrawal(userId: Long)
*/

}
