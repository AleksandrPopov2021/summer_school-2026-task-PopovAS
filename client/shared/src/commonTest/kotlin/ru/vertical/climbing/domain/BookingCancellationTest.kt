package ru.vertical.climbing.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.Instant
import ru.vertical.climbing.domain.model.Booking
import ru.vertical.climbing.domain.model.BookingAvailability
import ru.vertical.climbing.domain.model.BookingStatus
import ru.vertical.climbing.domain.model.CancellationPolicy
import ru.vertical.climbing.domain.model.CancellationWarningLevel
import ru.vertical.climbing.domain.model.Difficulty
import ru.vertical.climbing.domain.model.FormatType
import ru.vertical.climbing.domain.model.Instructor
import ru.vertical.climbing.domain.model.PaymentInfo
import ru.vertical.climbing.domain.model.PaymentStatus
import ru.vertical.climbing.domain.model.SlotStatus
import ru.vertical.climbing.domain.model.TrainingSlot
import ru.vertical.climbing.domain.model.TrainingZone
import ru.vertical.climbing.domain.model.canOpenCancellationScreen
import ru.vertical.climbing.domain.model.isCancellationConfirmEnabled
import ru.vertical.climbing.domain.model.isCancellationForbidden

class BookingCancellationTest {

    @Test
    fun confirm_enabled_for_none_warning() {
        assertTrue(policy(CancellationWarningLevel.NONE).isCancellationConfirmEnabled())
    }

    @Test
    fun confirm_enabled_for_late_cancellation() {
        assertTrue(policy(CancellationWarningLevel.LATE_CANCELLATION).isCancellationConfirmEnabled())
    }

    @Test
    fun confirm_disabled_for_forbidden() {
        assertFalse(policy(CancellationWarningLevel.FORBIDDEN).isCancellationConfirmEnabled())
    }

    @Test
    fun cannot_open_cancel_screen_when_forbidden() {
        val booking = sampleBooking(policy(CancellationWarningLevel.FORBIDDEN))
        assertFalse(booking.canOpenCancellationScreen())
        assertTrue(booking.isCancellationForbidden())
    }

    @Test
    fun can_open_cancel_screen_when_late_cancellation() {
        val booking = sampleBooking(policy(CancellationWarningLevel.LATE_CANCELLATION))
        assertTrue(booking.canOpenCancellationScreen())
        assertFalse(booking.isCancellationForbidden())
    }

    private fun policy(level: CancellationWarningLevel) = CancellationPolicy(
        canCancel = level != CancellationWarningLevel.FORBIDDEN,
        minutesUntilStart = when (level) {
            CancellationWarningLevel.FORBIDDEN -> 30
            CancellationWarningLevel.LATE_CANCELLATION -> 90
            else -> 180
        },
        warningLevel = level,
    )

    private fun sampleBooking(cancellationPolicy: CancellationPolicy) = Booking(
        id = "b1",
        slotId = "s1",
        bookingStatus = BookingStatus.BOOKED,
        createdAt = Instant.parse("2026-07-10T08:00:00Z"),
        cancelledAt = null,
        usesOwnEquipment = true,
        rebookingForbidden = false,
        slot = TrainingSlot(
            id = "s1",
            startsAt = Instant.parse("2026-07-10T10:00:00Z"),
            durationMinutes = 90,
            capacity = 8,
            freeSpots = 5,
            trainingPrice = 1200.0,
            rentalTariff = null,
            slotStatus = SlotStatus.ACTIVE,
            address = "addr",
            zone = TrainingZone("z", "Зона", FormatType.BOULDERING_INSTRUCTION, Difficulty.BEGINNER, 8),
            instructor = Instructor("i", "Инструктор", null),
            venue = null,
            availability = BookingAvailability(true, true, 5, true, false, false),
        ),
        payment = PaymentInfo("p1", "b1", 1200.0, 0.0, null, 1200.0, PaymentStatus.UNPAID),
        cancellationPolicy = cancellationPolicy,
    )
}
