package ru.vertical.climbing.domain

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import ru.vertical.climbing.domain.model.BookingDraft
import ru.vertical.climbing.domain.model.BookingStatus
import ru.vertical.climbing.domain.model.ClientRegistration
import ru.vertical.climbing.domain.model.CreateBookingCommand
import ru.vertical.climbing.domain.model.RentalLineInput
import ru.vertical.climbing.domain.model.UpdateRentalCommand
import ru.vertical.climbing.domain.usecase.AcceptRiskConsentUseCase
import ru.vertical.climbing.domain.usecase.CancelBookingUseCase
import ru.vertical.climbing.domain.usecase.CreateBookingUseCase
import ru.vertical.climbing.domain.usecase.FindAlternativeSlotUseCase
import ru.vertical.climbing.domain.usecase.GetNotificationPreferencesUseCase
import ru.vertical.climbing.domain.usecase.GetRebookingForbiddenSlotIdsUseCase
import ru.vertical.climbing.domain.usecase.ListMyBookingsUseCase
import ru.vertical.climbing.domain.usecase.LoadBookingDraftUseCase
import ru.vertical.climbing.domain.usecase.LoadProfileUseCase
import ru.vertical.climbing.domain.usecase.ProfileData
import ru.vertical.climbing.domain.usecase.LoadScheduleUseCase
import ru.vertical.climbing.domain.usecase.RegisterClientUseCase
import ru.vertical.climbing.domain.usecase.SaveBookingDraftUseCase
import ru.vertical.climbing.domain.usecase.SignOutUseCase
import ru.vertical.climbing.domain.usecase.UpdateBookingRentalUseCase
import ru.vertical.climbing.domain.usecase.UpdateNotificationPreferencesUseCase
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.ErrorCode
import ru.vertical.climbing.presentation.offlineFallbackOrNull
import ru.vertical.climbing.presentation.offlineListFallbackOrNull

/** Unit-тесты MVP use cases (Итерация 9). */
class MvpUseCasesTest {

    @Test
    fun register_client_saves_token() = runTest {
        val auth = FakeAuthRepository(tokenPresent = false)
        val useCase = RegisterClientUseCase(auth)
        val registration = ClientRegistration(
            phone = "+79001112233",
            fullName = "Тест Тестов",
            birthDate = sampleClient.birthDate,
        )
        val result = useCase(registration)
        assertIs<AppResult.Success<*>>(result)
        assertEquals("token", auth.savedToken)
    }

    @Test
    fun accept_risk_consent_returns_client() = runTest {
        val useCase = AcceptRiskConsentUseCase(FakeAuthRepository())
        val result = useCase()
        assertIs<AppResult.Success<*>>(result)
    }

    @Test
    fun load_schedule_returns_slots() = runTest {
        val useCase = LoadScheduleUseCase(FakeSlotRepository())
        val result = useCase()
        assertIs<AppResult.Success<*>>(result)
        assertTrue((result as AppResult.Success).value.isNotEmpty())
    }

    @Test
    fun list_bookings_returns_active() = runTest {
        val useCase = ListMyBookingsUseCase(FakeBookingRepository())
        val result = useCase()
        assertIs<AppResult.Success<*>>(result)
    }

    @Test
    fun create_booking_clears_draft() = runTest {
        val repo = FakeBookingRepository()
        repo.saveDraft(BookingDraft(slotId = "slot-1", usesOwnEquipment = true, rentalLines = emptyList()))
        val useCase = CreateBookingUseCase(repo)
        val result = useCase(CreateBookingCommand(slotId = "slot-1", usesOwnEquipment = true, rentalLines = emptyList()))
        assertIs<AppResult.Success<*>>(result)
        assertEquals(null, repo.readDraft())
    }

    @Test
    fun save_and_load_booking_draft() = runTest {
        val repo = FakeBookingRepository()
        val draft = BookingDraft(slotId = "slot-1", usesOwnEquipment = false, rentalLines = listOf(RentalLineInput("eq-1", 1)))
        SaveBookingDraftUseCase(repo)(draft)
        val loaded = LoadBookingDraftUseCase(repo)()
        assertEquals(draft, loaded)
    }

    @Test
    fun cancel_booking_success() = runTest {
        val useCase = CancelBookingUseCase(FakeBookingRepository())
        val result = useCase("booking-1")
        assertIs<AppResult.Success<*>>(result)
    }

    @Test
    fun update_booking_rental_success() = runTest {
        val useCase = UpdateBookingRentalUseCase(FakeBookingRepository())
        val result = useCase(
            "booking-1",
            UpdateRentalCommand(usesOwnEquipment = true, rentalLines = emptyList()),
        )
        assertIs<AppResult.Success<*>>(result)
    }

    @Test
    fun find_alternative_slot_found() = runTest {
        val useCase = FindAlternativeSlotUseCase(FakeSlotRepository())
        val result = useCase("slot-cancelled", "booking-1")
        assertIs<AppResult.Success<*>>(result)
    }

    @Test
    fun rebooking_forbidden_collects_slot_ids() = runTest {
        val useCase = GetRebookingForbiddenSlotIdsUseCase(FakeBookingRepository())
        val result = useCase()
        assertIs<AppResult.Success<Set<String>>>(result)
    }

    @Test
    fun load_profile_offline_uses_cache() = runTest {
        val useCase = LoadProfileUseCase(
            authRepository = FakeAuthRepository(currentClientResult = failure(ErrorCode.NETWORK)),
            configRepository = FakeConfigRepository(),
        )
        val result = useCase(cachedClient = sampleClient)
        assertIs<AppResult.Success<ProfileData>>(result)
        assertTrue(result.value.isStale)
    }

    @Test
    fun sign_out_clears_token() = runTest {
        val auth = FakeAuthRepository(tokenPresent = true)
        SignOutUseCase(auth)()
        assertTrue(auth.clearTokenCalled)
    }

    @Test
    fun get_notification_preferences_success() = runTest {
        val useCase = GetNotificationPreferencesUseCase(FakeNotificationPreferencesRepository())
        assertIs<AppResult.Success<*>>(useCase())
    }

    @Test
    fun update_notification_preferences_success() = runTest {
        val useCase = UpdateNotificationPreferencesUseCase(FakeNotificationPreferencesRepository())
        val result = useCase(
            ru.vertical.climbing.domain.model.EditableNotificationToggles(
                bookingConfirmationEnabled = false,
                ratingInvitationEnabled = true,
            ),
        )
        assertIs<AppResult.Success<*>>(result)
    }

    @Test
    fun offline_list_fallback_on_network() {
        val fallback = offlineListFallbackOrNull(ErrorCode.NETWORK, listOf("a"))
        assertEquals(listOf("a"), fallback?.content?.data)
    }

    @Test
    fun offline_fallback_null_without_cache() {
        assertEquals(null, offlineFallbackOrNull(ErrorCode.NETWORK, null))
        assertEquals(null, offlineListFallbackOrNull(ErrorCode.SERVER_ERROR, listOf("a")))
    }

    @Test
    fun error_code_from_api_maps_known_codes() {
        assertEquals(ErrorCode.CLIENT_ALREADY_EXISTS, ErrorCode.fromApiCode("CLIENT_ALREADY_EXISTS"))
        assertEquals(ErrorCode.RENTAL_UNAVAILABLE, ErrorCode.fromApiCode("RENTAL_UNAVAILABLE"))
        assertEquals(ErrorCode.UNKNOWN, ErrorCode.fromApiCode("SOMETHING_NEW"))
    }
}
