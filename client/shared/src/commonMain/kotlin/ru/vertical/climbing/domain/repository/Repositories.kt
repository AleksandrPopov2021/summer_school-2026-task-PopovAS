package ru.vertical.climbing.domain.repository

import kotlinx.datetime.LocalDate
import ru.vertical.climbing.domain.model.AuthSession
import ru.vertical.climbing.domain.model.Booking
import ru.vertical.climbing.domain.model.BookingDraft
import ru.vertical.climbing.domain.model.BookingStatus
import ru.vertical.climbing.domain.model.Client
import ru.vertical.climbing.domain.model.ClientRegistration
import ru.vertical.climbing.domain.model.CreateBookingCommand
import ru.vertical.climbing.domain.model.CreateRatingCommand
import ru.vertical.climbing.domain.model.InstructorClearance
import ru.vertical.climbing.domain.model.InstructorRating
import ru.vertical.climbing.domain.model.NotificationPreferences
import ru.vertical.climbing.domain.model.PushTokenRegistration
import ru.vertical.climbing.domain.model.RentalEquipmentType
import ru.vertical.climbing.domain.model.SlotRentalAvailability
import ru.vertical.climbing.domain.model.SystemConfig
import ru.vertical.climbing.domain.model.TrainingSlot
import ru.vertical.climbing.domain.model.UpdateRentalCommand
import ru.vertical.climbing.domain.util.AppResult

/** Авторизация и профиль (LOGIC-001, LOGIC-002). */
interface AuthRepository {
    suspend fun register(registration: ClientRegistration): AppResult<AuthSession>
    suspend fun getCurrentClient(): AppResult<Client>
    suspend fun acceptRiskConsent(): AppResult<Client>

    suspend fun saveToken(token: String)
    suspend fun readToken(): String?
    suspend fun clearToken()
    suspend fun hasToken(): Boolean
}

/** Системные параметры + кэш (LOGIC-001, cached_config). */
interface ConfigRepository {
    suspend fun getConfig(forceRefresh: Boolean = false): AppResult<SystemConfig>
    suspend fun cachedConfig(): SystemConfig?
}

/** Расписание и слоты (LOGIC-003, LOGIC-004). */
interface SlotRepository {
    suspend fun listSlots(from: LocalDate?, to: LocalDate?): AppResult<List<TrainingSlot>>
    suspend fun getSlot(slotId: String): AppResult<TrainingSlot>
    suspend fun getRentalAvailability(slotId: String): AppResult<List<SlotRentalAvailability>>
    suspend fun findAlternative(cancelledSlotId: String, bookingId: String?): AppResult<ru.vertical.climbing.domain.model.AlternativeSlotResult>
    suspend fun cachedSlots(): List<TrainingSlot>
    suspend fun updateCachedSlot(slot: TrainingSlot)
}

/** Записи клиента (LOGIC-005, LOGIC-008, LOGIC-010). */
interface BookingRepository {
    suspend fun listBookings(status: BookingStatus?): AppResult<List<Booking>>
    suspend fun getBooking(bookingId: String): AppResult<Booking>
    suspend fun createBooking(command: CreateBookingCommand): AppResult<Booking>
    suspend fun cancelBooking(bookingId: String): AppResult<Booking>
    suspend fun updateRental(bookingId: String, command: UpdateRentalCommand): AppResult<Booking>

    suspend fun saveDraft(draft: BookingDraft)
    suspend fun readDraft(): BookingDraft?
    suspend fun clearDraft()

    /** Кэш записей для offline (NFR-001). */
    suspend fun cachedBookings(): List<Booking>
}

/** Справочники (FR-010). */
interface ReferenceRepository {
    suspend fun listRentalEquipmentTypes(): AppResult<List<RentalEquipmentType>>
}

/** Настройки уведомлений (LOGIC-011, FR-032). */
interface NotificationPreferencesRepository {
    suspend fun get(): AppResult<NotificationPreferences>
    suspend fun update(
        bookingConfirmationEnabled: Boolean?,
        ratingInvitationEnabled: Boolean?,
    ): AppResult<NotificationPreferences>
}

/** Допуски инструктора (FR-009, BR-007). */
interface ClearanceRepository {
    suspend fun getClearances(): AppResult<List<InstructorClearance>>
}

/** Регистрация устройства для push (LOGIC-012). */
interface DeviceRepository {
    suspend fun registerPushToken(registration: PushTokenRegistration): AppResult<Unit>
}

/** Оценка инструктора (Post-MVP, LOGIC-014). */
interface RatingRepository {
    suspend fun createRating(command: CreateRatingCommand): AppResult<InstructorRating>
    fun submittedStarsFor(bookingId: String): Int?
    fun markSubmitted(bookingId: String, stars: Int)
}
