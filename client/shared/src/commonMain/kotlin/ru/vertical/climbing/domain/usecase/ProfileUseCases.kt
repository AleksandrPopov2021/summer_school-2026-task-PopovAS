package ru.vertical.climbing.domain.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import ru.vertical.climbing.domain.model.Client
import ru.vertical.climbing.domain.model.EditableNotificationToggles
import ru.vertical.climbing.domain.model.NotificationPreferences
import ru.vertical.climbing.domain.repository.AuthRepository
import ru.vertical.climbing.domain.repository.ConfigRepository
import ru.vertical.climbing.domain.repository.NotificationPreferencesRepository
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.ErrorCode

/** Fallback N для прогресса лояльности (SCR-010, AC-E02). */
const val DEFAULT_VISITS_FOR_LOYALTY = 10

/** Данные профиля для SCR-010. */
data class ProfileData(
    val client: Client,
    val visitsForLoyalty: Int,
    val isStale: Boolean = false,
)

/** SCR-010 — параллельная загрузка профиля и порога лояльности. */
class LoadProfileUseCase(
    private val authRepository: AuthRepository,
    private val configRepository: ConfigRepository,
) {
    suspend operator fun invoke(cachedClient: Client? = null): AppResult<ProfileData> = coroutineScope {
        val configDeferred = async { configRepository.getConfig() }
        when (val clientResult = authRepository.getCurrentClient()) {
            is AppResult.Success -> {
                val visitsForLoyalty = resolveVisitsForLoyalty(configDeferred.await())
                AppResult.Success(ProfileData(client = clientResult.value, visitsForLoyalty = visitsForLoyalty))
            }
            is AppResult.Failure -> when (clientResult.error.code) {
                ErrorCode.NETWORK -> if (cachedClient != null) {
                    val visitsForLoyalty = resolveVisitsForLoyalty(configDeferred.await())
                    AppResult.Success(
                        ProfileData(
                            client = cachedClient,
                            visitsForLoyalty = visitsForLoyalty,
                            isStale = true,
                        ),
                    )
                } else {
                    clientResult
                }
                else -> clientResult
            }
        }
    }

    private suspend fun resolveVisitsForLoyalty(configResult: AppResult<ru.vertical.climbing.domain.model.SystemConfig>): Int =
        when (configResult) {
            is AppResult.Success -> configResult.value.visitsForLoyalty
            is AppResult.Failure -> configRepository.cachedConfig()?.visitsForLoyalty ?: DEFAULT_VISITS_FOR_LOYALTY
        }
}

/** SCR-010 — выход из аккаунта (очистка JWT). */
class SignOutUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() {
        authRepository.clearToken()
    }
}

/** LOGIC-011 — загрузка настроек уведомлений. */
class GetNotificationPreferencesUseCase(
    private val repository: NotificationPreferencesRepository,
) {
    suspend operator fun invoke(): AppResult<NotificationPreferences> = repository.get()
}

/**
 * LOGIC-011 — сохранение только отключаемых toggles (BR-029).
 * Locked-поля (`reminders_enabled`, `gym_cancellation_enabled`) не передаются.
 */
class UpdateNotificationPreferencesUseCase(
    private val repository: NotificationPreferencesRepository,
) {
    suspend operator fun invoke(local: EditableNotificationToggles): AppResult<NotificationPreferences> =
        repository.update(
            bookingConfirmationEnabled = local.bookingConfirmationEnabled,
            ratingInvitationEnabled = local.ratingInvitationEnabled,
        )
}
