package marketing.mama.global.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class ValidTlnoValidator : ConstraintValidator<ValidTlno, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) {
            return false
        }

        // 숫자만 추출 (예: 010-1234-5678 → 01012345678)
        val digitsOnly = value.replace(Regex("[^0-9]"), "")

        val isValid = digitsOnly.length == 11

        if (!isValid) {
            context?.disableDefaultConstraintViolation()
            context?.buildConstraintViolationWithTemplate("전화번호는 숫자 11자리여야 합니다. (예: 01012345678 또는 010-1234-5678)")
                ?.addConstraintViolation()
        }

        return isValid
    }
}
