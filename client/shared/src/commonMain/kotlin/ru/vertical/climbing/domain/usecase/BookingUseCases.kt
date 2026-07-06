package ru.vertical.climbing.domain.usecase

import ru.vertical.climbing.domain.model.Booking
import ru.vertical.climbing.domain.model.BookingStatus
import ru.vertical.climbing.domain.model.BookingDraft
import ru.vertical.climbing.domain.model.CreateBookingCommand
import ru.vertical.climbing.domain.model.RentalEquipmentType
import ru.vertical.climbing.domain.model.RentalLineInput
import ru.vertical.climbing.domain.model.UpdateRentalCommand
import ru.vertical.climbing.domain.model.contributesToRebookingBlock
import ru.vertical.climbing.domain.repository.BookingRepository
import ru.vertical.climbing.domain.repository.ReferenceRepository
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.map
import ru.vertical.climbing.domain.util.onSuccess

/** SCR-006 — Список записей клиента. */
class ListMyBookingsUseCase(
    private val bookingRepository: BookingRepository,
) {
    suspend operator fun invoke(status: BookingStatus? = BookingStatus.BOOKED): AppResult<List<Booking>> =
        bookingRepository.listBookings(status)
}

/** SCR-007 — Детали записи. */
class GetBookingDetailUseCase(
    private val bookingRepository: BookingRepository,
) {
    suspend operator fun invoke(bookingId: String): AppResult<Booking> =
        bookingRepository.getBooking(bookingId)
}

/** LOGIC-005 — Создание записи. */
class CreateBookingUseCase(
    private val bookingRepository: BookingRepository,
) {
    suspend operator fun invoke(command: CreateBookingCommand): AppResult<Booking> =
        bookingRepository.createBooking(command).onSuccess {
            bookingRepository.clearDraft()
        }
}

/** LOGIC-007 — Справочник типов прокатного снаряжения (FR-010). */
class ListRentalEquipmentTypesUseCase(
    private val referenceRepository: ReferenceRepository,
) {
    suspend operator fun invoke(): AppResult<List<RentalEquipmentType>> =
        referenceRepository.listRentalEquipmentTypes()
}

/** Сохранение черновика записи (booking_draft) до подтверждения. */
class SaveBookingDraftUseCase(
    private val bookingRepository: BookingRepository,
) {
    suspend operator fun invoke(draft: BookingDraft) = bookingRepository.saveDraft(draft)
}

/** Восстановление черновика записи после kill app (AC-E01). */
class LoadBookingDraftUseCase(
    private val bookingRepository: BookingRepository,
) {
    suspend operator fun invoke(): BookingDraft? = bookingRepository.readDraft()
}

/** LOGIC-008 — Отмена записи. */
class CancelBookingUseCase(
    private val bookingRepository: BookingRepository,
) {
    suspend operator fun invoke(bookingId: String): AppResult<Booking> =
        bookingRepository.cancelBooking(bookingId)
}

/** LOGIC-009 — Поиск альтернативного слота (FR-022, BR-020). */
class FindAlternativeSlotUseCase(
    private val slotRepository: ru.vertical.climbing.domain.repository.SlotRepository,
) {
    suspend operator fun invoke(
        cancelledSlotId: String,
        bookingId: String?,
    ): AppResult<ru.vertical.climbing.domain.model.AlternativeSlotResult> =
        slotRepository.findAlternative(cancelledSlotId, bookingId)
}

/** LOGIC-009 — перенос проката из исходной записи в черновик (SCR-005). */
class PrefillBookingDraftFromBookingUseCase(
    private val bookingRepository: BookingRepository,
) {
    suspend operator fun invoke(slotId: String, source: Booking) {
        bookingRepository.saveDraft(
            BookingDraft(
                slotId = slotId,
                usesOwnEquipment = source.usesOwnEquipment,
                rentalLines = source.rentalLines.map { RentalLineInput(it.equipmentTypeId, it.quantity) },
            ),
        )
    }
}

/** BR-018 — слоты, на которые запрещена повторная запись после отмены скалодромом. */
class GetRebookingForbiddenSlotIdsUseCase(
    private val bookingRepository: BookingRepository,
) {
    suspend operator fun invoke(): AppResult<Set<String>> =
        bookingRepository.listBookings(status = null).map { bookings ->
            bookings
                .filter { it.contributesToRebookingBlock() }
                .map { it.slotId }
                .toSet()
        }
}

/** LOGIC-010 — Изменение проката в записи. */
class UpdateBookingRentalUseCase(
    private val bookingRepository: BookingRepository,
) {
    suspend operator fun invoke(bookingId: String, command: UpdateRentalCommand): AppResult<Booking> =
        bookingRepository.updateRental(bookingId, command)
}

/**
 * LOGIC-007 — Расчёт стоимости записи (тренировка + прокат − скидка).
 * Чистая доменная логика без сети; используется на SCR-005 для предпросмотра «итого».
 */
class CalculateBookingPriceUseCase {
    data class PriceBreakdown(
        val trainingAmount: Double,
        val rentalAmount: Double,
        val discountAmount: Double,
        val total: Double,
    )

    operator fun invoke(
        trainingPrice: Double,
        rentalLines: List<RentalLineInput>,
        equipmentTypes: List<RentalEquipmentType>,
        loyaltyDiscount: Double?,
    ): PriceBreakdown {
        val priceById = equipmentTypes.associateBy({ it.id }, { it.defaultPrice })
        val rentalAmount = rentalLines.sumOf { line ->
            (priceById[line.equipmentTypeId] ?: 0.0) * line.quantity
        }
        val discount = (loyaltyDiscount ?: 0.0).coerceAtLeast(0.0)
        val subtotal = trainingPrice + rentalAmount
        val total = (subtotal - discount).coerceAtLeast(0.0)
        return PriceBreakdown(
            trainingAmount = trainingPrice,
            rentalAmount = rentalAmount,
            discountAmount = discount,
            total = total,
        )
    }
}
