package ru.vertical.climbing.domain.model

/**
 * UI-состояние доступности слота (LOGIC-004, SCR-003 / SCR-004).
 * Итоговое состояние определяется приоритетно: отмена → нет мест → допуск →
 * закрытая запись → мало мест → доступно.
 */
enum class SlotAvailabilityState {
    /** Запись открыта, мест достаточно (≥ 3). */
    AVAILABLE,

    /** Осталось 1–2 места (BR-009, оранжевый индикатор). */
    FEW_SPOTS,

    /** Мест нет — кнопка записи скрыта (FR-007, BR-008). */
    NO_SPOTS,

    /** До начала меньше `booking_cutoff_minutes` — запись закрыта (FR-008, BR-006). */
    BOOKING_CLOSED,

    /** Требуется допуск инструктора для «трасс с верёвкой» (FR-009, BR-007). */
    CLEARANCE_REQUIRED,

    /** Слот отменён скалодромом — карточка помечается, кнопка скрыта (FR-006, BR-019). */
    CANCELLED,
}

/** Активна ли кнопка «Записаться» (доступно / мало мест). */
val SlotAvailabilityState.isBookable: Boolean
    get() = this == SlotAvailabilityState.AVAILABLE || this == SlotAvailabilityState.FEW_SPOTS

/** Скрыть кнопку записи полностью (нет мест / отменён, FR-006, FR-007). */
val SlotAvailabilityState.hidesBookButton: Boolean
    get() = this == SlotAvailabilityState.NO_SPOTS || this == SlotAvailabilityState.CANCELLED

/**
 * LOGIC-004 — вычисление UI-состояния слота на основе статуса и флагов доступности.
 * Порядок условий задаёт приоритет отображения.
 */
fun TrainingSlot.availabilityState(): SlotAvailabilityState = when {
    slotStatus == SlotStatus.CANCELLED_BY_GYM -> SlotAvailabilityState.CANCELLED
    freeSpots <= 0 || !availability.hasFreeSpots -> SlotAvailabilityState.NO_SPOTS
    availability.clearanceRequired && !availability.clearanceGranted -> SlotAvailabilityState.CLEARANCE_REQUIRED
    !availability.withinBookingWindow -> SlotAvailabilityState.BOOKING_CLOSED
    freeSpots <= FEW_SPOTS_THRESHOLD -> SlotAvailabilityState.FEW_SPOTS
    else -> SlotAvailabilityState.AVAILABLE
}

private const val FEW_SPOTS_THRESHOLD = 2
