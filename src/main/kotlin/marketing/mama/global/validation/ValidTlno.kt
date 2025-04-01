package marketing.mama.global.validation


import jakarta.validation.Constraint
import jakarta.validation.Payload
import jakarta.validation.ReportAsSingleViolation
import kotlin.reflect.KClass

@Constraint(validatedBy = [ValidTlnoValidator::class])
@ReportAsSingleViolation
@Retention(AnnotationRetention.RUNTIME)
@Target(allowedTargets = [AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER]
)

annotation class ValidTlno(
    val message: String = "전화번호는 숫자 11자리여야 합니다. (예: 01012345678 또는 010-1234-5678)",
    val groups: Array<KClass<out Any>> = [],
    val payload: Array<KClass<out Payload>> = []
)
