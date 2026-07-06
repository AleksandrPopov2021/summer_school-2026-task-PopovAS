package ru.vertical.climbing.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.Instant
import ru.vertical.climbing.domain.model.AlternativeSlotResult
import ru.vertical.climbing.domain.model.BookingAvailability
import ru.vertical.climbing.domain.model.Difficulty
import ru.vertical.climbing.domain.model.FormatType
import ru.vertical.climbing.domain.model.Instructor
import ru.vertical.climbing.domain.model.SlotStatus
import ru.vertical.climbing.domain.model.TrainingSlot
import ru.vertical.climbing.domain.model.TrainingZone
import ru.vertical.climbing.domain.model.canBookAlternativeSlot

class AlternativeSlotLogicTest {

    @Test
    fun found_alternative_is_bookable_when_available() {
        val slot = sampleSlot(id = "slot-alt", canBook = true, freeSpots = 5)
        assertTrue(
            canBookAlternativeSlot(
                alternative = slot,
                cancelledSlotId = "slot-cancelled",
                rebookingForbidden = true,
            ),
        )
    }

    @Test
    fun rebooking_forbidden_hides_book_for_same_slot() {
        val slot = sampleSlot(id = "slot-1", canBook = true, freeSpots = 5)
        assertFalse(
            canBookAlternativeSlot(
                alternative = slot,
                cancelledSlotId = "slot-1",
                rebookingForbidden = true,
            ),
        )
    }

    @Test
    fun not_found_when_api_returns_false() {
        val result = AlternativeSlotResult(found = false, alternativeSlot = null)
        assertFalse(result.found)
        assertEquals(null, result.alternativeSlot)
    }

    @Test
    fun found_when_api_returns_slot() {
        val slot = sampleSlot(id = "slot-5", canBook = true, freeSpots = 2)
        val result = AlternativeSlotResult(found = true, alternativeSlot = slot)
        assertTrue(result.found)
        assertEquals("slot-5", result.alternativeSlot?.id)
    }

    private fun sampleSlot(id: String, canBook: Boolean, freeSpots: Int) = TrainingSlot(
        id = id,
        startsAt = Instant.parse("2026-07-10T18:00:00Z"),
        durationMinutes = 90,
        capacity = 8,
        freeSpots = freeSpots,
        trainingPrice = 1200.0,
        rentalTariff = 500.0,
        slotStatus = SlotStatus.ACTIVE,
        address = "addr",
        zone = TrainingZone("z", "Болдеринг", FormatType.BOULDERING_INSTRUCTION, Difficulty.BEGINNER, 8),
        instructor = Instructor("i", "Петров", null),
        venue = null,
        availability = BookingAvailability(
            canBook = canBook,
            hasFreeSpots = freeSpots > 0,
            freeSpots = freeSpots,
            withinBookingWindow = true,
            clearanceRequired = false,
            clearanceGranted = false,
        ),
    )
}
