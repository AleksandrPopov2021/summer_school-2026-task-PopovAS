package ru.vertical.climbing.push

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import java.util.UUID
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.vertical.climbing.domain.model.DevicePlatform
import kotlin.coroutines.resume

/**
 * Android push-токен: FCM при наличии Firebase, иначе dev-fallback (LOGIC-012).
 * Канал уведомлений создаётся в [AndroidNotificationChannels].
 */
class AndroidPushTokenProvider(
    private val context: Context,
) : PushTokenProvider {

    override val platform: DevicePlatform = DevicePlatform.ANDROID

    private var refreshListener: ((String) -> Unit)? = null

    override suspend fun requestPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun getToken(): String? = runCatching {
        suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener {
                    cont.resume(devFallbackToken())
                }
        }
    }.getOrElse { devFallbackToken() }

    override fun setTokenRefreshListener(listener: ((String) -> Unit)?) {
        refreshListener = listener
    }

    internal fun notifyTokenRefresh(token: String) {
        refreshListener?.invoke(token)
    }

    private fun devFallbackToken(): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TOKEN, null) ?: UUID.randomUUID().toString().also { generated ->
            prefs.edit().putString(KEY_TOKEN, generated).apply()
        }.let { "dev-android-$it" }
    }

    companion object {
        private const val PREFS = "vertical_push"
        private const val KEY_TOKEN = "dev_token"
    }
}
