package marketing.mama.domain.user.controller

import marketing.mama.domain.search.dto.UserWithUsageResponse
import marketing.mama.domain.search.service.SearchUsageService
import marketing.mama.domain.user.dto.request.ApprovalRequest
import marketing.mama.domain.user.dto.request.ExtendUserRequest
import marketing.mama.domain.user.dto.request.RejectUserRequest
import marketing.mama.domain.user.dto.request.UpdateFeatureUsageRequest
import marketing.mama.domain.user.dto.response.UserResponse
import marketing.mama.domain.user.model.Role
import marketing.mama.domain.user.model.Status
import marketing.mama.domain.user.repository.UserRepository
import marketing.mama.domain.user.service.AdminUserService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/v1/admin/users")
class AdminUserController(
    private val adminUserService: AdminUserService,
    private val userRepository: UserRepository, // ✅ 추가
    private val searchUsageService: SearchUsageService // ✅ 추가

) {

    // ✅ [GET] 승인 대기 중인 프로 유저 목록 조회
    // - Status: PENDING_APPROVAL
    @GetMapping("/pending-pros")
    fun getPendingPros(): ResponseEntity<List<UserResponse>> {
        return ResponseEntity.ok(adminUserService.getPendingPros())
    }

    // [PUT] 프로 유저 승인 (승인 요청 시 role 값 포함)
    @PutMapping("/approve/{userId}")
    fun approvePro(
        @PathVariable userId: Long,
        @RequestBody approvalRequest: ApprovalRequest
    ): ResponseEntity<String> {
        val message = adminUserService.approvePro(userId, approvalRequest.role)
        return ResponseEntity.ok(message)
    }

    // [PUT] 프로 유저 거절
    @PutMapping("/reject/{userId}")
    fun rejectPro(
        @PathVariable userId: Long,
        @RequestBody request: RejectUserRequest
    ): ResponseEntity<String> {
        return ResponseEntity.ok(adminUserService.rejectPro(userId, request.reason))
    }

    // ✅ [GET] 거절된 유저 목록 조회
    // - Status: REJECTED인 유저 목록 반환
    @GetMapping("/rejected")
    fun getRejectedUsers(): ResponseEntity<List<UserResponse>> {
        val rejectedUsers = adminUserService.findRejectedUsers()
        return ResponseEntity.ok(rejectedUsers)
    }


    // ✅ [DELETE] 유저 삭제
    // - 유저 ID로 완전 삭제 (거절된 유저 등)
    @DeleteMapping("/{userId}")
    fun deleteUser(@PathVariable userId: Long): ResponseEntity<Void> {
        adminUserService.deleteUser(userId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/restore/{userId}")
    fun restorePro(@PathVariable userId: Long): ResponseEntity<String> {
        return ResponseEntity.ok(adminUserService.restorePro(userId))
    }

    @GetMapping("/pro")
    fun getApprovedProUsers(): ResponseEntity<List<UserResponse>> {
        val users = adminUserService.getApprovedProUsers()
        return ResponseEntity.ok(users)
    }

    // ✅ [GET] 재승인 대기 중인 프로 유저 목록 조회
    @GetMapping("/reapproval-pending-pros")
    fun getReapprovalPendingPros(): ResponseEntity<List<UserResponse>> {
        return ResponseEntity.ok(adminUserService.getReapprovalPendingPros())
    }

    @PutMapping("/extend/{userId}")
    fun extendUserApproval(
        @PathVariable userId: Long,
        @RequestBody request: ExtendUserRequest
    ): ResponseEntity<String> {
        adminUserService.extendApproval(userId, request)
        return ResponseEntity.ok("승인 기간이 연장되었습니다.")
    }


    @GetMapping("/{userId}/detail")
    fun getUserDetail(@PathVariable userId: Long): ResponseEntity<UserWithUsageResponse> {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "해당 유저를 찾을 수 없습니다.")

        val usage = searchUsageService.getUsageInfo(user)

        return ResponseEntity.ok(
            UserWithUsageResponse(
                user = UserResponse.from(user),
                usage = usage,
            )
        )
    }

    @PutMapping("/{userId}/feature-usage")
    fun updateFeatureUsage(
        @PathVariable userId: Long,
        @RequestBody request: UpdateFeatureUsageRequest
    ): ResponseEntity<String> {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다.")

        when (request.feature) {
            "single" -> user.canUseSingleSearch = request.enabled
            "ranking" -> user.canUseRankingSearch = request.enabled
            "related" -> user.canUseRelatedSearch = request.enabled
            "mixer" -> user.canUseKeywordMix = request.enabled
            else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "알 수 없는 기능명입니다.")
        }

        userRepository.save(user)

        return ResponseEntity.ok("기능 사용 여부가 업데이트되었습니다.")
    }

    @PutMapping("/expire/{userId}") // ← URL 충돌 방지
    fun expireUserImmediately(@PathVariable userId: Long): ResponseEntity<String> {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "해당 유저를 찾을 수 없습니다.")

        user.status = Status.PENDING_APPROVAL
        user.approvedUntil = null
        user.autoExtend = false

        userRepository.save(user)

        return ResponseEntity.ok("해당 유저의 상태가 만료되었습니다.")
    }

    @PutMapping("/{userId}/usage-limit")
    fun updateUsageLimit(
        @PathVariable userId: Long,
        @RequestBody req: Map<String, Any>
    ): ResponseEntity<String> {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다.")

        val type = req["type"] as? String
        val limit = (req["limit"] as? Number)?.toInt()

        if (type.isNullOrBlank() || limit == null || limit <= 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "type 또는 limit 값이 잘못되었습니다.")
        }

        when (type) {
            "single" -> user.singleSearchLimit = limit
            "ranking" -> user.rankingSearchLimit = limit
            else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "알 수 없는 타입입니다.")
        }

        userRepository.save(user)
        return ResponseEntity.ok("사용 제한이 $limit 회로 설정되었습니다.")
    }

}
