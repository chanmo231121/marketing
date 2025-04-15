package marketing.mama.domain.notices.model



import jakarta.persistence.*
import marketing.mama.domain.user.model.User
import marketing.mama.global.entity.BaseEntity


@Entity
@Table(name = "board")
class Board(


    @Column(nullable = false)
    var title: String,

    @Lob
    @Column(nullable = false)
    var content: String,

    @Column(name = "name")
    var name: String,

    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne
    val user: User,

    @Column(name = "is_pinned", nullable = false)
    var isPinned: Boolean = false

) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null


}