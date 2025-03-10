package marketing.mama.domain.refreshToken.repository

import marketing.mama.domain.refreshToken.model.RefreshToken
import marketing.mama.domain.user.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByUser(user: User): Optional<RefreshToken>
    fun deleteByToken(token: String)

}