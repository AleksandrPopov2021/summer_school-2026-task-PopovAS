package ru.vertical.climbing.push

import ru.vertical.climbing.domain.model.DevicePlatform

/** Платформенный провайдер push-токена (LOGIC-012). */
interface PushTokenProvider {
    val platform: DevicePlatform

    /** Запрос разрешения ОС (если ещё не выдано). */
    suspend fun requestPermission(): Boolean

    /** FCM/APNs token или dev-fallback. */
    suspend fun getToken(): String?

    /** Подписка на обновление токена (onTokenRefresh). */
    fun setTokenRefreshListener(listener: ((String) -> Unit)?)
}
