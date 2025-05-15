package marketing.mama.domain.user.model

import jakarta.persistence.*
import marketing.mama.domain.refreshToken.model.RefreshToken
import marketing.mama.global.entity.BaseEntity
import marketing.mama.global.exception.StringMutableListConverter
import java.time.LocalDateTime

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
    var role: Role,

    @Column(name = "ip_address")
    var ipAddress: String? = null,

    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null,

    @Column(name = "last_approved_at")
    var lastApprovedAt: LocalDateTime? = null,

    @Column(name = "device_id", nullable = true, unique = true)
    var deviceId: String? = null,

    @Column(nullable = false)
    var autoExtend: Boolean = false,


    @Column(name = "approved_until")
    var approvedUntil: LocalDateTime? = null,

    @Column(nullable = false)
    var receiveLogEmail: Boolean = false,

    @Column(nullable = false)
    var canUseSingleSearch: Boolean = true,

    @Column(nullable = false)
    var canUseRankingSearch: Boolean = true,

    @Column(nullable = false)
    var canUseKeywordMix: Boolean = true,

    @Column(nullable = false)
    var canUseRelatedSearch: Boolean = true,

    @Column(nullable = false)
    var canUseShoppingSearch: Boolean = true,

    @Column(nullable = false)
    var canUseTrendSearch:Boolean = true,

    @Column(name = "single_search_limit")
    var singleSearchLimit: Int? = 200,

    @Column(name = "ranking_search_limit")
    var rankingSearchLimit: Int? = 50,

    @Column(name = "shopping_search_limit")
    var shoppingSearchLimit: Int? = 50,

    @Column(name = "trend_search_limit")
    var trendSearchLimit: Int? = 100,

    @OneToMany(mappedBy = "user", cascade = [CascadeType.REMOVE], orphanRemoval = true)
    val refreshTokens: MutableList<RefreshToken> = mutableListOf()

    ) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

}