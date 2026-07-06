package ru.vertical.climbing.domain

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import ru.vertical.climbing.domain.model.AlternativeSlotResult
import ru.vertical.climbing.domain.model.AuthSession
import ru.vertical.climbing.domain.model.Booking
import ru.vertical.climbing.domain.model.BookingAvailability
import ru.vertical.climbing.domain.model.BookingDraft
import ru.vertical.climbing.domain.model.BookingStatus
import ru.vertical.climbing.domain.model.CancellationPolicy
import ru.vertical.climbing.domain.model.CancellationWarningLevel
import ru.vertical.climbing.domain.model.Client
import ru.vertical.climbing.domain.model.ClientRegistration
import ru.vertical.climbing.domain.model.CreateBookingCommand
import ru.vertical.climbing.domain.model.Difficulty
import ru.vertical.climbing.domain.model.FormatType
import ru.vertical.climbing.domain.model.Instructor
import ru.vertical.climbing.domain.model.NotificationPreferences
import ru.vertical.climbing.domain.model.PaymentInfo
import ru.vertical.climbing.domain.model.PaymentStatus
import ru.vertical.climbing.domain.model.SlotRentalAvailability
import ru.vertical.climbing.domain.model.SlotStatus
import ru.vertical.climbing.domain.model.SystemConfig
import ru.vertical.climbing.domain.model.TrainingSlot
import ru.vertical.climbing.domain.model.TrainingZone
import ru.vertical.climbing.domain.model.UpdateRentalCommand
import ru.vertical.climbing.domain.repository.AuthRepository
import ru.vertical.climbing.domain.repository.BookingRepository
import ru.vertical.climbing.domain.repository.ConfigRepository
import ru.vertical.climbing.domain.repository.NotificationPreferencesRepository
import ru.vertical.climbing.domain.repository.SlotRepository
import ru.vertical.climbing.domain.util.AppError
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.ErrorCode

internal val sampleClient = Client(
    id = "client-1",
    fullName = "Иванов Иван",
    phone = "+79001234567",
    birthDate = LocalDate.parse("1995-03-15"),
    riskConsentAccepted = false,
    completedVisitsCount = 0,
    isLoyalClient = false,
    loyaltyDiscount = null,
    lateCancellationCount = 0,
    noShowCount = 0,
)

internal val sampleConfig = SystemConfig(
    reminderHoursBefore = 3,
    visitsForLoyalty = 10,
    violationsForSanctions = 3,
    bookingCutoffMinutes = 30,
    cancellationForbiddenMinutes = 60,
)

internal class FakeAuthRepository(
    private var tokenPresent: Boolean = false,
    private val currentClientResult: AppResult<Client> = AppResult.Success(sampleClient),
    private val registerResult: AppResult<AuthSession> = AppResult.Success(AuthSession("token", sampleClient)),
) : AuthRepository {

    var clearTokenCalled = false
        private set
    var savedToken: String? = null
        private set

    override suspend fun register(registration: ClientRegistration): AppResult<AuthSession> = registerResult
    override suspend fun getCurrentClient(): AppResult<Client> = currentClientResult
    override suspend fun acceptRiskConsent(): AppResult<Client> = AppResult.Success(sampleClient)

    override suspend fun saveToken(token: String) { savedToken = token }
    override suspend fun readToken(): String? = if (tokenPresent) "token" else null
    override suspend fun clearToken() { clearTokenCalled = true; tokenPresent = false }
    override suspend fun hasToken(): Boolean = tokenPresent
}

internal class FakeConfigRepository(
    private val result: AppResult<SystemConfig> = AppResult.Success(sampleConfig),
    private val cached: SystemConfig? = sampleConfig,
) : ConfigRepository {
    override suspend fun getConfig(forceRefresh: Boolean): AppResult<SystemConfig> = result
    override suspend fun cachedConfig(): SystemConfig? = cached
}

internal fun failure(code: ErrorCode): AppResult<Nothing> = AppResult.Failure(AppError(code))

