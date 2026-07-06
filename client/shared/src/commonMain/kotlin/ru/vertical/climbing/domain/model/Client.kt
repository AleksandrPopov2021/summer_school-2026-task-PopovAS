package ru.vertical.climbing.domain.model

import kotlinx.datetime.LocalDate

/** Клиент скалодрома (OpenAPI: Client, FR-026, FR-027). */
data class Client(
    val id: String,
    val fullName: String,
    val phone: String,
    val birthDate: LocalDate,
    val riskConsentAccepted: Boolean,
    val completedVisitsCount: Int,
    val isLoyalClient: Boolean,
    val loyaltyDiscount: Double?,
    val lateCancellationCount: Int,
    val noShowCount: Int,
)

/** Данные для регистрации клиента (OpenAPI: ClientRegistrationRequest). */
data class ClientRegistration(
    val phone: String,
    val fullName: String,
    val birthDate: LocalDate,
)

/** Результат авторизации: токен + профиль (OpenAPI: ClientRegistrationResponse). */
data class AuthSession(
    val accessToken: String,
    val client: Client,
)

/** Допуск инструктора (OpenAPI: InstructorClearance, BR-007). */
data class InstructorClearance(
    val id: String,
    val clientId: String,
    val instructorId: String?,
    val isGranted: Boolean,
)

/** Настройки уведомлений (OpenAPI: NotificationPreferences, FR-032). */
data class NotificationPreferences(
    val id: String,
    val clientId: String,
    val bookingConfirmationEnabled: Boolean,
    val ratingInvitationEnabled: Boolean,
    /** Всегда true, read-only (BR-028). */
    val remindersEnabled: Boolean,
    /** Всегда true, read-only (BR-028). */
    val gymCancellationEnabled: Boolean,
)
