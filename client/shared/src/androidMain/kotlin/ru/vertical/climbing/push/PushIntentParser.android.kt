package ru.vertical.climbing.push

import android.content.Intent
import android.net.Uri
import android.os.Build
import ru.vertical.climbing.domain.model.parsePushData

/** Разбор Intent → push/deep link событие (LOGIC-013). */
fun Intent.toPushNavigationEvent(): PushNavigationEvent? {
    data?.toString()?.let { uri ->
        if (uri.startsWith("vertical://")) {
            return PushNavigationEvent(
                payload = null,
                deepLink = uri,
                source = PushDeliverySource.DEEP_LINK,
                showPreview = false,
            )
        }
    }

    val dataMap = extras?.toPushDataMap() ?: return null
    val payload = parsePushData(dataMap) ?: return null
    return PushNavigationEvent(
        payload = payload,
        source = PushDeliverySource.COLD_START,
        showPreview = !payload.isValid || payload.type.isNotBlank(),
    )
}

private fun android.os.Bundle.toPushDataMap(): Map<String, String> = buildMap {
    for (key in keySet()) {
        when (val value = get(key)) {
            is String -> put(key, value)
        }
    }
    // Flat extras from notification tap
    getString(PushIntentExtras.PUSH_TYPE)?.let { put("type", it) }
    getString(PushIntentExtras.PUSH_BOOKING_ID)?.let { put("booking_id", it) }
    getString(PushIntentExtras.PUSH_SLOT_ID)?.let { put("slot_id", it) }
    getString(PushIntentExtras.PUSH_CANCELLATION_REASON)?.let { put("cancellation_reason_code", it) }
    getString(PushIntentExtras.DEEP_LINK)?.let { /* handled above */ }
}

/** Запрос runtime-разрешения POST_NOTIFICATIONS (Android 13+). */
fun android.app.Activity.requestNotificationPermissionIfNeeded(requestCode: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), requestCode)
        }
    }
}

fun Uri?.toVerticalDeepLink(): String? = this?.toString()?.takeIf { it.startsWith("vertical://") }
