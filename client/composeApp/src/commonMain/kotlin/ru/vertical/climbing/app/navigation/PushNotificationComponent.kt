package ru.vertical.climbing.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import ru.vertical.climbing.domain.model.PushNotificationPayload
import ru.vertical.climbing.domain.model.PushNotificationType
import ru.vertical.climbing.domain.model.primaryPushNavigationTarget

/** SCR-014 — контекстный экран push-уведомления (LOGIC-013). */
interface PushNotificationComponent {
    val model: Value<Model>

    fun onPrimaryAction()
    fun onDismiss()

    data class Model(
        val payload: PushNotificationPayload,
        val isInvalid: Boolean = !payload.isValid,
    )
}

class DefaultPushNotificationComponent(
    componentContext: ComponentContext,
    payload: PushNotificationPayload,
    private val onNavigate: (PushNotificationPayload) -> Unit,
    private val onDismissRequested: () -> Unit,
) : PushNotificationComponent, ComponentContext by componentContext {

    private val _model = MutableValue(
        PushNotificationComponent.Model(payload = payload),
    )
    override val model: Value<PushNotificationComponent.Model> = _model

    override fun onPrimaryAction() {
        val payload = _model.value.payload
        if (!_model.value.isInvalid && primaryPushNavigationTarget(payload) != null) {
            onNavigate(payload)
        } else {
            onDismissRequested()
        }
    }

    override fun onDismiss() = onDismissRequested()
}

/** UI-вариант SCR-014 по типу push. */
enum class PushNotificationVariant {
    BOOKING_CONFIRMED,
    REMINDER,
    GYM_CANCELLATION,
    RATING_INVITATION,
    INVALID,
}

fun PushNotificationPayload.toVariant(): PushNotificationVariant = when (notificationType) {
    PushNotificationType.BOOKING_CONFIRMED -> PushNotificationVariant.BOOKING_CONFIRMED
    PushNotificationType.REMINDER -> PushNotificationVariant.REMINDER
    PushNotificationType.GYM_CANCELLATION -> PushNotificationVariant.GYM_CANCELLATION
    PushNotificationType.RATING_INVITATION -> PushNotificationVariant.RATING_INVITATION
    PushNotificationType.UNKNOWN -> PushNotificationVariant.INVALID
}
