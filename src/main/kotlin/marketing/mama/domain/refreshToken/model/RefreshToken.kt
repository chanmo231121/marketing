package marketing.mama.domain.refreshToken.model

import jakarta.persistence.*
import marketing.mama.domain.user.model.User


@Entity
@Table(name = "refresh_tokens")
class RefreshToken(


    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    val user: User,

    @Column(nullable = false, unique = true)
    val token: String
){
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}