internal fun sampleSlot(id: String = "slot-1") = TrainingSlot(
    id = id,
    startsAt = Instant.parse("2026-07-10T18:00:00Z"),
    durationMinutes = 90,
    capacity = 8,
    freeSpots = 5,
    trainingPrice = 1200.0,
    rentalTariff = 500.0,
    slotStatus = SlotStatus.ACTIVE,
    address = "addr",
    zone = TrainingZone("z", "Болдеринг", FormatType.BOULDERING_INSTRUCTION, Difficulty.BEGINNER, 8),
    instructor = Instructor("i", "Петров", null),
    venue = null,
    availability = BookingAvailability(
        canBook = true,
        hasFreeSpots = true,
        freeSpots = 5,
        withinBookingWindow = true,
        clearanceRequired = false,
        clearanceGranted = false,
    ),
)

internal fun sampleBooking(id: String = "booking-1", slotId: String = "slot-1") = Booking(
    id = id,
    slotId = slotId,
    bookingStatus = BookingStatus.BOOKED,
    createdAt = Instant.parse("2026-07-01T10:00:00Z"),
    cancelledAt = null,
    usesOwnEquipment = true,
    rebookingForbidden = false,
    slot = sampleSlot(slotId),
    payment = PaymentInfo(
        id = "pay-1",
        bookingId = id,
        trainingAmount = 1200.0,
        rentalAmount = 0.0,
        discountAmount = null,
        totalAmount = 1200.0,
        paymentStatus = PaymentStatus.UNPAID,
    ),
    cancellationPolicy = CancellationPolicy(
        canCancel = true,
        minutesUntilStart = 120,
        warningLevel = CancellationWarningLevel.NONE,
    ),
)

internal class FakeSlotRepository(
    private val slots: List<TrainingSlot> = listOf(sampleSlot()),
) : SlotRepository {
    override suspend fun listSlots(from: LocalDate?, to: LocalDate?) = AppResult.Success(slots)
    override suspend fun getSlot(slotId: String) = AppResult.Success(slots.first { it.id == slotId })
    override suspend fun getRentalAvailability(slotId: String) = AppResult.Success(emptyList<SlotRentalAvailability>())
    override suspend fun findAlternative(cancelledSlotId: String, bookingId: String?) =
        AppResult.Success(AlternativeSlotResult(found = true, alternativeSlot = sampleSlot("slot-alt")))
    override suspend fun cachedSlots() = slots
    override suspend fun updateCachedSlot(slot: TrainingSlot) = Unit
}

internal class FakeBookingRepository(
    private var draft: BookingDraft? = null,
) : BookingRepository {
    override suspend fun listBookings(status: BookingStatus?) =
        AppResult.Success(listOf(sampleBooking()))

    override suspend fun getBooking(bookingId: String) = AppResult.Success(sampleBooking(bookingId))

    override suspend fun createBooking(command: CreateBookingCommand) =
        AppResult.Success(sampleBooking(slotId = command.slotId))

    override suspend fun cancelBooking(bookingId: String) =
        AppResult.Success(sampleBooking(bookingId).copy(bookingStatus = BookingStatus.CANCELLED_BY_CLIENT))

    override suspend fun updateRental(bookingId: String, command: UpdateRentalCommand) =
        AppResult.Success(sampleBooking(bookingId))

    override suspend fun saveDraft(draft: BookingDraft) { this.draft = draft }
    override suspend fun readDraft() = draft
    override suspend fun clearDraft() { draft = null }
    override suspend fun cachedBookings() = listOf(sampleBooking())
}

internal class FakeNotificationPreferencesRepository : NotificationPreferencesRepository {
    private val prefs = NotificationPreferences(
        id = "np-1",
        clientId = "client-1",
        bookingConfirmationEnabled = true,
        ratingInvitationEnabled = true,
        remindersEnabled = true,
        gymCancellationEnabled = true,
    )

    override suspend fun get() = AppResult.Success(prefs)

    override suspend fun update(bookingConfirmationEnabled: Boolean?, ratingInvitationEnabled: Boolean?) =
        AppResult.Success(
            prefs.copy(
                bookingConfirmationEnabled = bookingConfirmationEnabled ?: prefs.bookingConfirmationEnabled,
                ratingInvitationEnabled = ratingInvitationEnabled ?: prefs.ratingInvitationEnabled,
            ),
        )
}
