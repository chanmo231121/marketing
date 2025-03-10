package marketing.mama.global.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.time.ZonedDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: ZonedDateTime

    @PrePersist
    fun prePersist() {
        createdAt = ZonedDateTime.now()
        updatedAt = ZonedDateTime.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = ZonedDateTime.now()
    }
}