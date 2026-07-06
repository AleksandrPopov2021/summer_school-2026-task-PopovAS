package ru.vertical.climbing.data.local

import com.russhwolf.settings.Settings

/**
 * Именованные хранилища key-value для платформы.
 *
 * - [secure] — для чувствительных данных (`access_token`). В Итерации 1 заменяется
 *   на Keychain (iOS) / EncryptedSharedPreferences (Android).
 * - [cache] — для нечувствительного кэша (config, slots, draft).
 */
expect object PlatformSettings {
    fun secure(): Settings
    fun cache(): Settings
}
