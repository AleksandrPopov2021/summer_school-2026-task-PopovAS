package ru.vertical.climbing.data.remote

/** Окружение сборки (dev/staging/prod) — переключается debug-меню в Итерации 9. */
enum class ApiEnvironment(val baseUrl: String) {
    /**
     * Локальный backend для отладки.
     * `10.0.2.2` — это localhost хост-машины с точки зрения Android-эмулятора.
     * Для iOS-симулятора/desktop используйте `http://localhost:8080/v1`,
     * для физического устройства — IP вашего ПК в локальной сети.
     */
    LOCAL("http://10.0.2.2:8080/v1"),
    DEV("https://staging-api.vertical-climbing.ru/v1"),
    STAGING("https://staging-api.vertical-climbing.ru/v1"),
    PROD("https://api.vertical-climbing.ru/v1"),
}

/**
 * Конфигурация API-клиента.
 *
 * @param environment выбранное окружение (Base URL берётся отсюда);
 * @param useMock использовать mock-движок Ktor вместо реальной сети (разработка до backend).
 */
data class ApiConfig(
    val environment: ApiEnvironment = ApiEnvironment.STAGING,
    val useMock: Boolean = true,
    val enableLogging: Boolean = true,
) {
    val baseUrl: String get() = environment.baseUrl
}
