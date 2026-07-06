package ru.vertical.climbing.domain.usecase

import kotlinx.datetime.LocalDate
import ru.vertical.climbing.domain.repository.SlotRepository
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.model.SlotRentalAvailability
import ru.vertical.climbing.domain.model.TrainingSlot

/** LOGIC-003 — Загрузка расписания слотов (по умолчанию 7 дней, BR-027). */
class LoadScheduleUseCase(
    private val slotRepository: SlotRepository,
) {
    suspend operator fun invoke(from: LocalDate? = null, to: LocalDate? = null): AppResult<List<TrainingSlot>> =
        slotRepository.listSlots(from, to)
}

/** SCR-004 — Детали слота. */
class GetSlotDetailUseCase(
    private val slotRepository: SlotRepository,
) {
    suspend operator fun invoke(slotId: String): AppResult<TrainingSlot> =
        slotRepository.getSlot(slotId)
}

/** LOGIC-007 (часть) — доступность прокатного фонда слота. */
class GetSlotRentalAvailabilityUseCase(
    private val slotRepository: SlotRepository,
) {
    suspend operator fun invoke(slotId: String): AppResult<List<SlotRentalAvailability>> =
        slotRepository.getRentalAvailability(slotId)
}
