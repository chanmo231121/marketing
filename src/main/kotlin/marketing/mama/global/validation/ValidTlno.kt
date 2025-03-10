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
    val message: String = "번호는 -포함 13자리입니다 . 숫자(0~9)로 구성 되어야 합니다.",
    val groups: Array<KClass<out Any>> = [],
    val payload: Array<KClass<out Payload>> = []
)
