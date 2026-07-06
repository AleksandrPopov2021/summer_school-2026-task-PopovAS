package ru.vertical.climbing.domain.validation

import kotlinx.datetime.LocalDate

/** Причина ошибки конкретного поля формы регистрации (SCR-002, LOGIC-002). */
enum class RegistrationError {
    PHONE_INVALID,
    NAME_INVALID,
    BIRTH_DATE_INVALID,
}

/** Результат клиентской валидации формы регистрации. */
data class RegistrationValidationResult(
    val phone: RegistrationError? = null,
    val fullName: RegistrationError? = null,
    val birthDate: RegistrationError? = null,
) {
    val isValid: Boolean get() = phone == null && fullName == null && birthDate == null
}

/**
 * Клиентская валидация полей регистрации (BR-030). Чистая логика без платформенных
 * зависимостей — легко тестируется.
 *
 * Правила (SCR-002):
 * - телефон: ровно 10 цифр после `+7` → E.164;
 * - ФИО: 2–200 символов после нормализации, минимум два слова;
 * - дата рождения: не в будущем и возраст ≥ [MIN_AGE_YEARS].
 */
object RegistrationValidator {

    const val MIN_AGE_YEARS = 6
    const val PHONE_DIGITS = 10
    const val NAME_MIN = 2
    const val NAME_MAX = 200

    fun validate(
        phoneDigits: String,
        fullName: String,
        birthDate: LocalDate?,
        today: LocalDate,
    ): RegistrationValidationResult = RegistrationValidationResult(
        phone = validatePhone(phoneDigits),
        fullName = validateName(fullName),
        birthDate = validateBirthDate(birthDate, today),
    )

    fun validatePhone(phoneDigits: String): RegistrationError? {
        val digits = phoneDigits.filter { it.isDigit() }
        return if (digits.length == PHONE_DIGITS) null else RegistrationError.PHONE_INVALID
    }

    fun validateName(fullName: String): RegistrationError? {
        val normalized = normalizeName(fullName)
        val wordCount = normalized.split(' ').filter { it.isNotBlank() }.size
        return when {
            normalized.length < NAME_MIN || normalized.length > NAME_MAX -> RegistrationError.NAME_INVALID
            wordCount < 2 -> RegistrationError.NAME_INVALID
            else -> null
        }
    }

    fun validateBirthDate(birthDate: LocalDate?, today: LocalDate): RegistrationError? {
        if (birthDate == null || birthDate > today) return RegistrationError.BIRTH_DATE_INVALID
        return if (ageInYears(birthDate, today) >= MIN_AGE_YEARS) null else RegistrationError.BIRTH_DATE_INVALID
    }

    /** Схлопывает повторяющиеся пробелы и обрезает края. */
    fun normalizeName(fullName: String): String =
        fullName.trim().replace(Regex("\\s+"), " ")

    /** Формирует номер в формате E.164 для РФ (`+7` + 10 цифр). */
    fun toE164(phoneDigits: String): String = "+7" + phoneDigits.filter { it.isDigit() }.takeLast(PHONE_DIGITS)

    private fun ageInYears(birthDate: LocalDate, today: LocalDate): Int {
        var age = today.year - birthDate.year
        val hadBirthdayThisYear = today.monthNumber > birthDate.monthNumber ||
            (today.monthNumber == birthDate.monthNumber && today.dayOfMonth >= birthDate.dayOfMonth)
        if (!hadBirthdayThisYear) age -= 1
        return age
    }
}
