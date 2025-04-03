package marketing.mama.domain.user.model

import jakarta.persistence.*
import marketing.mama.global.entity.BaseEntity
import marketing.mama.global.exception.StringMutableListConverter

@Entity
@Table(name = "app_user")
class User(

    @Column(name = "name", nullable = false)
    var name: String,


    @Column(name = "email", nullable = false)
    val email: String,

    @Column(name = "password", nullable = false)
    var password: String,

    @Column(name = "introduction", nullable = false)
    var introduction: String,

    @Column(name = "tlno")
    var tlno: String,

    @Column(nullable = true)
    var rejectReason: String? = null,
/*    @Column(name = "verification_code")
    var verificationCode: String? = null,*/

/*    @Column(name = "password_code")
    var passwordCode: String? = null,

    @Column(name = "provider_id")
    val providerId: String? = null,*/

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