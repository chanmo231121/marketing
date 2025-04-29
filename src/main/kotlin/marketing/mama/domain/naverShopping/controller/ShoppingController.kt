package marketing.mama.domain.naverShopping.controller


import marketing.mama.domain.naverShopping.service.ShoppingService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/shopping")
class ShoppingController(
    private val shoppingService: ShoppingService
) {

    @GetMapping("/mobile")
    fun crawlMobile(@RequestParam keyword: String): List<Map<String, Any>> {
        return shoppingService.crawlMobileShopping(keyword)
    }
}