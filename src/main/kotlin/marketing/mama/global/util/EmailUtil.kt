package marketing.mama.global.util

import jakarta.activation.DataHandler
import jakarta.activation.FileDataSource
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component
import java.io.File

@Component
class EmailUtil(
    private val mailSender: JavaMailSender
) {

    @Value("\${spring.mail.username}")
    private lateinit var fromEmail: String

    fun sendZipEmail(to: String, zipFile: File, subject: String, body: String) {
        val message: MimeMessage = mailSender.createMimeMessage()
        val multipart = MimeMultipart()

        val textPart = MimeBodyPart()
        textPart.setText(body)
        multipart.addBodyPart(textPart)

        val attachPart = MimeBodyPart()
        attachPart.dataHandler = DataHandler(FileDataSource(zipFile)) // ✅ 수정된 부분
        attachPart.fileName = zipFile.name
        multipart.addBodyPart(attachPart)

        message.setFrom(InternetAddress(fromEmail))
        message.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(to))
        message.subject = subject
        message.setContent(multipart)

        mailSender.send(message)
    }
}
