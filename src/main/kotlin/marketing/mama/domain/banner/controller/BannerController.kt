package marketing.mama.domain.banner.controller

import marketing.mama.domain.banner.dto.BannerRequest
import marketing.mama.domain.banner.dto.BannerResponse
import marketing.mama.domain.banner.service.BannerService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/banner")
class BannerController(
    private val bannerService: BannerService
) {

    @GetMapping
    fun getBanner(@RequestParam page: String): BannerResponse =
        bannerService.getBanner(page)

    @PutMapping("/update")
    fun updateBanner(
        @RequestParam page: String,
        @RequestBody request: BannerRequest
    ): BannerResponse = bannerService.updateBanner(page, request)
}