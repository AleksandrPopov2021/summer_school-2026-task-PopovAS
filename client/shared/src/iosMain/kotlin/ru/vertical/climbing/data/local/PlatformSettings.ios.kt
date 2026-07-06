package ru.vertical.climbing.data.local

import com.russhwolf.settings.Settings

/**
 * iOS: `Settings()` из multiplatform-settings-no-arg (backed by NSUserDefaults).
 *
 * TODO(Итерация 1): для [secure] заменить на KeychainSettings.
 */
actual object PlatformSettings {
    actual fun secure(): Settings = Settings()
    actual fun cache(): Settings = Settings()
}
