package ru.vertical.climbing.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.Instant
import ru.vertical.climbing.domain.model.Booking
import ru.vertical.climbing.domain.model.BookingAvailability
import ru.vertical.climbing.domain.model.BookingStatus
import ru.vertical.climbing.domain.model.FormatType
import ru.vertical.climbing.domain.model.Instructor
import ru.vertical.climbing.domain.model.PaymentInfo
import ru.vertical.climbing.domain.model.PaymentStatus
import ru.vertical.climbing.domain.model.RatingAvailability
import ru.vertical.climbing.domain.model.SlotStatus
import ru.vertical.climbing.domain.model.TrainingSlot
import ru.vertical.climbing.domain.model.TrainingZone
import ru.vertical.climbing.domain.model.canPromptRating
import ru.vertical.climbing.domain.model.isRatingWindowExpired
import ru.vertical.climbing.domain.model.resolveRatingAvailability

class RatingLogicTest {

    @Test
    fun completed_booking_within_window_is_available() {
        val booking = sampleBooking(BookingStatus.COMPLETED, slotStartsAt = Instant.parse("2026-07-04T10:00:00Z"))
        val now = Instant.parse("2026-07-05T10:00:00Z")
        assertEquals(
            RatingAvailability.Available,
            resolveRatingAvailability(booking, now = now),
        )
        assertTrue(canPromptRating(booking, now = now))
    }

    @Test
    fun gym_cancelled_booking_is_blocked() {
        val booking = sampleBooking(BookingStatus.CANCELLED_BY_GYM)
        assertEquals(RatingAvailability.GymCancelled, resolveRatingAvailability(booking))
        assertFalse(canPromptRating(booking))
    }

    @Test
    fun expired_window_blocks_rating() {
        val booking = sampleBooking(
            BookingStatus.COMPLETED,
            slotStartsAt = Instant.parse("2026-07-01T10:00:00Z"),
            durationMinutes = 90,
        )
        val now = Instant.parse("2026-07-05T10:00:00Z")
        assertTrue(isRatingWindowExpired(booking.slot, now))
        assertEquals(RatingAvailability.Expired, resolveRatingAvailability(booking, now = now))
    }

    @Test
    fun already_rated_shows_stored_stars() {
        val booking = sampleBooking(BookingStatus.COMPLETED)
        assertEquals(
            RatingAvailability.AlreadyRated(4),
            resolveRatingAvailability(booking, submittedStars = 4),
        )
    }

    @Test
    fun booked_status_is_not_eligible() {
        val booking = sampleBooking(BookingStatus.BOOKED)
        assertEquals(RatingAvailability.NotEligible, resolveRatingAvailability(booking))
    }

    private fun sampleBooking(
        status: BookingStatus,
        slotStartsAt: Instant = Instant.parse("2026-07-04T10:00:00Z"),
        durationMinutes: Int = 90,
    ): Booking {
        val slot = TrainingSlot(
            id = "slot-1",
            startsAt = slotStartsAt,
            durationMinutes = durationMinutes,
            capacity = 8,
            freeSpots = 0,
            trainingPrice = 1200.0,
            rentalTariff = null,
            slotStatus = SlotStatus.ACTIVE,
            address = "Address",
            zone = TrainingZone("z1", "Болдеринг", FormatType.BOULDERING_INSTRUCTION, ru.vertical.climbing.domain.model.Difficulty.BEGINNER, 8),
            instructor = Instructor("ins-1", "Петров А.С.", 4.8),
            venue = null,
            availability = BookingAvailability(false, false, 0, false, false, false),
        )
        return Booking(
            id = "b1",
            slotId = slot.id,
            bookingStatus = status,
            createdAt = slotStartsAt,
            cancelledAt = null,
            usesOwnEquipment = true,
            rebookingForbidden = false,
            slot = slot,
            payment = PaymentInfo("p1", "b1", 1200.0, 0.0, null, 1200.0, PaymentStatus.PAID),
            cancellationPolicy = null,
        )
    }
}
