package marketing.mama.domain.post.model



import jakarta.persistence.*
import marketing.mama.domain.user.model.User
import marketing.mama.global.entity.BaseEntity
import marketing.mama.global.exception.StringMutableListConverter


@Entity
@Table(name = "board")
class Board(
    @Column(nullable = false)
    var title: String,

    @Column(nullable = false)
    var content: String,

    @Column(name = "name")
    var name: String,

    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne
    val user: User

) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null


}