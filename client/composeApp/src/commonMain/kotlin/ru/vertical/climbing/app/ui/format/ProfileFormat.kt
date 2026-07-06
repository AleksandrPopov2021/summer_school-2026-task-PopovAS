package ru.vertical.climbing.app.ui.format

import kotlinx.datetime.LocalDate

/** Формат даты рождения DD.MM.YYYY (SCR-010). */
fun LocalDate.formatBirthDateRu(): String =
    "${dayOfMonth.toString().padStart(2, '0')}.${monthNumber.toString().padStart(2, '0')}.$year"

/**
 * Телефон E.164 → +7 XXX XXX-XX-XX (SCR-010).
 * Некорректные значения возвращаются как есть.
 */
fun formatPhoneRu(e164: String): String {
    val digits = e164.filter { it.isDigit() }
    if (digits.length != 11 || !digits.startsWith("7")) return e164
    return "+7 ${digits.substring(1, 4)} ${digits.substring(4, 7)}-${digits.substring(7, 9)}-${digits.substring(9, 11)}"
}

/** Инициалы из ФИО (первая буква фамилии и имени). */
fun initialsFromFullName(fullName: String): String {
    val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (parts.isEmpty()) return "?"
    if (parts.size == 1) return parts.first().take(1).uppercase()
    return parts[0].take(1).uppercase() + parts[1].take(1).uppercase()
}
