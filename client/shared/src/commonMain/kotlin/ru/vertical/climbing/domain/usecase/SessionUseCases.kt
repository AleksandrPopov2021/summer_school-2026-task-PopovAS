package ru.vertical.climbing.domain.usecase

import ru.vertical.climbing.domain.model.AuthSession
import ru.vertical.climbing.domain.model.Client
import ru.vertical.climbing.domain.model.ClientRegistration
import ru.vertical.climbing.domain.repository.AuthRepository
import ru.vertical.climbing.domain.repository.ConfigRepository
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.ErrorCode
import ru.vertical.climbing.domain.util.onSuccess

/** Итог проверки сессии при старте (LOGIC-001). */
sealed interface SessionState {
    /** Токена нет — показать регистрацию (SCR-002). */
    data object Unauthenticated : SessionState

    /** Токен валиден — показать расписание (SCR-003). */
    data class Authenticated(val client: Client) : SessionState

    /** Нет сети, но есть кэш конфига — расписание с баннером offline. */
    data object Offline : SessionState
}

/**
 * LOGIC-001 — Проверка сессии при запуске (SCR-001).
 *
 * - нет токена → best-effort кэширование `config` → SCR-002 (регистрация);
 * - токен валиден (200 `/clients/me`) → кэш `config` → SCR-003 (расписание);
 * - 401 → очистить токен → SCR-002;
 * - нет сети при валидном токене → SCR-003 из кэша с баннером offline;
 * - прочие ошибки → пробросить для показа retry на splash.
 *
 * `getSystemConfig` некритичен (SCR-001): его ошибки не блокируют маршрутизацию.
 */
class CheckSessionUseCase(
    private val authRepository: AuthRepository,
    private val configRepository: ConfigRepository,
) {
    suspend operator fun invoke(): AppResult<SessionState> {
        if (!authRepository.hasToken()) {
            configRepository.getConfig() // best-effort, ошибки игнорируем
            return AppResult.Success(SessionState.Unauthenticated)
        }
        return when (val me = authRepository.getCurrentClient()) {
            is AppResult.Success -> {
                configRepository.getConfig()
                AppResult.Success(SessionState.Authenticated(me.value))
            }
            is AppResult.Failure -> when (me.error.code) {
                ErrorCode.UNAUTHORIZED -> {
                    authRepository.clearToken()
                    AppResult.Success(SessionState.Unauthenticated)
                }
                ErrorCode.NETWORK -> AppResult.Success(SessionState.Offline)
                else -> me
            }
        }
    }
}

/** LOGIC-002 — Регистрация клиента. */
class RegisterClientUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(registration: ClientRegistration): AppResult<AuthSession> =
        authRepository.register(registration).onSuccess { session ->
            authRepository.saveToken(session.accessToken)
        }
}

/** LOGIC-006 — Подтверждение согласия на риск (SCR-015). */
class AcceptRiskConsentUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): AppResult<Client> = authRepository.acceptRiskConsent()
}

/** SCR-010 — Профиль текущего клиента. */
class GetProfileUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): AppResult<Client> = authRepository.getCurrentClient()
}
