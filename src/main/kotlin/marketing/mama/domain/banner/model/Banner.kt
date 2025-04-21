package marketing.mama.domain.banner.model

import jakarta.persistence.*

@Entity
@Table(name = "banner")
class Banner(

    @Column(nullable = false, unique = true)
    val page: String,  // 예: "keyword-single", "ranking"

    var title: String,

    var description1: String,

    var description2: String

){
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}