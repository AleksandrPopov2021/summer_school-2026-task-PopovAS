package ru.vertical.climbing.domain.usecase

import kotlinx.coroutines.delay
import ru.vertical.climbing.domain.model.PushTokenRegistration
import ru.vertical.climbing.domain.repository.AuthRepository
import ru.vertical.climbing.domain.repository.DeviceRepository
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.ErrorCode
import ru.vertical.climbing.push.PushTokenProvider

/**
 * LOGIC-012 — фоновая регистрация push-токена после авторизации.
 * Ошибки сети / 5xx — до 3 retry с backoff; UI не блокируется.
 */
class RegisterPushTokenUseCase(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository,
    private val pushTokenProvider: PushTokenProvider,
) {
    suspend operator fun invoke() {
        if (!authRepository.hasToken()) return
        if (!pushTokenProvider.requestPermission()) return
        val token = pushTokenProvider.getToken() ?: return
        registerWithRetry(token)
    }

    suspend fun registerToken(token: String) {
        if (!authRepository.hasToken()) return
        registerWithRetry(token)
    }

    private suspend fun registerWithRetry(token: String) {
        var attempt = 0
        var backoffMs = 1_000L
        while (attempt < MAX_ATTEMPTS) {
            when (val result = deviceRepository.registerPushToken(
                PushTokenRegistration(token = token, platform = pushTokenProvider.platform),
            )) {
                is AppResult.Success -> return
                is AppResult.Failure -> when (result.error.code) {
                    ErrorCode.UNAUTHORIZED, ErrorCode.BAD_REQUEST -> return
                    else -> {
                        attempt++
                        if (attempt >= MAX_ATTEMPTS) return
                        delay(backoffMs)
                        backoffMs *= 2
                    }
                }
            }
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}

/** Координатор LOGIC-012: регистрация + refresh токена. */
class PushRegistrationCoordinator(
    private val registerPushToken: RegisterPushTokenUseCase,
    private val pushTokenProvider: PushTokenProvider,
) {
    fun bindTokenRefresh(handler: (String) -> Unit) {
        pushTokenProvider.setTokenRefreshListener(handler)
    }

    suspend fun registerIfNeeded() {
        registerPushToken()
    }

    suspend fun onTokenRefreshed(token: String) {
        registerPushToken.registerToken(token)
    }
}
