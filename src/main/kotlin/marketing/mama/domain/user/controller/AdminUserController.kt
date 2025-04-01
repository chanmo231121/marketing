package marketing.mama.domain.user.controller

import marketing.mama.domain.user.dto.response.UserResponse
import marketing.mama.domain.user.service.AdminUserService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/users")
class AdminUserController(
    private val adminUserService: AdminUserService
) {

    @PreAuthorize("hasAnyRole('관리자', '개발자')")
    @GetMapping("/pending-pros")
    fun getPendingPros(): ResponseEntity<List<UserResponse>> {
        return ResponseEntity.ok(adminUserService.getPendingPros())
    }
    @PreAuthorize("hasAnyRole('관리자', '개발자')")
    @PutMapping("/approve/{userId}")
    fun approvePro(@PathVariable userId: Long): ResponseEntity<String> {
        return ResponseEntity.ok(adminUserService.approvePro(userId))
    }
}