package ru.vertical.climbing.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.Instant
import ru.vertical.climbing.domain.model.Booking
import ru.vertical.climbing.domain.model.BookingAvailability
import ru.vertical.climbing.domain.model.BookingStatus
import ru.vertical.climbing.domain.model.Difficulty
import ru.vertical.climbing.domain.model.FormatType
import ru.vertical.climbing.domain.model.Instructor
import ru.vertical.climbing.domain.model.PaymentInfo
import ru.vertical.climbing.domain.model.PaymentStatus
import ru.vertical.climbing.domain.model.SlotStatus
import ru.vertical.climbing.domain.model.TrainingSlot
import ru.vertical.climbing.domain.model.TrainingZone
import ru.vertical.climbing.domain.model.contributesToRebookingBlock
import ru.vertical.climbing.domain.model.isRebookingBlocked

class RebookingForbiddenTest {

    @Test
    fun gym_cancel_with_rebooking_forbidden_blocks_slot() {
        val booking = gymCancelledBooking(rebookingForbidden = true, slotId = "slot-1")
        assertTrue(booking.contributesToRebookingBlock())
        assertTrue(sampleSlot("slot-1").isRebookingBlocked(setOf("slot-1")))
    }

    @Test
    fun other_slots_are_not_blocked() {
        assertFalse(sampleSlot("slot-2").isRebookingBlocked(setOf("slot-1")))
    }

    @Test
    fun rebooking_allowed_does_not_contribute_to_block() {
        val booking = gymCancelledBooking(rebookingForbidden = false, slotId = "slot-1")
        assertFalse(booking.contributesToRebookingBlock())
    }

    private fun gymCancelledBooking(rebookingForbidden: Boolean, slotId: String) = Booking(
        id = "b1",
        slotId = slotId,
        bookingStatus = BookingStatus.CANCELLED_BY_GYM,
        createdAt = Instant.parse("2026-07-10T08:00:00Z"),
        cancelledAt = Instant.parse("2026-07-10T09:00:00Z"),
        usesOwnEquipment = false,
        rebookingForbidden = rebookingForbidden,
        slot = sampleSlot(slotId),
        payment = PaymentInfo("p1", "b1", 1200.0, 0.0, null, 1200.0, PaymentStatus.REFUND),
        cancellationPolicy = null,
    )

    private fun sampleSlot(id: String) = TrainingSlot(
        id = id,
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
    )
}
