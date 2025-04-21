package marketing.mama.domain.banner.repository

import marketing.mama.domain.banner.model.Banner
import org.springframework.data.jpa.repository.JpaRepository

interface BannerRepository : JpaRepository<Banner, Long> {
    fun findByPage(page: String): Banner?
}