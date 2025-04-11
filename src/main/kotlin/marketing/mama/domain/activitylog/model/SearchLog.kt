package marketing.mama.domain.activitylog.model

import jakarta.persistence.*
import marketing.mama.domain.activitylog.dto.SearchLogResponse
import marketing.mama.domain.user.model.User
import marketing.mama.global.entity.BaseEntity
import java.time.LocalDateTime

@Entity
data class SearchLog(


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @Column(nullable = false)
    val uuid: String?,

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
    val userName: String

) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null


}
fun SearchLog.toResponse(): SearchLogResponse {
    return SearchLogResponse(
        userName = this.userName,
        ipAddress = this.ipAddress,
        uuid = this.uuid,
        actionType = this.actionType.name,
        keyword = this.keyword,
        searchedAt = this.searchedAt
    )
}