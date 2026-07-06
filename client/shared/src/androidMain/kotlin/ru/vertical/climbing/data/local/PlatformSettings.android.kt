package ru.vertical.climbing.data.local

import com.russhwolf.settings.Settings

/**
 * Android: `Settings()` из multiplatform-settings-no-arg (backed by SharedPreferences,
 * context подтягивается через androidx.startup).
 *
 * TODO(Итерация 1): для [secure] заменить на EncryptedSharedPreferences.
 */
actual object PlatformSettings {
    actual fun secure(): Settings = Settings()
    actual fun cache(): Settings = Settings()
}
