package marketing.mama.global.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.ConstraintViolationException

class ValidTlnoValidator : ConstraintValidator<ValidTlno, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {

        val isValid = value != null && value.matches(Regex("^[0-9-]*$")) && value.length == 13

        if (!isValid) {
            context?.disableDefaultConstraintViolation()
            context?.buildConstraintViolationWithTemplate("번호는 -포함 13자리입니다 . 숫자(0~9)로 구성 되어야 합니다.")
                ?.addConstraintViolation()
        }
        return isValid
    }
}
