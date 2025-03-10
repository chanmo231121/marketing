package marketing.mama.global.validation


import jakarta.validation.Constraint
import jakarta.validation.Payload
import jakarta.validation.ReportAsSingleViolation
import kotlin.reflect.KClass

@Constraint(validatedBy = [ValidPasswordValidator::class])
@ReportAsSingleViolation
@Retention(AnnotationRetention.RUNTIME)
@Target(allowedTargets = [AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER]
)
annotation class ValidPassword(
    val message: String = "password는 최소 8자 이상, 15자 이하이며 알파벳 대소문자(a~z, A~Z), 숫자(0~9), 특수문자로 구성되어야 합니다.",
    val groups: Array<KClass<out Any>> = [],
    val payload: Array<KClass<out Payload>> = []
)
