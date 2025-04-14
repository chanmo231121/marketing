package marketing.mama.domain.user.dto.request

import jakarta.validation.constraints.NotBlank
import marketing.mama.global.validation.ValidTlno

data class UpdateUserProfileRequest(

    @field:NotBlank(message = "이름은 필수입니다.")
    val name: String,

    @field:NotBlank(message = "회사는 필수입니다.")
    val introduction: String,

    @field:ValidTlno
    val tlno: String,


)