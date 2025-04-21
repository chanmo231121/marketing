package marketing.mama.domain.banner.service

import marketing.mama.domain.banner.dto.BannerRequest
import marketing.mama.domain.banner.dto.BannerResponse

interface BannerService {
    fun getBanner(page: String): BannerResponse
    fun updateBanner(page: String, request: BannerRequest): BannerResponse
}