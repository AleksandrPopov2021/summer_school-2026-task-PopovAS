package ru.vertical.climbing.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.plus

/** Окно оценки после тренировки (BR-033, backend: RatingWindowHours = 48). */
const val RATING_WINDOW_HOURS = 48

/** Результат проверки доступности оценки (LOGIC-014, SCR-012). */
sealed interface RatingAvailability {
    data object Available : RatingAvailability
    data object GymCancelled : RatingAvailability
    data object Expired : RatingAvailability
    data class AlreadyRated(val stars: Int) : RatingAvailability
    data object NotEligible : RatingAvailability
}

/** Конец тренировки + окно оценки. */
fun ratingDeadline(slot: TrainingSlot): Instant {
    val slotEnd = slot.startsAt.plus(slot.durationMinutes, DateTimeUnit.MINUTE)
    return slotEnd.plus(RATING_WINDOW_HOURS, DateTimeUnit.HOUR)
}

/** Истекло ли окно 1–2 суток после окончания тренировки. */
fun isRatingWindowExpired(slot: TrainingSlot, now: Instant = Clock.System.now()): Boolean =
    now > ratingDeadline(slot)

/**
 * Локальная проверка доступности оценки (LOGIC-014 шаг 1–2).
 * [submittedStars] — звёзды из локального кэша или ответа 409.
 */
fun resolveRatingAvailability(
    booking: Booking,
    submittedStars: Int? = null,
    now: Instant = Clock.System.now(),
): RatingAvailability {
    submittedStars?.let { return RatingAvailability.AlreadyRated(it) }
    return when (booking.bookingStatus) {
        BookingStatus.CANCELLED_BY_GYM -> RatingAvailability.GymCancelled
        BookingStatus.COMPLETED -> when {
            isRatingWindowExpired(booking.slot, now) -> RatingAvailability.Expired
            else -> RatingAvailability.Available
        }
        else -> RatingAvailability.NotEligible
    }
}

/** Показывать ли CTA «Оценить» на SCR-006. */
fun canPromptRating(
    booking: Booking,
    submittedStars: Int? = null,
    now: Instant = Clock.System.now(),
): Boolean = resolveRatingAvailability(booking, submittedStars, now) is RatingAvailability.Available
