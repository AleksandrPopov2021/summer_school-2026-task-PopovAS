package ru.vertical.climbing.domain.model

/** LOGIC-008 — можно открыть SCR-008 (не forbidden, запись активна). */
fun Booking.canOpenCancellationScreen(): Boolean {
    if (bookingStatus != BookingStatus.BOOKED) return false
    val policy = cancellationPolicy ?: return true
    return policy.canCancel && policy.warningLevel != CancellationWarningLevel.FORBIDDEN
}

/** Отмена запрещена политикой (< 1 ч, FR-019). */
fun Booking.isCancellationForbidden(): Boolean {
    if (bookingStatus != BookingStatus.BOOKED) return true
    val policy = cancellationPolicy ?: return false
    return !policy.canCancel || policy.warningLevel == CancellationWarningLevel.FORBIDDEN
}

/** Показывать действие «Отменить» на SCR-006/007. */
fun Booking.showCancelAction(): Boolean = bookingStatus == BookingStatus.BOOKED

/** LOGIC-008 — активна ли кнопка «Подтвердить отмену» на SCR-008. */
fun CancellationPolicy?.isCancellationConfirmEnabled(): Boolean {
    if (this == null) return true
    return canCancel && warningLevel != CancellationWarningLevel.FORBIDDEN
}
