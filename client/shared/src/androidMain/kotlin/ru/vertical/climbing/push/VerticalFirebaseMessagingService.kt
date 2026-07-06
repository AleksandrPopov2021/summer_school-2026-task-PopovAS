package ru.vertical.climbing.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import ru.vertical.climbing.domain.model.parsePushData

/**
 * FCM-сервис (LOGIC-012/013).
 * При tap система открывает MainActivity с extras; foreground — in-app banner.
 */
class VerticalFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        (applicationContext.applicationContext as? PushTokenRefreshHost)?.onPushTokenRefreshed(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val payload = parsePushData(message.data) ?: return
        PushNotificationCenter.publish(
            PushNavigationEvent(
                payload = payload,
                source = PushDeliverySource.FOREGROUND,
                showPreview = true,
            ),
        )
    }
}

/** Host Application для callback refresh токена. */
interface PushTokenRefreshHost {
    fun onPushTokenRefreshed(token: String)
}

/** Intent extras для cold start / tap. */
object PushIntentExtras {
    const val PUSH_TYPE = "vertical_push_type"
    const val PUSH_BOOKING_ID = "vertical_push_booking_id"
    const val PUSH_SLOT_ID = "vertical_push_slot_id"
    const val PUSH_CANCELLATION_REASON = "vertical_push_cancellation_reason"
    const val DEEP_LINK = "vertical_deep_link"

    fun toMap(type: String, bookingId: String?, slotId: String?, reason: String?): Map<String, String> =
        buildMap {
            put("type", type)
            bookingId?.let { put("booking_id", it) }
            slotId?.let { put("slot_id", it) }
            reason?.let { put("cancellation_reason_code", it) }
        }
}
