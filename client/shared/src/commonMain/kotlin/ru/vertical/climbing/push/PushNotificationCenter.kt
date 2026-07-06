package ru.vertical.climbing.push

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import ru.vertical.climbing.domain.model.PushNotificationPayload

/** Источник доставки push-события. */
enum class PushDeliverySource {
    NOTIFICATION_TAP,
    FOREGROUND,
    DEEP_LINK,
    COLD_START,
}

/** Событие push / deep link для UI (LOGIC-013). */
data class PushNavigationEvent(
    val payload: PushNotificationPayload?,
    val deepLink: String? = null,
    val source: PushDeliverySource,
    val showPreview: Boolean,
)

/**
 * Центральная шина push-событий (commonMain).
 * Платформа публует сюда tap / foreground / cold start.
 */
object PushNotificationCenter {
    private val _events = MutableSharedFlow<PushNavigationEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<PushNavigationEvent> = _events.asSharedFlow()

    var coldStartEvent: PushNavigationEvent? = null

    fun publish(event: PushNavigationEvent) {
        _events.tryEmit(event)
    }

    fun setColdStart(event: PushNavigationEvent?) {
        coldStartEvent = event
    }
}
