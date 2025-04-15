package marketing.mama.domain.user.controller

import marketing.mama.domain.user.dto.request.ApprovalRequest
import marketing.mama.domain.user.dto.request.RejectUserRequest
import marketing.mama.domain.user.dto.response.UserResponse
import marketing.mama.domain.user.service.AdminAdminService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/admins")
class AdminApprovalController(
    private val adminAdminService: AdminAdminService
) {

    // ✅ 대기 중인 관리자 유저 목록 조회
    @PreAuthorize("hasRole('DEV')")
    @GetMapping("/pending")
    fun getPendingAdmins(): ResponseEntity<List<UserResponse>> {
        return ResponseEntity.ok(adminAdminService.getPendingAdmins())
    }

    // ✅ 관리자 승인 처리
    @PutMapping("/approve/{userId}")
    @PreAuthorize("hasRole('DEV')")
    fun approveAdmin(
        @PathVariable userId: Long,
        @RequestBody request: ApprovalRequest
    ): ResponseEntity<String> {
        return ResponseEntity.ok(adminAdminService.approveAdmin(userId, request.role))
    }

    // ✅ 관리자 거절 처리 (선택)
    @PreAuthorize("hasRole('DEV')")
    @PutMapping("/reject/{userId}")
    fun rejectAdmin(
        @PathVariable userId: Long,
        @RequestBody request: RejectUserRequest
    ): ResponseEntity<String> {
        return ResponseEntity.ok(adminAdminService.rejectAdmin(userId, request.reason))
    }

    @PreAuthorize("hasRole('DEV')")
    @GetMapping("/rejected")
    fun getRejectedAdmins(): ResponseEntity<List<UserResponse>> {
        return ResponseEntity.ok(adminAdminService.getRejectedAdmins())
    }

    @PreAuthorize("hasRole('DEV')")
    @PutMapping("/restore/{userId}")
    fun restoreAdmin(@PathVariable userId: Long): ResponseEntity<String> {
        return ResponseEntity.ok(adminAdminService.restoreAdmin(userId))
    }

    @PreAuthorize("hasRole('DEV')")
    @GetMapping("/approved")
    fun getApprovedAdminsAndPros(): ResponseEntity<List<UserResponse>> {
        return ResponseEntity.ok(adminAdminService.getApprovedAdminsAndPros())
    }

    @PreAuthorize("hasRole('DEV')")
    @GetMapping("/reapproval-pending")
    fun getReapprovalPendingAdmins(): ResponseEntity<List<UserResponse>> {
        return ResponseEntity.ok(adminAdminService.getReapprovalPendingAdmins())
    }

}
