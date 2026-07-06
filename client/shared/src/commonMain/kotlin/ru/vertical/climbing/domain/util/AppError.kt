package ru.vertical.climbing.domain.util

/**
 * Доменная ошибка (NFR-003). Строится из OpenAPI `ErrorResponse` или из сетевого сбоя.
 *
 * @param code машиночитаемый код (см. [ErrorCode]);
 * @param message понятное пользователю сообщение с сервера (если есть);
 * @param httpStatus HTTP-статус ответа, если применимо.
 */
data class AppError(
    val code: ErrorCode,
    val message: String? = null,
    val httpStatus: Int? = null,
    val cause: Throwable? = null,
    /** Актуальный слот из BookingConflictResponse (409, FR-014). */
    val conflictSlot: ru.vertical.climbing.domain.model.TrainingSlot? = null,
)

/**
 * Каталог кодов ошибок из OpenAPI + инфраструктурные коды.
 * Presentation-слой мапит код на локализованное сообщение (см. strings).
 */
enum class ErrorCode {
    // --- Клиент / авторизация ---
    CLIENT_ALREADY_EXISTS,
    UNAUTHORIZED,

    // --- Запись ---
    INSTRUCTOR_CLEARANCE_REQUIRED,
    RISK_CONSENT_REQUIRED,
    NO_FREE_SPOTS,
    BOOKING_CUTOFF_EXCEEDED,
    BOOKING_CONFLICT,
    CANCELLATION_FORBIDDEN,
    RENTAL_UNAVAILABLE,
    FORBIDDEN,

    // --- Оценка (Post-MVP) ---
    RATING_NOT_ALLOWED_GYM_CANCELLED,
    RATING_WINDOW_EXPIRED,
    RATING_ALREADY_SUBMITTED,

    // --- Общие HTTP ---
    BAD_REQUEST,
    NOT_FOUND,
    CONFLICT,
    SERVER_ERROR,

    // --- Инфраструктура ---
    NETWORK,
    SERIALIZATION,
    UNKNOWN,
    ;

    companion object {
        /** Разбор строкового кода из `ErrorResponse.code`. */
        fun fromApiCode(raw: String?): ErrorCode = when (raw?.uppercase()) {
            "CLIENT_ALREADY_EXISTS" -> CLIENT_ALREADY_EXISTS
            "UNAUTHORIZED" -> UNAUTHORIZED
            "INSTRUCTOR_CLEARANCE_REQUIRED" -> INSTRUCTOR_CLEARANCE_REQUIRED
            "RISK_CONSENT_REQUIRED" -> RISK_CONSENT_REQUIRED
            "NO_FREE_SPOTS" -> NO_FREE_SPOTS
            "BOOKING_CUTOFF_EXCEEDED" -> BOOKING_CUTOFF_EXCEEDED
            "BOOKING_CONFLICT" -> BOOKING_CONFLICT
            "CANCELLATION_FORBIDDEN" -> CANCELLATION_FORBIDDEN
            "RENTAL_UNAVAILABLE" -> RENTAL_UNAVAILABLE
            "RATING_NOT_ALLOWED_GYM_CANCELLED" -> RATING_NOT_ALLOWED_GYM_CANCELLED
            "RATING_WINDOW_EXPIRED" -> RATING_WINDOW_EXPIRED
            "RATING_ALREADY_SUBMITTED" -> RATING_ALREADY_SUBMITTED
            "NOT_FOUND" -> NOT_FOUND
            else -> UNKNOWN
        }
    }
}
