package ru.vertical.climbing.domain.model

/** Статус слота тренировки (OpenAPI: SlotStatus). */
enum class SlotStatus {
    ACTIVE,
    CANCELLED_BY_GYM,
    UNKNOWN,
}

/** Формат тренировки (OpenAPI: FormatType). */
enum class FormatType {
    /** Болдеринг с инструктажем (новички). */
    BOULDERING_INSTRUCTION,

    /** Трассы с верёвкой (опытные, нужен допуск). */
    ROPE_ROUTES,
    UNKNOWN,
}

/** Сложность (OpenAPI: Difficulty). */
enum class Difficulty {
    BEGINNER,
    EXPERIENCED,
    UNKNOWN,
}

/** Статус записи (OpenAPI: BookingStatus). */
enum class BookingStatus {
    BOOKED,
    CANCELLED_BY_CLIENT,
    CANCELLED_BY_GYM,
    COMPLETED,
    NO_SHOW,
    UNKNOWN,
}

/** Статус оплаты (OpenAPI: PaymentStatus). */
enum class PaymentStatus {
    UNPAID,
    PAID,
    REFUND,
    UNKNOWN,
}

/** Код позиции проката (OpenAPI: RentalEquipmentCode). */
enum class RentalEquipmentCode {
    SHOES,
    HARNESS,
    HELMET,
    CHALK,
    UNKNOWN,
}

/** Уровень предупреждения при отмене (OpenAPI: CancellationPolicy.warning_level). */
enum class CancellationWarningLevel {
    /** Более 2 часов (BR-010). */
    NONE,

    /** 1–2 часа, показать предупреждение (BR-011). */
    LATE_CANCELLATION,

    /** Менее 1 часа (BR-012). */
    FORBIDDEN,
    UNKNOWN,
}

/** Платформа устройства для push (OpenAPI: PushTokenRequest.platform). */
enum class DevicePlatform {
    ANDROID,
    IOS,
}
