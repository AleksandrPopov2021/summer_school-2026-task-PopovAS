package ru.vertical.climbing.domain

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ru.vertical.climbing.domain.model.BookingAvailability
import ru.vertical.climbing.domain.model.Difficulty
import ru.vertical.climbing.domain.model.FormatType
import ru.vertical.climbing.domain.model.Instructor
import ru.vertical.climbing.domain.model.SlotAvailabilityState
import ru.vertical.climbing.domain.model.SlotStatus
import ru.vertical.climbing.domain.model.TrainingSlot
import ru.vertical.climbing.domain.model.TrainingZone
import ru.vertical.climbing.domain.model.availabilityState
import ru.vertical.climbing.domain.model.hidesBookButton
import ru.vertical.climbing.domain.model.isBookable

class SlotAvailabilityTest {

    @Test
    fun available_when_can_book_and_enough_spots() {
        val slot = slot(freeSpots = 5, canBook = true)
        assertEquals(SlotAvailabilityState.AVAILABLE, slot.availabilityState())
        assertTrue(slot.availabilityState().isBookable)
        assertFalse(slot.availabilityState().hidesBookButton)
    }

    @Test
    fun few_spots_when_one_or_two_left() {
        assertEquals(SlotAvailabilityState.FEW_SPOTS, slot(freeSpots = 2, canBook = true).availabilityState())
        assertEquals(SlotAvailabilityState.FEW_SPOTS, slot(freeSpots = 1, canBook = true).availabilityState())
    }

    @Test
    fun no_spots_hides_book_button() {
        val slot = slot(freeSpots = 0, hasFreeSpots = false, canBook = false)
        val state = slot.availabilityState()
        assertEquals(SlotAvailabilityState.NO_SPOTS, state)
        assertTrue(state.hidesBookButton)
        assertFalse(state.isBookable)
    }

    @Test
    fun cancelled_by_gym_hides_book_button() {
        val slot = slot(freeSpots = 4, canBook = false, status = SlotStatus.CANCELLED_BY_GYM)
        val state = slot.availabilityState()
        assertEquals(SlotAvailabilityState.CANCELLED, state)
        assertTrue(state.hidesBookButton)
    }

    @Test
    fun clearance_required_disables_booking() {
        val slot = slot(freeSpots = 2, canBook = false, clearanceRequired = true, clearanceGranted = false)
        val state = slot.availabilityState()
        assertEquals(SlotAvailabilityState.CLEARANCE_REQUIRED, state)
        assertFalse(state.isBookable)
        assertFalse(state.hidesBookButton)
    }

    @Test
    fun booking_closed_when_outside_window() {
        val slot = slot(freeSpots = 5, canBook = false, withinWindow = false)
        val state = slot.availabilityState()
        assertEquals(SlotAvailabilityState.BOOKING_CLOSED, state)
        assertFalse(state.isBookable)
    }

    private fun slot(
        freeSpots: Int,
        canBook: Boolean,
        hasFreeSpots: Boolean = freeSpots > 0,
        withinWindow: Boolean = true,
        clearanceRequired: Boolean = false,
        clearanceGranted: Boolean = false,
        status: SlotStatus = SlotStatus.ACTIVE,
    ) = TrainingSlot(
        id = "slot",
        startsAt = Instant.parse("2026-07-10T10:00:00Z"),
        durationMinutes = 90,
        capacity = 8,
        freeSpots = freeSpots,
        trainingPrice = 1200.0,
        rentalTariff = 500.0,
        slotStatus = status,
        address = "addr",
        zone = TrainingZone("z", "Зона", FormatType.BOULDERING_INSTRUCTION, Difficulty.BEGINNER, 8),
        instructor = Instructor("i", "Инструктор", null),
        venue = null,
        availability = BookingAvailability(
            canBook = canBook,
            hasFreeSpots = hasFreeSpots,
            freeSpots = freeSpots,
            withinBookingWindow = withinWindow,
            clearanceRequired = clearanceRequired,
            clearanceGranted = clearanceGranted,
        ),
    )
}
