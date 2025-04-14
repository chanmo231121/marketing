package marketing.mama.domain.activitylog.model

import jakarta.persistence.*
import marketing.mama.domain.activitylog.dto.SearchLogResponse
import marketing.mama.domain.user.model.User
import marketing.mama.global.entity.BaseEntity
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.LocalDateTime

@Entity
data class SearchLog(


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val user: User,

    @Column(nullable = true)
    val ipAddress: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val actionType: ActionType,

    @Column(nullable = false)
    val keyword: String,

    @Column(nullable = false)
    val searchedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = true)
    val loggedInAt: LocalDateTime? = null,

    @Column(nullable = false)
    val userName: String,

    @Column(nullable = true)
    val uuid: String? = null,

) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null


}
fun SearchLog.toResponse(): SearchLogResponse {
    return SearchLogResponse(
        userName = this.userName,
        ipAddress = this.ipAddress,
        actionType = this.actionType.name,
        keyword = this.keyword,
        searchedAt = this.searchedAt,
        uuid = this.uuid,
    )
}