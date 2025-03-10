package marketing.mama.domain.user.model

import jakarta.persistence.*
import marketing.mama.global.entity.BaseEntity
import marketing.mama.global.exception.StringMutableListConverter

@Entity
@Table(name = "app_user")
class User(

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "nickname", nullable = false)
    var nickname: String,

    @Column(name = "email", nullable = false)
    val email: String,

    @Column(name = "password", nullable = false)
    var password: String,

    @Column(name = "introduction", nullable = false)
    var introduction: String,

    @Column(name = "tlno")
    var tlno: String,

    @Column(name = "profile_pic_url", columnDefinition = "TEXT")
    @Convert(converter = StringMutableListConverter::class)
    //사용자가 프로필 이미지를 업로드하지 않았을 때, 사용되는 기본 이미지 URL
    var profilePicUrl: MutableList<String> = mutableListOf("https://cdn.quasar.dev/img/boy-avatar.png"),

/*    @Column(name = "verification_code")
    var verificationCode: String? = null,*/

    @Column(name = "password_code")
    var passwordCode: String? = null,

    @Column(name = "provider_id")
    val providerId: String? = null,

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: Status,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    val role: Role,


) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

}