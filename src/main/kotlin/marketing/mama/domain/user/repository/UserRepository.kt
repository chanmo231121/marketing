package marketing.mama.domain.user.repository


import marketing.mama.domain.user.model.Role
import marketing.mama.domain.user.model.Status
import marketing.mama.domain.user.model.User
import org.springframework.data.jpa.repository.JpaRepository


interface UserRepository:JpaRepository<User, Long> {

//    fun findByProviderAndProviderId(provider: OAuth2Provider, toString: String): User?
    fun existsByEmail(email: String): Boolean
    fun findByEmail(email:String):User?
    fun findByStatus(status: Status): List<User>
    fun findByEmailAndTlno(email: String, tlno: String): User?
    //fun findByNickname(nickname: String): User?
    fun existsByname(name: String): Boolean
    fun findAllByRoleAndStatus(role: Role, status: Status): List<User>

}