package ru.vertical.climbing.domain.model

/** Редактируемые переключатели уведомлений (BR-029). */
data class EditableNotificationToggles(
    val bookingConfirmationEnabled: Boolean,
    val ratingInvitationEnabled: Boolean,
)

fun NotificationPreferences.toEditableToggles(): EditableNotificationToggles = EditableNotificationToggles(
    bookingConfirmationEnabled = bookingConfirmationEnabled,
    ratingInvitationEnabled = ratingInvitationEnabled,
)

/** Есть ли несохранённые изменения отключаемых toggles (LOGIC-011). */
fun hasUnsavedNotificationChanges(
    saved: NotificationPreferences,
    local: EditableNotificationToggles,
): Boolean = saved.toEditableToggles() != local

/** Locked toggles всегда ON в UI, независимо от ответа API (AC-E01, BR-028). */
fun lockedRemindersEnabled(): Boolean = true

fun lockedGymCancellationEnabled(): Boolean = true
