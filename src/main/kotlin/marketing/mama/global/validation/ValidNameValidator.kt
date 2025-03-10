package marketing.mama.global.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class ValidNameValidator : ConstraintValidator<ValidName, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {

        val isValid = value != null && value.matches(Regex("^(?=.*[a-z])(?=.*\\d)[a-z0-9]*$")) && value.length in 4..10

        if (!isValid) {
            context?.disableDefaultConstraintViolation()
            context?.buildConstraintViolationWithTemplate("Name은 최소 4자 이상, 10자 이하이며 알파벳 소문자(a~z), 숫자(0~9)로 구성 되어야 합니다.")
                ?.addConstraintViolation()
        }
        return isValid
    }
}