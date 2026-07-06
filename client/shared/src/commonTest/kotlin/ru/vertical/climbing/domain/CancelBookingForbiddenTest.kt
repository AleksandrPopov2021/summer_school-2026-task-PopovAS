package ru.vertical.climbing.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import ru.vertical.climbing.domain.model.Booking
import ru.vertical.climbing.domain.model.BookingAvailability
import ru.vertical.climbing.domain.model.BookingDraft
import ru.vertical.climbing.domain.model.BookingStatus
import ru.vertical.climbing.domain.model.CancellationPolicy
import ru.vertical.climbing.domain.model.CancellationWarningLevel
import ru.vertical.climbing.domain.model.CreateBookingCommand
import ru.vertical.climbing.domain.model.Difficulty
import ru.vertical.climbing.domain.model.FormatType
import ru.vertical.climbing.domain.model.Instructor
import ru.vertical.climbing.domain.model.PaymentInfo
import ru.vertical.climbing.domain.model.PaymentStatus
import ru.vertical.climbing.domain.model.SlotStatus
import ru.vertical.climbing.domain.model.TrainingSlot
import ru.vertical.climbing.domain.model.TrainingZone
import ru.vertical.climbing.domain.model.UpdateRentalCommand
import ru.vertical.climbing.domain.model.isCancellationConfirmEnabled
import ru.vertical.climbing.domain.repository.BookingRepository
import ru.vertical.climbing.domain.usecase.CancelBookingUseCase
import ru.vertical.climbing.domain.util.AppResult

class CancelBookingForbiddenTest {

    @Test
    fun forbidden_policy_blocks_confirm_without_api_call() = runTest {
        val repository = TrackingBookingRepository()
        val forbidden = cancellationPolicy(CancellationWarningLevel.FORBIDDEN)

        assertFalse(forbidden.isCancellationConfirmEnabled())
        assertFalse(repository.cancelCalled)
    }

    @Test
    fun none_policy_allows_cancel_api() = runTest {
        val repository = TrackingBookingRepository()
        val useCase = CancelBookingUseCase(repository)
        val none = cancellationPolicy(CancellationWarningLevel.NONE)

        assertTrue(none.isCancellationConfirmEnabled())
        useCase("b1")
        assertTrue(repository.cancelCalled)
    }

    private fun cancellationPolicy(level: CancellationWarningLevel) = CancellationPolicy(
        canCancel = level != CancellationWarningLevel.FORBIDDEN,
        minutesUntilStart = if (level == CancellationWarningLevel.FORBIDDEN) 30 else 120,
        warningLevel = level,
    )

    private class TrackingBookingRepository : BookingRepository {
        var cancelCalled = false
            private set

        override suspend fun listBookings(status: BookingStatus?) =
            AppResult.Success(emptyList<Booking>())

        override suspend fun getBooking(bookingId: String) =
            AppResult.Success(sampleBooking())

        override suspend fun createBooking(command: CreateBookingCommand) =
            error("not used")

        override suspend fun cancelBooking(bookingId: String): AppResult<Booking> {
            cancelCalled = true
            return AppResult.Success(sampleBooking())
        }

        override suspend fun updateRental(bookingId: String, command: UpdateRentalCommand) =
            error("not used")

        override suspend fun saveDraft(draft: BookingDraft) = Unit
        override suspend fun readDraft(): BookingDraft? = null
        override suspend fun clearDraft() = Unit
        override suspend fun cachedBookings(): List<Booking> = emptyList()

        private fun sampleBooking() = Booking(
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
            cancellationPolicy = CancellationPolicy(
                canCancel = true,
                minutesUntilStart = 120,
                warningLevel = CancellationWarningLevel.NONE,
            ),
        )
    }
}
