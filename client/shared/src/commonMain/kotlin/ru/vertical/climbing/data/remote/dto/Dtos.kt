package ru.vertical.climbing.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponseDto(
    val code: String,
    val message: String,
)

/** Ответ 409 при конфликте записи (OpenAPI: BookingConflictResponse, FR-014). */
@Serializable
data class BookingConflictResponseDto(
    val code: String,
    val message: String,
    val slot: TrainingSlotDto,
)

// --- Clients ---

@Serializable
data class ClientRegistrationRequestDto(
    val phone: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("birth_date") val birthDate: String,
)

@Serializable
data class ClientRegistrationResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    val client: ClientDto,
)

@Serializable
data class ClientUpdateRequestDto(
    @SerialName("risk_consent_accepted") val riskConsentAccepted: Boolean? = null,
)

@Serializable
data class ClientDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val phone: String,
    @SerialName("birth_date") val birthDate: String,
    @SerialName("risk_consent_accepted") val riskConsentAccepted: Boolean,
    @SerialName("completed_visits_count") val completedVisitsCount: Int,
    @SerialName("is_loyal_client") val isLoyalClient: Boolean,
    @SerialName("loyalty_discount") val loyaltyDiscount: Double? = null,
    @SerialName("late_cancellation_count") val lateCancellationCount: Int,
    @SerialName("no_show_count") val noShowCount: Int,
)

@Serializable
data class InstructorClearanceDto(
    val id: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("instructor_id") val instructorId: String? = null,
    @SerialName("is_granted") val isGranted: Boolean,
    @SerialName("granted_at") val grantedAt: String? = null,
)

@Serializable
data class ClearanceListDto(val items: List<InstructorClearanceDto>)

@Serializable
data class NotificationPreferencesDto(
    val id: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("booking_confirmation_enabled") val bookingConfirmationEnabled: Boolean,
    @SerialName("rating_invitation_enabled") val ratingInvitationEnabled: Boolean,
    @SerialName("reminders_enabled") val remindersEnabled: Boolean = true,
    @SerialName("gym_cancellation_enabled") val gymCancellationEnabled: Boolean = true,
)

@Serializable
data class NotificationPreferencesUpdateRequestDto(
    @SerialName("booking_confirmation_enabled") val bookingConfirmationEnabled: Boolean? = null,
    @SerialName("rating_invitation_enabled") val ratingInvitationEnabled: Boolean? = null,
)

// --- Slots ---

@Serializable
data class TrainingZoneDto(
    val id: String,
    val name: String,
    @SerialName("format_type") val formatType: String,
    val difficulty: String,
    @SerialName("max_group_size") val maxGroupSize: Int,
)

@Serializable
data class InstructorDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("average_rating") val averageRating: Double? = null,
)

@Serializable
data class GymVenueDto(
    val id: String,
    val name: String,
    val address: String,
)

@Serializable
data class BookingAvailabilityDto(
    @SerialName("can_book") val canBook: Boolean,
    @SerialName("has_free_spots") val hasFreeSpots: Boolean,
    @SerialName("free_spots") val freeSpots: Int? = null,
    @SerialName("within_booking_window") val withinBookingWindow: Boolean,
    @SerialName("clearance_required") val clearanceRequired: Boolean,
    @SerialName("clearance_granted") val clearanceGranted: Boolean,
)

@Serializable
data class SlotRentalAvailabilityDto(
    val id: String,
    @SerialName("slot_id") val slotId: String,
    @SerialName("equipment_type_id") val equipmentTypeId: String,
    @SerialName("available_quantity") val availableQuantity: Int,
    @SerialName("equipment_type") val equipmentType: RentalEquipmentTypeDto,
)

@Serializable
data class TrainingSlotDto(
    val id: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    val capacity: Int,
    @SerialName("free_spots") val freeSpots: Int,
    @SerialName("training_price") val trainingPrice: Double,
    @SerialName("rental_tariff") val rentalTariff: Double? = null,
    @SerialName("slot_status") val slotStatus: String,
    val address: String,
    val zone: TrainingZoneDto,
    val instructor: InstructorDto,
    val venue: GymVenueDto? = null,
    val availability: BookingAvailabilityDto,
    @SerialName("rental_availability") val rentalAvailability: List<SlotRentalAvailabilityDto> = emptyList(),
)

@Serializable
data class SlotListDto(val items: List<TrainingSlotDto>)

@Serializable
data class SlotRentalAvailabilityListDto(
    @SerialName("slot_id") val slotId: String,
    val items: List<SlotRentalAvailabilityDto>,
)

