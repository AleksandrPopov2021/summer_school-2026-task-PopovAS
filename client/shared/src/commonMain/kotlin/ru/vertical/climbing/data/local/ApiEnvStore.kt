package ru.vertical.climbing.data.local

import com.russhwolf.settings.Settings
import ru.vertical.climbing.data.remote.ApiConfig
import ru.vertical.climbing.data.remote.ApiEnvironment
import ru.vertical.climbing.platform.isDebugBuild

/**
 * Хранение выбранного окружения API (debug-only, Итерация 9).
 * Release-сборки всегда используют prod без mock.
 */
class ApiEnvStore(
    private val settings: Settings,
) {
    fun load(): ApiConfig {
        if (!isDebugBuild()) {
            return ApiConfig(
                environment = ApiEnvironment.PROD,
                useMock = false,
                enableLogging = false,
            )
        }
        val envName = settings.getStringOrNull(StorageKeys.API_ENVIRONMENT)
        val environment = envName?.let { runCatching { ApiEnvironment.valueOf(it) }.getOrNull() }
            ?: ApiEnvironment.STAGING
        val useMock = settings.getBoolean(StorageKeys.API_USE_MOCK, defaultValue = true)
        return ApiConfig(
            environment = environment,
            useMock = useMock,
            enableLogging = true,
        )
    }

    fun save(environment: ApiEnvironment, useMock: Boolean) {
        settings.putString(StorageKeys.API_ENVIRONMENT, environment.name)
        settings.putBoolean(StorageKeys.API_USE_MOCK, useMock)
    }

    fun currentEnvironment(): ApiEnvironment = load().environment
    fun isMockEnabled(): Boolean = load().useMock
}
