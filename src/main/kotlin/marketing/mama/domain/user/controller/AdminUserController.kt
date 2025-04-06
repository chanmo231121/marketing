package marketing.mama.domain.user.controller

import marketing.mama.domain.user.dto.request.ExtendUserRequest
import marketing.mama.domain.user.dto.request.RejectUserRequest
import marketing.mama.domain.user.dto.response.UserResponse
import marketing.mama.domain.user.model.Role
import marketing.mama.domain.user.service.AdminUserService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/users")
class AdminUserController(
    private val adminUserService: AdminUserService
) {

    // ✅ [GET] 승인 대기 중인 프로 유저 목록 조회
    // - Status: PENDING_APPROVAL
    @PreAuthorize("hasAnyRole('관리자', '개발자')")
    @GetMapping("/pending-pros")
    fun getPendingPros(): ResponseEntity<List<UserResponse>> {
        return ResponseEntity.ok(adminUserService.getPendingPros())
    }

    // ✅ [PUT] 프로 유저 승인
    // - 해당 유저의 status를 NORMAL로 변경
    @PreAuthorize("hasAnyRole('관리자', '개발자')")
    @PutMapping("/approve/{userId}")
    fun approvePro(@PathVariable userId: Long): ResponseEntity<String> {
        return ResponseEntity.ok(adminUserService.approvePro(userId))
    }

    // ✅ [PUT] 프로 유저 거절
    // - 해당 유저의 status를 REJECTED로 변경
    @PreAuthorize("hasAnyRole('관리자', '개발자')")
    @PutMapping("/reject/{userId}")
    fun rejectPro(
        @PathVariable userId: Long,
        @RequestBody request: RejectUserRequest
    ): ResponseEntity<String> {
        return ResponseEntity.ok(adminUserService.rejectPro(userId, request.reason))
    }

    // ✅ [GET] 거절된 유저 목록 조회
    // - Status: REJECTED인 유저 목록 반환
    @PreAuthorize("hasAnyRole('관리자', '개발자')")
    @GetMapping("/rejected")
    fun getRejectedUsers(): ResponseEntity<List<UserResponse>> {
        val rejectedUsers = adminUserService.findRejectedUsers()
        return ResponseEntity.ok(rejectedUsers)
    }


    // ✅ [DELETE] 유저 삭제
    // - 유저 ID로 완전 삭제 (거절된 유저 등)
    @PreAuthorize("hasAnyRole('관리자', '개발자')")
    @DeleteMapping("/{userId}")
    fun deleteUser(@PathVariable userId: Long): ResponseEntity<Void> {
        adminUserService.deleteUser(userId)
        return ResponseEntity.noContent().build()
    }

    @PreAuthorize("hasAnyRole('관리자', '개발자')")
    @PutMapping("/restore/{userId}")
    fun restorePro(@PathVariable userId: Long): ResponseEntity<String> {
        return ResponseEntity.ok(adminUserService.restorePro(userId))
    }

    @PreAuthorize("hasAnyRole('관리자', '개발자')")
    @GetMapping("/pro")
    fun getApprovedProUsers(): ResponseEntity<List<UserResponse>> {
        val users = adminUserService.getApprovedProUsers()
        return ResponseEntity.ok(users)
    }

    // ✅ [GET] 재승인 대기 중인 프로 유저 목록 조회
    @PreAuthorize("hasAnyRole('관리자', '개발자')")
    @GetMapping("/reapproval-pending-pros")
    fun getReapprovalPendingPros(): ResponseEntity<List<UserResponse>> {
        return ResponseEntity.ok(adminUserService.getReapprovalPendingPros())
    }

    @PutMapping("/extend/{userId}")
    @PreAuthorize("hasAnyRole('관리자', '개발자')")
    fun extendUserApproval(
        @PathVariable userId: Long,
        @RequestBody request: ExtendUserRequest
    ): ResponseEntity<String> {
        adminUserService.extendApproval(userId, request)
        return ResponseEntity.ok("승인 기간이 연장되었습니다.")
    }

}
