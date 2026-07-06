package ru.vertical.climbing.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Тип push-уведомления (LOGIC-013). */
enum class PushNotificationType {
    BOOKING_CONFIRMED,
    REMINDER,
    GYM_CANCELLATION,
    RATING_INVITATION,
    UNKNOWN,
}

/** Данные слота из push payload (опционально). */
@Serializable
data class PushSlotInfo(
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("zone_format") val zoneFormat: String? = null,
    val address: String? = null,
    @SerialName("instructor_name") val instructorName: String? = null,
)

/** Push / deep link payload (LOGIC-013). */
@Serializable
data class PushNotificationPayload(
    val type: String,
    @SerialName("booking_id") val bookingId: String? = null,
    @SerialName("slot_id") val slotId: String? = null,
    @SerialName("reminder_kind") val reminderKind: String? = null,
    @SerialName("cancellation_reason") val cancellationReason: String? = null,
    @SerialName("cancellation_reason_code") val cancellationReasonCode: String? = null,
    @SerialName("alternative_slot_id") val alternativeSlotId: String? = null,
    val slot: PushSlotInfo? = null,
) {
    val notificationType: PushNotificationType
        get() = when (type.lowercase()) {
            "booking_confirmed" -> PushNotificationType.BOOKING_CONFIRMED
            "reminder" -> PushNotificationType.REMINDER
            "gym_cancellation" -> PushNotificationType.GYM_CANCELLATION
            "rating_invitation" -> PushNotificationType.RATING_INVITATION
            else -> PushNotificationType.UNKNOWN
        }

    val isValid: Boolean
        get() = when (notificationType) {
            PushNotificationType.BOOKING_CONFIRMED,
            PushNotificationType.REMINDER,
            PushNotificationType.RATING_INVITATION,
            -> !bookingId.isNullOrBlank()
            PushNotificationType.GYM_CANCELLATION ->
                !bookingId.isNullOrBlank() && !slotId.isNullOrBlank()
            PushNotificationType.UNKNOWN -> false
        }
}

/** Целевой экран после primary CTA на SCR-014 или прямой маршрутизации. */
sealed interface PushNavigationTarget {
    data object BookingsList : PushNavigationTarget
    data class BookingDetail(val bookingId: String) : PushNavigationTarget
    data class AlternativeSlot(val bookingId: String, val cancelledSlotId: String) : PushNavigationTarget
    data class RatingStub(val bookingId: String) : PushNavigationTarget
    data object Schedule : PushNavigationTarget
    data class ScheduleDate(val date: LocalDate) : PushNavigationTarget
}

/** Primary CTA SCR-014 по типу уведомления. */
fun primaryPushNavigationTarget(payload: PushNotificationPayload): PushNavigationTarget? =
    when (payload.notificationType) {
        PushNotificationType.BOOKING_CONFIRMED -> PushNavigationTarget.BookingsList
        PushNotificationType.REMINDER -> payload.bookingId?.let(PushNavigationTarget::BookingDetail)
        PushNotificationType.GYM_CANCELLATION -> {
            val bookingId = payload.bookingId ?: return null
            val slotId = payload.slotId ?: return null
            PushNavigationTarget.AlternativeSlot(bookingId, slotId)
        }
        PushNotificationType.RATING_INVITATION -> payload.bookingId?.let(PushNavigationTarget::RatingStub)
        PushNotificationType.UNKNOWN -> null
    }

/** Прямая маршрутизация без SCR-014 (deep link / programmatic). */
fun directPushNavigationTarget(payload: PushNotificationPayload): PushNavigationTarget? =
    when (payload.notificationType) {
        PushNotificationType.BOOKING_CONFIRMED -> payload.bookingId?.let(PushNavigationTarget::BookingDetail)
            ?: PushNavigationTarget.BookingsList
        PushNotificationType.REMINDER -> payload.bookingId?.let(PushNavigationTarget::BookingDetail)
        PushNotificationType.GYM_CANCELLATION -> {
            val bookingId = payload.bookingId ?: return null
            val slotId = payload.slotId ?: return null
            PushNavigationTarget.AlternativeSlot(bookingId, slotId)
        }
        PushNotificationType.RATING_INVITATION -> payload.bookingId?.let(PushNavigationTarget::RatingStub)
        PushNotificationType.UNKNOWN -> PushNavigationTarget.Schedule
    }

private val pushJson = Json { ignoreUnknownKeys = true }

/** Парсинг FCM/APNs data map → payload. */
fun parsePushData(data: Map<String, String>): PushNotificationPayload? {
    val type = data["type"] ?: data["payload"]?.let { raw ->
        return runCatching { pushJson.decodeFromString(PushNotificationPayload.serializer(), raw) }.getOrNull()
    } ?: return null

    return PushNotificationPayload(
        type = type,
        bookingId = data["booking_id"],
        slotId = data["slot_id"],
        reminderKind = data["reminder_kind"],
        cancellationReason = data["cancellation_reason"],
        cancellationReasonCode = data["cancellation_reason_code"],
        alternativeSlotId = data["alternative_slot_id"],
        slot = PushSlotInfo(
            startsAt = data["starts_at"],
            zoneFormat = data["zone_format"],
            address = data["address"],
            instructorName = data["instructor_name"],
        ).takeIf { slot ->
            listOf(slot.startsAt, slot.zoneFormat, slot.address, slot.instructorName).any { !it.isNullOrBlank() }
        },
    )
}

/** Разбор deep link `vertical://...` (LOGIC-013). */
fun parseDeepLink(uri: String): PushNavigationTarget? {
    val normalized = uri.removePrefix("vertical://").trim('/')
    if (normalized.isBlank()) return PushNavigationTarget.Schedule

    val segments = normalized.split('/').filter { it.isNotBlank() }
    return when (segments.firstOrNull()) {
        "schedule" -> {
            val dateParam = uri.substringAfter("date=", "").takeIf { it.isNotBlank() }
            dateParam?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?.let(PushNavigationTarget::ScheduleDate)
                ?: PushNavigationTarget.Schedule
        }
        "bookings" -> {
            val bookingId = segments.getOrNull(1) ?: return null
            when (segments.getOrNull(2)) {
                "alternative" -> PushNavigationTarget.AlternativeSlot(
                    bookingId = bookingId,
                    cancelledSlotId = segments.getOrNull(3).orEmpty().ifBlank { bookingId },
                )
                else -> PushNavigationTarget.BookingDetail(bookingId)
            }
        }
        "ratings" -> segments.getOrNull(1)?.let(PushNavigationTarget::RatingStub)
        "notification" -> {
            val params = uri.substringAfter('?', "")
                .split('&')
                .mapNotNull { part ->
                    val (k, v) = part.split('=', limit = 2).let { it[0] to it.getOrNull(1) }
                    v?.let { k to it }
                }
                .toMap()
            parsePushData(params)?.let { directPushNavigationTarget(it) }
        }
        else -> null
    }
}
