package marketing.mama.global.validation


import jakarta.validation.Constraint
import jakarta.validation.Payload
import jakarta.validation.ReportAsSingleViolation
import kotlin.reflect.KClass

@Constraint(validatedBy = [ValidNameValidator::class])
@ReportAsSingleViolation
@Retention(AnnotationRetention.RUNTIME)
@Target(allowedTargets = [AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER])
annotation class ValidName(
    val message: String = "Name은 최소 4자 이상, 10자 이하이며 알파벳 소문자(a~z), 숫자(0~9)로 구성 되어야 합니다.",
    val groups: Array<KClass<out Any>> = [],
    val payload: Array<KClass<out Payload>> = []
)

