package marketing.mama.domain.user.dto.request

import jakarta.validation.constraints.Email
import marketing.mama.domain.user.model.Role
import marketing.mama.global.validation.ValidPassword
import org.springframework.validation.annotation.Validated

@Validated
data class LoginRequest(

    @field: Email
    val email:String,

    @field: ValidPassword
    val password: String,


//    val verificationCode: String
)
