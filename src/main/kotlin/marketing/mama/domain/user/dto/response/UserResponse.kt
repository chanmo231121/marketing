package marketing.mama.domain.user.dto.response

import marketing.mama.domain.user.model.User


data class UserResponse(

    var id: Long,

    var name: String,

    var nickname:String,

    var email: String,

    var introduction: String,

    var tlno: String,

    var role: String,

//    var profilePicUrl: MutableList<String>,
){

    companion object{
        fun from(user: User) = UserResponse(
            id = user.id!!,
            name = user.name,
            email = user.email,
            introduction = user.introduction,
            tlno = user.tlno,
            role = user.role.name,
            //profilePicUrl = user.profilePicUrl,
            nickname = user.name
        )
    }
}
