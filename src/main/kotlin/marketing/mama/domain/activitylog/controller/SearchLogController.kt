package marketing.mama.domain.activitylog.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import marketing.mama.domain.activitylog.dto.FrontLogRequest
import marketing.mama.domain.activitylog.dto.SearchLogResponse
import marketing.mama.domain.activitylog.model.ActionType
import marketing.mama.domain.activitylog.service.SearchLogService
import marketing.mama.domain.user.repository.UserRepository
import marketing.mama.domain.user.service.UserService
import marketing.mama.infra.security.UserPrincipal
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.net.URLEncoder
import java.time.LocalDate

@RestController
@RequestMapping("/api/admin/logs")
class SearchLogController(
    private val searchLogService: SearchLogService,
    private val userRepository: UserRepository,
    private val userService: UserService,
) {

    @GetMapping("/user/{userId}/download")
    fun downloadLogExcelByUser(
        @PathVariable userId: Long,
        response: HttpServletResponse
    ) {
        val logs = searchLogService.getLogsByUserId(userId)

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("사용 로그")

        val columns = listOf(
            "사용자 이름", "UUID", "IP 주소", "사용한 기능", "검색 키워드", "검색 시간"
        )

        val header = sheet.createRow(0)
        columns.forEachIndexed { i, v -> header.createCell(i).setCellValue(v) }

        logs.forEachIndexed { index, log ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(log.userName)
            row.createCell(2).setCellValue(log.ipAddress ?: "-")
            row.createCell(3).setCellValue(log.actionType)
            row.createCell(4).setCellValue(log.keyword)
            row.createCell(5).setCellValue(log.searchedAt.toString())
        }

        // 캐시 차단
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate")
        response.setHeader("Pragma", "no-cache")
        response.setHeader("Expires", "0")
        response.setHeader("ETag", "") // ETag 제거

        response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        val filename = URLEncoder.encode("user_log_${userId}_${System.currentTimeMillis()}.xlsx", "UTF-8")
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''$filename")

        response.outputStream.use { workbook.write(it) }
        workbook.close()
    }

    @PostMapping("/log")
    fun logFromFrontend(
        @RequestBody request: FrontLogRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ) {
        val user = userRepository.findById(userPrincipal.id)
            .orElseThrow { IllegalArgumentException("유저를 찾을 수 없습니다.") }

        searchLogService.logSearch(
            user = user,
            userName = user.name,
            ip = request.ip,
            keyword = request.keyword,
            type = request.actionType
        )
    }



    @GetMapping("/user/{userId}/logs")
    @PreAuthorize("hasAnyRole('DEV', 'ADMIN')")
    fun getLogsByUser(
        @PathVariable userId: Long,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) limit: Int? // 👈 이거 추가
    ): List<SearchLogResponse> {
        return if (!startDate.isNullOrBlank()) {
            val date = LocalDate.parse(startDate)
            searchLogService.getLogsByUserIdAndDate(userId, date)
        } else {
            searchLogService.getLogsByUserId(userId, limit = limit) // 👈 limit 반영
        }
    }

    @PostMapping("/custom")
    @PreAuthorize("isAuthenticated()")
    fun logCustomAction(
        @RequestBody request: CustomLogRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        requestRaw: HttpServletRequest
    ): ResponseEntity<Void> {
        val user = userRepository.findById(userPrincipal.id).orElseThrow()
        val ip = requestRaw.remoteAddr

        userService.validateDevice(user, request.uuid)

        searchLogService.logSearch(
            user = user,
            userName = user.name,
            ip = ip,
            keyword = request.keyword,
            type = ActionType.키워드조합,
            uuid = request.uuid
        )
        return ResponseEntity.ok().build()
    }

    data class CustomLogRequest(
        val keyword: String,
        val uuid: String?
    )

}
