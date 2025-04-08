package marketing.mama.global.util

import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


@Component
object ZipUtil {

    fun zipFiles(fileName: String, fileContent: ByteArray): ByteArray {
        val zipBytes = ByteArrayOutputStream()

        ZipOutputStream(zipBytes).use { zipOut ->
            val entry = ZipEntry(fileName)
            zipOut.putNextEntry(entry)
            zipOut.write(fileContent)
            zipOut.closeEntry()
        }

        return zipBytes.toByteArray()
    }
}
