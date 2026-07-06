package ru.vertical.climbing.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import ru.vertical.climbing.domain.validation.RegistrationError
import ru.vertical.climbing.domain.validation.RegistrationValidator

class RegistrationValidatorTest {

    private val today = LocalDate.parse("2026-07-05")

    @Test
    fun valid_input_passes() {
        val result = RegistrationValidator.validate(
            phoneDigits = "9001234567",
            fullName = "Иванов Иван",
            birthDate = LocalDate.parse("1995-03-15"),
            today = today,
        )
        assertTrue(result.isValid)
    }

    @Test
    fun phone_must_have_ten_digits() {
        assertEquals(RegistrationError.PHONE_INVALID, RegistrationValidator.validatePhone("900123"))
        assertNull(RegistrationValidator.validatePhone("900 123 45 67"))
    }

    @Test
    fun name_needs_two_words() {
        assertEquals(RegistrationError.NAME_INVALID, RegistrationValidator.validateName("Иван"))
        assertNull(RegistrationValidator.validateName("  Иванов   Иван  "))
    }

    @Test
    fun birth_date_in_future_is_invalid() {
        assertEquals(
            RegistrationError.BIRTH_DATE_INVALID,
            RegistrationValidator.validateBirthDate(LocalDate.parse("2027-01-01"), today),
        )
    }

    @Test
    fun age_below_minimum_is_invalid() {
        // Возраст ~5 лет на дату today.
        assertEquals(
            RegistrationError.BIRTH_DATE_INVALID,
            RegistrationValidator.validateBirthDate(LocalDate.parse("2021-01-01"), today),
        )
    }

    @Test
    fun age_exactly_minimum_is_valid() {
        assertNull(RegistrationValidator.validateBirthDate(LocalDate.parse("2020-07-05"), today))
    }

    @Test
    fun e164_is_formed_from_digits() {
        assertEquals("+79001234567", RegistrationValidator.toE164("9001234567"))
        assertEquals("+79001234567", RegistrationValidator.toE164("+7 900 123-45-67"))
    }
}