@Serializable
data class AlternativeSlotResponseDto(
    val found: Boolean,
    @SerialName("alternative_slot") val alternativeSlot: TrainingSlotDto? = null,
)

// --- Reference ---

@Serializable
data class RentalEquipmentTypeDto(
    val id: String,
    val code: String,
    val name: String,
    @SerialName("default_price") val defaultPrice: Double,
)

@Serializable
data class RentalEquipmentTypeListDto(val items: List<RentalEquipmentTypeDto>)

@Serializable
data class SystemConfigDto(
    @SerialName("reminder_hours_before") val reminderHoursBefore: Int,
    @SerialName("visits_for_loyalty") val visitsForLoyalty: Int,
    @SerialName("violations_for_sanctions") val violationsForSanctions: Int,
    @SerialName("booking_cutoff_minutes") val bookingCutoffMinutes: Int,
    @SerialName("cancellation_forbidden_minutes") val cancellationForbiddenMinutes: Int,
)

// --- Bookings ---

@Serializable
data class BookingRentalLineInputDto(
    @SerialName("equipment_type_id") val equipmentTypeId: String,
    val quantity: Int = 1,
)

@Serializable
data class BookingRentalLineDto(
    val id: String,
    @SerialName("booking_id") val bookingId: String,
    @SerialName("equipment_type_id") val equipmentTypeId: String,
    val quantity: Int,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("equipment_type") val equipmentType: RentalEquipmentTypeDto,
)

@Serializable
data class PaymentInfoDto(
    val id: String,
    @SerialName("booking_id") val bookingId: String,
    @SerialName("training_amount") val trainingAmount: Double,
    @SerialName("rental_amount") val rentalAmount: Double,
    @SerialName("discount_amount") val discountAmount: Double? = null,
    @SerialName("total_amount") val totalAmount: Double,
    @SerialName("payment_status") val paymentStatus: String,
)

@Serializable
data class CancellationReasonDto(
    val id: String,
    val code: String,
    val title: String,
    @SerialName("apology_text") val apologyText: String,
)

@Serializable
data class CancellationPolicyDto(
    @SerialName("can_cancel") val canCancel: Boolean,
    @SerialName("minutes_until_start") val minutesUntilStart: Int,
    @SerialName("warning_level") val warningLevel: String,
)

@Serializable
data class CreateBookingRequestDto(
    @SerialName("slot_id") val slotId: String,
    @SerialName("uses_own_equipment") val usesOwnEquipment: Boolean,
    @SerialName("rental_lines") val rentalLines: List<BookingRentalLineInputDto> = emptyList(),
)

@Serializable
data class UpdateBookingRentalRequestDto(
    @SerialName("uses_own_equipment") val usesOwnEquipment: Boolean,
    @SerialName("rental_lines") val rentalLines: List<BookingRentalLineInputDto> = emptyList(),
)

/** Локальный черновик записи (booking_draft). */
@Serializable
data class BookingDraftDto(
    @SerialName("slot_id") val slotId: String,
    @SerialName("uses_own_equipment") val usesOwnEquipment: Boolean,
    @SerialName("rental_lines") val rentalLines: List<BookingRentalLineInputDto> = emptyList(),
)

@Serializable
data class BookingDto(
    val id: String,
    @SerialName("slot_id") val slotId: String,
    @SerialName("booking_status") val bookingStatus: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("cancelled_at") val cancelledAt: String? = null,
    @SerialName("uses_own_equipment") val usesOwnEquipment: Boolean,
    @SerialName("rebooking_forbidden") val rebookingForbidden: Boolean,
    val slot: TrainingSlotDto,
    val payment: PaymentInfoDto,
    @SerialName("cancellation_policy") val cancellationPolicy: CancellationPolicyDto? = null,
    @SerialName("rental_lines") val rentalLines: List<BookingRentalLineDto> = emptyList(),
    @SerialName("cancellation_reason") val cancellationReason: CancellationReasonDto? = null,
)

@Serializable
data class BookingListDto(val items: List<BookingDto>)

// --- Devices ---

@Serializable
data class PushTokenRequestDto(
    val token: String,
    val platform: String,
)

// --- Ratings (Post-MVP) ---

@Serializable
data class CreateRatingRequestDto(
    @SerialName("booking_id") val bookingId: String,
    val stars: Int,
)

@Serializable
data class InstructorRatingDto(
    val id: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("instructor_id") val instructorId: String,
    @SerialName("booking_id") val bookingId: String,
    val stars: Int,
    @SerialName("rated_at") val ratedAt: String,
)
