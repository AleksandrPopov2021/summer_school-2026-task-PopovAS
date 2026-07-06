package ru.vertical.climbing.data.local

/** Ключи локального хранилища (из ТЗ, раздел «Локальное хранилище»). */
object StorageKeys {
    /** JWT доступа — Keychain / EncryptedSharedPreferences. */
    const val ACCESS_TOKEN = "access_token"

    /** Кэш системных параметров. */
    const val CACHED_CONFIG = "cached_config"

    /** Кэш расписания для offline-просмотра. */
    const val CACHED_SLOTS = "cached_slots"

    /** Черновик записи до подтверждения. */
    const val BOOKING_DRAFT = "booking_draft"

    /** Кэш списка записей для offline-просмотра. */
    const val CACHED_BOOKINGS = "cached_bookings"

    /** Debug: выбранное окружение API (dev/staging/prod). */
    const val API_ENVIRONMENT = "api_environment"

    /** Debug: mock-режим API. */
    const val API_USE_MOCK = "api_use_mock"

    /** Локально отправленные оценки (booking_id → stars). */
    const val SUBMITTED_RATINGS = "submitted_ratings"
}
