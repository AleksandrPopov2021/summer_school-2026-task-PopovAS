package ru.vertical.climbing.push

import ru.vertical.climbing.domain.model.DevicePlatform
import platform.Foundation.NSUUID

/**
 * iOS push-токен (LOGIC-012).
 * APNs token передаётся из Swift через [IosPushBridge]; до этого — dev-fallback.
 */
class IosPushTokenProvider : PushTokenProvider {

    override val platform: DevicePlatform = DevicePlatform.IOS

    private var refreshListener: ((String) -> Unit)? = null
    private var apnsToken: String? = null

    override suspend fun requestPermission(): Boolean = IosPushBridge.requestPermission()

    override suspend fun getToken(): String? = apnsToken ?: devFallbackToken()

    override fun setTokenRefreshListener(listener: ((String) -> Unit)?) {
        refreshListener = listener
    }

    fun updateApnsToken(token: String) {
        apnsToken = token
        refreshListener?.invoke(token)
    }

    private fun devFallbackToken(): String {
        val uuid = NSUUID().UUIDString()
        return "dev-ios-$uuid"
    }
}

/** Мост для Swift → Kotlin (APNs permission / token). */
object IosPushBridge {
    var requestPermissionHandler: (() -> Boolean)? = null
    var tokenUpdateHandler: ((String) -> Unit)? = null

    fun requestPermission(): Boolean = requestPermissionHandler?.invoke() ?: true

    fun onApnsTokenReceived(token: String) {
        tokenUpdateHandler?.invoke(token)
    }
}
