package marketing.mama.global.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class ValidPasswordValidator : ConstraintValidator<ValidPassword, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {

        val isValid = value != null && value.matches(Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#\$%^&*()/*-+?|]).{8,15}$"))

        if (!isValid) {
            context?.disableDefaultConstraintViolation()
            context?.buildConstraintViolationWithTemplate("password는 최소 8자 이상, 15자 이하이며 알파벳 대소문자(a~z, A~Z), 숫자(0~9), 특수문자로 구성되어야 합니다.")
                ?.addConstraintViolation()
        }
        return isValid
    }
}