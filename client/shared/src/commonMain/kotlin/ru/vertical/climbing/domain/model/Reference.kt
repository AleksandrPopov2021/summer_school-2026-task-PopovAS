package ru.vertical.climbing.domain.model

/** Тип прокатного снаряжения (OpenAPI: RentalEquipmentType, FR-010). */
data class RentalEquipmentType(
    val id: String,
    val code: RentalEquipmentCode,
    val name: String,
    val defaultPrice: Double,
)

/** Системные параметры бизнес-логики (OpenAPI: SystemConfig, NFR-006). */
data class SystemConfig(
    val reminderHoursBefore: Int,
    val visitsForLoyalty: Int,
    val violationsForSanctions: Int,
    val bookingCutoffMinutes: Int,
    val cancellationForbiddenMinutes: Int,
)

/** Push-токен устройства (OpenAPI: PushTokenRequest, LOGIC-012). */
data class PushTokenRegistration(
    val token: String,
    val platform: DevicePlatform,
)
