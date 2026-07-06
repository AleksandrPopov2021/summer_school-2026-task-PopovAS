package ru.vertical.climbing.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import ru.vertical.climbing.domain.model.BookingDraft
import ru.vertical.climbing.domain.model.RentalLineInput
import ru.vertical.climbing.domain.usecase.LoadBookingDraftUseCase
import ru.vertical.climbing.domain.usecase.SaveBookingDraftUseCase

class BookingDraftUseCaseTest {

    private val repository = FakeBookingRepository()
    private val saveDraft = SaveBookingDraftUseCase(repository)
    private val loadDraft = LoadBookingDraftUseCase(repository)

    @Test
    fun draft_restore_after_save() = runTest {
        val draft = BookingDraft(
            slotId = "slot-1",
            usesOwnEquipment = true,
            rentalLines = listOf(RentalLineInput("eq-shoes", 1)),
        )
        saveDraft(draft)
        val restored = loadDraft()
        assertNotNull(restored)
        assertEquals(draft, restored)
    }

    @Test
    fun draft_cleared_returns_null() = runTest {
        repository.clearDraft()
        assertNull(loadDraft())
    }
}
