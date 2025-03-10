package marketing.mama.domain.user.dto.request

import jakarta.validation.constraints.Email
import marketing.mama.domain.user.model.Role
import marketing.mama.domain.user.model.Status
import marketing.mama.domain.user.model.User
import marketing.mama.global.validation.ValidPassword
import marketing.mama.global.validation.ValidTlno
import org.springframework.validation.annotation.Validated
import org.springframework.web.multipart.MultipartFile

@Validated
data class SignUpRequest(

    var name: String,

    var nickname: String,

    @field:Email
    var email: String,

    @field: ValidPassword
    var password: String,

    @field: ValidPassword
    var confirmpassword:String,

    var introduction: String,

    @field: ValidTlno
    var tlno: String,

    var role: Role,

    var profilePicUrl: MutableList<MultipartFile> = mutableListOf()
){

    fun to(): User {
        return User(
            role = role,
            name = name,
            email = email,
            password = password,
            introduction = introduction,
            tlno = tlno,
            status = Status.NORMAL,
            nickname = nickname,
        )
    }
}
