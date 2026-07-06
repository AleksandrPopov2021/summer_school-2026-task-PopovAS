package ru.vertical.climbing.domain.model

/** SCR-007 — показать CTA «Подобрать другой слот» (FR-021, FR-022). */
fun Booking.canFindAlternative(): Boolean = bookingStatus == BookingStatus.CANCELLED_BY_GYM

/**
 * LOGIC-009 — видимость кнопки «Записаться на этот слот» (LOGIC-004 + BR-018).
 */
fun canBookAlternativeSlot(
    alternative: TrainingSlot,
    cancelledSlotId: String,
    rebookingForbidden: Boolean,
): Boolean {
    if (rebookingForbidden && alternative.id == cancelledSlotId) return false
    return alternative.availability.canBook && alternative.availabilityState().isBookable
}

/**
 * BR-018 — запрет повторной записи на отменённый слот (SCR-003 / SCR-004).
 */
fun TrainingSlot.isRebookingBlocked(forbiddenSlotIds: Set<String>): Boolean = id in forbiddenSlotIds

fun Booking.contributesToRebookingBlock(): Boolean =
    bookingStatus == BookingStatus.CANCELLED_BY_GYM && rebookingForbidden
