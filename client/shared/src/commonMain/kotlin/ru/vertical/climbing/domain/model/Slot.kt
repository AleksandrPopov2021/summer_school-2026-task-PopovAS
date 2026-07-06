package ru.vertical.climbing.domain.model

import kotlinx.datetime.Instant

/** Зона тренировки (OpenAPI: TrainingZone). */
data class TrainingZone(
    val id: String,
    val name: String,
    val formatType: FormatType,
    val difficulty: Difficulty,
    val maxGroupSize: Int,
)

/** Инструктор (OpenAPI: Instructor). */
data class Instructor(
    val id: String,
    val fullName: String,
    val averageRating: Double?,
)

/** Скалодром (OpenAPI: GymVenue). */
data class GymVenue(
    val id: String,
    val name: String,
    val address: String,
)

/** Вычисляемые флаги доступности записи (OpenAPI: BookingAvailability, FR-007–FR-009). */
data class BookingAvailability(
    val canBook: Boolean,
    val hasFreeSpots: Boolean,
    val freeSpots: Int?,
    val withinBookingWindow: Boolean,
    val clearanceRequired: Boolean,
    val clearanceGranted: Boolean,
)

/** Позиция прокатного фонда слота (OpenAPI: SlotRentalAvailability). */
data class SlotRentalAvailability(
    val id: String,
    val slotId: String,
    val equipmentTypeId: String,
    val availableQuantity: Int,
    val equipmentType: RentalEquipmentType,
)

/**
 * Слот тренировки (OpenAPI: TrainingSlotSummary / TrainingSlotDetail).
 * [rentalAvailability] заполнен только для деталей слота.
 */
data class TrainingSlot(
    val id: String,
    val startsAt: Instant,
    val durationMinutes: Int,
    val capacity: Int,
    val freeSpots: Int,
    val trainingPrice: Double,
    val rentalTariff: Double?,
    val slotStatus: SlotStatus,
    val address: String,
    val zone: TrainingZone,
    val instructor: Instructor,
    val venue: GymVenue?,
    val availability: BookingAvailability,
    val rentalAvailability: List<SlotRentalAvailability> = emptyList(),
)

/** Ответ поиска альтернативного слота (OpenAPI: AlternativeSlotResponse, BR-020). */
data class AlternativeSlotResult(
    val found: Boolean,
    val alternativeSlot: TrainingSlot?,
)
