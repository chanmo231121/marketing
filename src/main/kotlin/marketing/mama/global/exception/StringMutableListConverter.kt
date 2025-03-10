package marketing.mama.global.exception

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter

class StringMutableListConverter : AttributeConverter<MutableList<String>, String> {
    private val mapper: ObjectMapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
        .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)

    override fun convertToDatabaseColumn(attribute: MutableList<String>?): String {
        try {
            return mapper.writeValueAsString(attribute)
        } catch (e: JsonProcessingException) {
            throw BusinessLogicException("list를 string으로 변환 실패")
        }
    }

    override fun convertToEntityAttribute(dbData: String?): MutableList<String> {
        try {
            return mapper.readValue(dbData ?: "", object : TypeReference<MutableList<String>>() {})
        } catch (e: JsonProcessingException) {
            throw BusinessLogicException("string을 list로 변환 실패")
        }
    }
}