package marketing.mama.domain.logarchive.scheduler

import marketing.mama.domain.activitylog.repository.SearchLogRepository
import marketing.mama.domain.user.repository.UserRepository
import marketing.mama.global.util.EmailUtil
import marketing.mama.global.util.ExcelUtil
import marketing.mama.global.util.ZipUtil
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class MonthlyLogScheduler(
    private val userRepository: UserRepository,
    private val searchLogRepository: SearchLogRepository,
    private val emailUtil: EmailUtil,
    private val excelUtil: ExcelUtil,
    private val zipUtil: ZipUtil
) {

    // 매월 1일 새벽 3시에 실행
    @Scheduled(cron = "0 0 3 1 * *")
    fun sendLogsAndDeletePreviousMonth() {
        val startOfLastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1).atStartOfDay()
        val endOfLastMonth = startOfLastMonth.plusMonths(1).minusNanos(1)

        val logs = searchLogRepository.findAllBySearchedAtBetween(startOfLastMonth, endOfLastMonth)
        if (logs.isEmpty()) {
            println("✅ 전달 로그 없음. 작업 종료")
            return
        }

        // 1. 엑셀 생성 → ByteArray
        val excelBytes = excelUtil.createExcelFromLogs(logs)

        // 2. ByteArray 압축 → zip ByteArray
        val formattedMonth = startOfLastMonth.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val zipBytes = zipUtil.zipFiles("logs.xlsx", excelBytes)

        // 3. zip ByteArray → 임시 파일
        val zipFile = File.createTempFile("logs_$formattedMonth", ".zip")
        zipFile.writeBytes(zipBytes)

        // 4. 관리자에게 이메일 발송
        val admins = userRepository.findAllByReceiveLogEmailTrue()
        if (admins.isEmpty()) {
            println("⚠️ 이메일 받을 관리자 없음. 압축만 완료됨.")
        } else {
            admins.forEach { admin ->
                emailUtil.sendZipEmail(
                    to = admin.email,
                    zipFile = zipFile,
                    subject = "🗃️ $formattedMonth 검색 로그",
                    body = "안녕하세요 ${admin.name}님,\n\n${formattedMonth}월의 전체 검색 로그를 첨부드립니다.\n\n감사합니다."
                )
                println("📧 ${admin.email} 전송 완료")
            }
        }

        // 5. 전송 후 로그 삭제
        searchLogRepository.deleteAll(logs)
        println("🧹 전달 로그 삭제 완료")

        // 6. 임시 파일 삭제
        zipFile.delete()
    }
}
