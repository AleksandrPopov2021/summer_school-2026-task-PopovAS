package ru.vertical.climbing.domain.model

import kotlinx.datetime.Instant

/** Строка проката в записи (OpenAPI: BookingRentalLine). */
data class BookingRentalLine(
    val id: String,
    val bookingId: String,
    val equipmentTypeId: String,
    val quantity: Int,
    val unitPrice: Double,
    val equipmentType: RentalEquipmentType,
)

/** Информация об оплате (OpenAPI: PaymentInfo, FR-011, FR-023). */
data class PaymentInfo(
    val id: String,
    val bookingId: String,
    val trainingAmount: Double,
    val rentalAmount: Double,
    val discountAmount: Double?,
    val totalAmount: Double,
    val paymentStatus: PaymentStatus,
)

/** Причина отмены скалодромом (OpenAPI: CancellationReason, FR-021). */
data class CancellationReason(
    val id: String,
    val code: String,
    val title: String,
    val apologyText: String,
)

/** Политика отмены для UI (OpenAPI: CancellationPolicy, FR-017–FR-019). */
data class CancellationPolicy(
    val canCancel: Boolean,
    val minutesUntilStart: Int,
    val warningLevel: CancellationWarningLevel,
)

/**
 * Запись на тренировку (OpenAPI: BookingSummary / BookingDetail).
 * [rentalLines] и [cancellationReason] заполнены только для деталей записи.
 */
data class Booking(
    val id: String,
    val slotId: String,
    val bookingStatus: BookingStatus,
    val createdAt: Instant,
    val cancelledAt: Instant?,
    val usesOwnEquipment: Boolean,
    val rebookingForbidden: Boolean,
    val slot: TrainingSlot,
    val payment: PaymentInfo,
    val cancellationPolicy: CancellationPolicy?,
    val rentalLines: List<BookingRentalLine> = emptyList(),
    val cancellationReason: CancellationReason? = null,
)

/** Ввод позиции проката при создании/изменении записи (OpenAPI: BookingRentalLineInput). */
data class RentalLineInput(
    val equipmentTypeId: String,
    val quantity: Int,
)

/** Запрос на создание записи (OpenAPI: CreateBookingRequest, UC-002). */
data class CreateBookingCommand(
    val slotId: String,
    val usesOwnEquipment: Boolean,
    val rentalLines: List<RentalLineInput>,
)

/** Запрос на изменение проката (OpenAPI: UpdateBookingRentalRequest, UC-008). */
data class UpdateRentalCommand(
    val usesOwnEquipment: Boolean,
    val rentalLines: List<RentalLineInput>,
)

/** Черновик записи, хранится локально до подтверждения (booking_draft). */
data class BookingDraft(
    val slotId: String,
    val usesOwnEquipment: Boolean,
    val rentalLines: List<RentalLineInput>,
)
