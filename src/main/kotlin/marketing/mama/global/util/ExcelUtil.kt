package marketing.mama.global.util

import marketing.mama.domain.activitylog.model.SearchLog
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream

@Component
class ExcelUtil {

    fun createExcelFromLogs(logs: List<SearchLog>): ByteArray {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("검색 로그")

        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("유저명")
        header.createCell(1).setCellValue("UUID")
        header.createCell(2).setCellValue("IP")
        header.createCell(3).setCellValue("검색어")
        header.createCell(4).setCellValue("기능")
        header.createCell(5).setCellValue("검색일시")

        logs.forEachIndexed { index, log ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(log.userName)
            row.createCell(1).setCellValue(log.uuid)
            row.createCell(2).setCellValue(log.ipAddress ?: "-")
            row.createCell(3).setCellValue(log.keyword)
            row.createCell(4).setCellValue(log.actionType.name)
            row.createCell(5).setCellValue(log.searchedAt.toString())
        }

        return ByteArrayOutputStream().use {
            workbook.write(it)
            it.toByteArray()
        }
    }
}
