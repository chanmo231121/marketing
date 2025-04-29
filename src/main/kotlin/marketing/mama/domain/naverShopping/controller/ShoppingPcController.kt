package marketing.mama.domain.naverShopping.controller

import marketing.mama.domain.naverShopping.service.ShoppingPcService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/shopping/pc")
class ShoppingPcController(
    private val shoppingPcService: ShoppingPcService
) {

    @GetMapping
    fun crawlPc(@RequestParam keyword: String): List<Map<String, Any>> {
        return shoppingPcService.crawlPcShopping(keyword)
    }
}