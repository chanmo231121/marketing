package org.team.b4.cosmicadventures.global.aws

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.util.*

@Service
class S3Service(
    private val s3Client: AmazonS3Client,
    @Value("\${aws.s3.bucket}") private val bucket: String,
) {

    @Throws(IOException::class)
    fun upload(
        multipartFiles: MutableList<MultipartFile>,
        dir: String
    ): List<String> {
        return multipartFiles.map { multipartFile ->
            val fileName = UUID.randomUUID().toString() + "-" + multipartFile.originalFilename

            val objMeta = ObjectMetadata().apply {
                this.contentType = multipartFile.contentType
                this.contentLength = multipartFile.size
            }

            val putObjectRequest = PutObjectRequest(
                bucket,
                "$dir/$fileName",
                multipartFile.inputStream,
                objMeta,
            )
            s3Client.putObject(
                putObjectRequest
                    .withCannedAcl(CannedAccessControlList.PublicRead)
            )

            s3Client.getUrl(bucket, "$dir/$fileName").toString()
        }
    }

}
