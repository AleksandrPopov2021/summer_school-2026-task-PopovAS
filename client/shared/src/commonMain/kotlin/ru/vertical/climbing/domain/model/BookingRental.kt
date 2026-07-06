package ru.vertical.climbing.domain.model

/** LOGIC-010 — можно изменить прокат (активная запись, FR-028). */
fun Booking.canModifyRental(): Boolean {
    if (bookingStatus != BookingStatus.BOOKED) return false
    val policy = cancellationPolicy ?: return true
    return policy.canCancel
}

/** Сравнение выбора проката для определения изменений (LOGIC-010). */
fun rentalSelectionEquals(
    usesOwnA: Boolean,
    linesA: List<RentalLineInput>,
    usesOwnB: Boolean,
    linesB: List<RentalLineInput>,
): Boolean {
    if (usesOwnA != usesOwnB) return false
    val normalizedA = linesA.map { it.equipmentTypeId to it.quantity }.toSet()
    val normalizedB = linesB.map { it.equipmentTypeId to it.quantity }.toSet()
    return normalizedA == normalizedB
}

/** Есть ли отличия от исходного выбора проката. */
fun hasRentalSelectionChanged(
    originalUsesOwn: Boolean,
    originalLines: List<RentalLineInput>,
    newUsesOwn: Boolean,
    newLines: List<RentalLineInput>,
): Boolean = !rentalSelectionEquals(originalUsesOwn, originalLines, newUsesOwn, newLines)
