package marketing.mama.domain.search.model

import jakarta.persistence.*
import marketing.mama.domain.user.model.User
import marketing.mama.global.entity.BaseEntity
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.LocalDate

@Entity
@Table(name = "search_usage")
data class SearchUsage(


    // User 엔티티와 연관 (Lazy 로딩)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val user: User,

    @Column(name = "usage_date", nullable = false)
    val usageDate: LocalDate = LocalDate.now(),

    @Column(name = "single_search_count", nullable = false)
    var singleSearchCount: Int = 0,

    @Column(name = "ranking_search_count", nullable = false)
    var rankingSearchCount: Int = 0,

    @Column(name = "shopping_search_count", nullable = false)
    var shoppingSearchCount: Int = 0,

    @Column(name = "trend_search_count", nullable = false)
    var trendSearchCount: Int = 0


) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

}