package marketing.mama.domain.banner.service

import marketing.mama.domain.banner.dto.BannerRequest
import marketing.mama.domain.banner.dto.BannerResponse
import marketing.mama.domain.banner.model.Banner
import marketing.mama.domain.banner.repository.BannerRepository
import org.springframework.stereotype.Service

@Service
class BannerServiceImpl(
    private val bannerRepository: BannerRepository
) : BannerService {

    override fun getBanner(page: String): BannerResponse {
        val banner = bannerRepository.findByPage(page)
            ?: bannerRepository.save(
                Banner(page, "기본 제목입니다.", "기본 설명1입니다.", "기본 설명2입니다.")
            )

        return BannerResponse(
            page = banner.page,
            title = banner.title,
            description1 = banner.description1,
            description2 = banner.description2
        )
    }

    override fun updateBanner(page: String, request: BannerRequest): BannerResponse {
        val banner = bannerRepository.findByPage(page)
            ?: Banner(page, request.title, request.description1, request.description2)

        banner.title = request.title
        banner.description1 = request.description1
        banner.description2 = request.description2
        bannerRepository.save(banner)

        return BannerResponse(
            page = banner.page,
            title = banner.title,
            description1 = banner.description1,
            description2 = banner.description2
        )
    }
}