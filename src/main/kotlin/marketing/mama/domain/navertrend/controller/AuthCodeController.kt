package marketing.mama.domain.navertrend.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.*

@RestController
class AuthCodeController {

    private val secretKey = "MAGLO_SECRET_KEY"  // 클라이언트와 동일한 시크릿 키

    @GetMapping("/api/code")
    fun getAuthCode(): Map<String, String> {
        val now = LocalDate.now(ZoneId.of("Asia/Seoul"))
        val weekIndex = now.get(WeekFields.of(Locale.KOREA).weekOfYear())
        val raw = "$secretKey-$weekIndex"
        val hash = sha256(raw).substring(0, 16).uppercase()
        return mapOf(
            "code" to hash,
            "week" to weekIndex.toString(),
            "date" to now.toString()
        )
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
