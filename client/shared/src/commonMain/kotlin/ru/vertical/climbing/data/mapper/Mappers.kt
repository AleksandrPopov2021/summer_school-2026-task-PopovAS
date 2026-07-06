package ru.vertical.climbing.data.mapper

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import ru.vertical.climbing.data.remote.dto.BookingDraftDto
import ru.vertical.climbing.data.remote.dto.BookingDto
import ru.vertical.climbing.data.remote.dto.BookingRentalLineDto
import ru.vertical.climbing.data.remote.dto.BookingRentalLineInputDto
import ru.vertical.climbing.data.remote.dto.CancellationPolicyDto
import ru.vertical.climbing.data.remote.dto.CancellationReasonDto
import ru.vertical.climbing.data.remote.dto.ClientDto
import ru.vertical.climbing.data.remote.dto.ClientRegistrationRequestDto
import ru.vertical.climbing.data.remote.dto.CreateBookingRequestDto
import ru.vertical.climbing.data.remote.dto.CreateRatingRequestDto
import ru.vertical.climbing.data.remote.dto.InstructorClearanceDto
import ru.vertical.climbing.data.remote.dto.InstructorDto
import ru.vertical.climbing.data.remote.dto.InstructorRatingDto
import ru.vertical.climbing.data.remote.dto.NotificationPreferencesDto
import ru.vertical.climbing.data.remote.dto.PaymentInfoDto
import ru.vertical.climbing.data.remote.dto.PushTokenRequestDto
import ru.vertical.climbing.data.remote.dto.RentalEquipmentTypeDto
import ru.vertical.climbing.data.remote.dto.SlotRentalAvailabilityDto
import ru.vertical.climbing.data.remote.dto.SystemConfigDto
import ru.vertical.climbing.data.remote.dto.TrainingSlotDto
import ru.vertical.climbing.data.remote.dto.TrainingZoneDto
import ru.vertical.climbing.data.remote.dto.UpdateBookingRentalRequestDto
import ru.vertical.climbing.domain.model.AlternativeSlotResult
import ru.vertical.climbing.domain.model.Booking
import ru.vertical.climbing.domain.model.BookingAvailability
import ru.vertical.climbing.domain.model.BookingDraft
import ru.vertical.climbing.domain.model.BookingRentalLine
import ru.vertical.climbing.domain.model.BookingStatus
import ru.vertical.climbing.domain.model.CancellationPolicy
import ru.vertical.climbing.domain.model.CancellationReason
import ru.vertical.climbing.domain.model.CancellationWarningLevel
import ru.vertical.climbing.domain.model.Client
import ru.vertical.climbing.domain.model.ClientRegistration
import ru.vertical.climbing.domain.model.CreateBookingCommand
import ru.vertical.climbing.domain.model.CreateRatingCommand
import ru.vertical.climbing.domain.model.DevicePlatform
import ru.vertical.climbing.domain.model.Difficulty
import ru.vertical.climbing.domain.model.FormatType
import ru.vertical.climbing.domain.model.GymVenue
import ru.vertical.climbing.domain.model.Instructor
import ru.vertical.climbing.domain.model.InstructorClearance
import ru.vertical.climbing.domain.model.InstructorRating
import ru.vertical.climbing.domain.model.NotificationPreferences
import ru.vertical.climbing.domain.model.PaymentInfo
import ru.vertical.climbing.domain.model.PaymentStatus
import ru.vertical.climbing.domain.model.PushTokenRegistration
import ru.vertical.climbing.domain.model.RentalEquipmentCode
import ru.vertical.climbing.domain.model.RentalEquipmentType
import ru.vertical.climbing.domain.model.RentalLineInput
import ru.vertical.climbing.domain.model.SlotRentalAvailability
import ru.vertical.climbing.domain.model.SlotStatus
import ru.vertical.climbing.domain.model.SystemConfig
import ru.vertical.climbing.domain.model.TrainingSlot
import ru.vertical.climbing.domain.model.TrainingZone
import ru.vertical.climbing.domain.model.UpdateRentalCommand
import ru.vertical.climbing.data.remote.dto.AlternativeSlotResponseDto
import ru.vertical.climbing.data.remote.dto.BookingAvailabilityDto
import ru.vertical.climbing.data.remote.dto.GymVenueDto

// --- Enums ---

fun String.toSlotStatus(): SlotStatus = when (this) {
    "active" -> SlotStatus.ACTIVE
    "cancelled_by_gym" -> SlotStatus.CANCELLED_BY_GYM
    else -> SlotStatus.UNKNOWN
}

fun String.toFormatType(): FormatType = when (this) {
    "bouldering_instruction" -> FormatType.BOULDERING_INSTRUCTION
    "rope_routes" -> FormatType.ROPE_ROUTES
    else -> FormatType.UNKNOWN
}

fun String.toDifficulty(): Difficulty = when (this) {
    "beginner" -> Difficulty.BEGINNER
    "experienced" -> Difficulty.EXPERIENCED
    else -> Difficulty.UNKNOWN
}

fun String.toBookingStatus(): BookingStatus = when (this) {
    "booked" -> BookingStatus.BOOKED
    "cancelled_by_client" -> BookingStatus.CANCELLED_BY_CLIENT
    "cancelled_by_gym" -> BookingStatus.CANCELLED_BY_GYM
    "completed" -> BookingStatus.COMPLETED
    "no_show" -> BookingStatus.NO_SHOW
    else -> BookingStatus.UNKNOWN
}

fun BookingStatus.toApiValue(): String = when (this) {
    BookingStatus.BOOKED -> "booked"
    BookingStatus.CANCELLED_BY_CLIENT -> "cancelled_by_client"
    BookingStatus.CANCELLED_BY_GYM -> "cancelled_by_gym"
    BookingStatus.COMPLETED -> "completed"
    BookingStatus.NO_SHOW -> "no_show"
    BookingStatus.UNKNOWN -> "booked"
}

fun String.toPaymentStatus(): PaymentStatus = when (this) {
    "unpaid" -> PaymentStatus.UNPAID
    "paid" -> PaymentStatus.PAID
    "refund" -> PaymentStatus.REFUND
    else -> PaymentStatus.UNKNOWN
}

fun String.toRentalCode(): RentalEquipmentCode = when (this) {
    "shoes" -> RentalEquipmentCode.SHOES
    "harness" -> RentalEquipmentCode.HARNESS
    "helmet" -> RentalEquipmentCode.HELMET
    "chalk" -> RentalEquipmentCode.CHALK
    else -> RentalEquipmentCode.UNKNOWN
}

fun String.toWarningLevel(): CancellationWarningLevel = when (this) {
    "none" -> CancellationWarningLevel.NONE
    "late_cancellation" -> CancellationWarningLevel.LATE_CANCELLATION
    "forbidden" -> CancellationWarningLevel.FORBIDDEN
    else -> CancellationWarningLevel.UNKNOWN
}

fun DevicePlatform.toApiValue(): String = when (this) {
    DevicePlatform.ANDROID -> "android"
    DevicePlatform.IOS -> "ios"
}

// --- Client ---

fun ClientDto.toDomain(): Client = Client(
    id = id,
    fullName = fullName,
    phone = phone,
    birthDate = LocalDate.parse(birthDate),
    riskConsentAccepted = riskConsentAccepted,
    completedVisitsCount = completedVisitsCount,
    isLoyalClient = isLoyalClient,
    loyaltyDiscount = loyaltyDiscount,
    lateCancellationCount = lateCancellationCount,
    noShowCount = noShowCount,
)

fun ClientRegistration.toDto(): ClientRegistrationRequestDto = ClientRegistrationRequestDto(
    phone = phone,
    fullName = fullName,
    birthDate = birthDate.toString(),
)

fun InstructorClearanceDto.toDomain(): InstructorClearance = InstructorClearance(
    id = id,
    clientId = clientId,
    instructorId = instructorId,
    isGranted = isGranted,
)

fun NotificationPreferencesDto.toDomain(): NotificationPreferences = NotificationPreferences(
    id = id,
    clientId = clientId,
    bookingConfirmationEnabled = bookingConfirmationEnabled,
    ratingInvitationEnabled = ratingInvitationEnabled,
    remindersEnabled = remindersEnabled,
    gymCancellationEnabled = gymCancellationEnabled,
)

// --- Slots ---

fun TrainingZoneDto.toDomain(): TrainingZone = TrainingZone(
    id = id,
    name = name,
    formatType = formatType.toFormatType(),
    difficulty = difficulty.toDifficulty(),
    maxGroupSize = maxGroupSize,
)

fun InstructorDto.toDomain(): Instructor = Instructor(
    id = id,
    fullName = fullName,
    averageRating = averageRating,
)

fun GymVenueDto.toDomain(): GymVenue = GymVenue(id = id, name = name, address = address)

fun SlotRentalAvailabilityDto.toDomain(): SlotRentalAvailability = SlotRentalAvailability(
    id = id,
    slotId = slotId,
    equipmentTypeId = equipmentTypeId,
    availableQuantity = availableQuantity,
    equipmentType = equipmentType.toDomain(),
)

fun TrainingSlotDto.toDomain(): TrainingSlot = TrainingSlot(
    id = id,
    startsAt = Instant.parse(startsAt),
    durationMinutes = durationMinutes,
    capacity = capacity,
    freeSpots = freeSpots,
    trainingPrice = trainingPrice,
    rentalTariff = rentalTariff,
    slotStatus = slotStatus.toSlotStatus(),
    address = address,
    zone = zone.toDomain(),
    instructor = instructor.toDomain(),
    venue = venue?.toDomain(),
    availability = availability.let {
        BookingAvailability(
            canBook = it.canBook,
            hasFreeSpots = it.hasFreeSpots,
            freeSpots = it.freeSpots,
            withinBookingWindow = it.withinBookingWindow,
            clearanceRequired = it.clearanceRequired,
            clearanceGranted = it.clearanceGranted,
        )
    },
    rentalAvailability = rentalAvailability.map { it.toDomain() },
)

fun SlotStatus.toApiValue(): String = when (this) {
    SlotStatus.ACTIVE -> "active"
    SlotStatus.CANCELLED_BY_GYM -> "cancelled_by_gym"
    SlotStatus.UNKNOWN -> "active"
}

fun FormatType.toApiValue(): String = when (this) {
    FormatType.BOULDERING_INSTRUCTION -> "bouldering_instruction"
    FormatType.ROPE_ROUTES -> "rope_routes"
    FormatType.UNKNOWN -> "bouldering_instruction"
}

fun Difficulty.toApiValue(): String = when (this) {
    Difficulty.BEGINNER -> "beginner"
    Difficulty.EXPERIENCED -> "experienced"
    Difficulty.UNKNOWN -> "beginner"
}

fun RentalEquipmentCode.toApiValue(): String = when (this) {
    RentalEquipmentCode.SHOES -> "shoes"
    RentalEquipmentCode.HARNESS -> "harness"
    RentalEquipmentCode.HELMET -> "helmet"
    RentalEquipmentCode.CHALK -> "chalk"
    RentalEquipmentCode.UNKNOWN -> "shoes"
}

fun TrainingZone.toDto(): TrainingZoneDto = TrainingZoneDto(
    id = id,
    name = name,
    formatType = formatType.toApiValue(),
    difficulty = difficulty.toApiValue(),
    maxGroupSize = maxGroupSize,
)

fun Instructor.toDto(): InstructorDto = InstructorDto(
    id = id,
    fullName = fullName,
    averageRating = averageRating,
)

fun GymVenue.toDto(): GymVenueDto = GymVenueDto(id = id, name = name, address = address)

fun RentalEquipmentType.toDto(): RentalEquipmentTypeDto = RentalEquipmentTypeDto(
    id = id,
    code = code.toApiValue(),
    name = name,
    defaultPrice = defaultPrice,
)

fun SlotRentalAvailability.toDto(): SlotRentalAvailabilityDto = SlotRentalAvailabilityDto(
    id = id,
    slotId = slotId,
    equipmentTypeId = equipmentTypeId,
    availableQuantity = availableQuantity,
    equipmentType = equipmentType.toDto(),
)

fun BookingAvailability.toDto(): BookingAvailabilityDto = BookingAvailabilityDto(
    canBook = canBook,
    hasFreeSpots = hasFreeSpots,
    freeSpots = freeSpots,
    withinBookingWindow = withinBookingWindow,
    clearanceRequired = clearanceRequired,
    clearanceGranted = clearanceGranted,
)

fun TrainingSlot.toDto(): TrainingSlotDto = TrainingSlotDto(
    id = id,
    startsAt = startsAt.toString(),
    durationMinutes = durationMinutes,
    capacity = capacity,
    freeSpots = freeSpots,
    trainingPrice = trainingPrice,
    rentalTariff = rentalTariff,
    slotStatus = slotStatus.toApiValue(),
    address = address,
    zone = zone.toDto(),
    instructor = instructor.toDto(),
    venue = venue?.toDto(),
    availability = availability.toDto(),
    rentalAvailability = rentalAvailability.map { it.toDto() },
)

fun AlternativeSlotResponseDto.toDomain(): AlternativeSlotResult = AlternativeSlotResult(
    found = found,
    alternativeSlot = alternativeSlot?.toDomain(),
)

// --- Reference ---

fun RentalEquipmentTypeDto.toDomain(): RentalEquipmentType = RentalEquipmentType(
    id = id,
    code = code.toRentalCode(),
    name = name,
    defaultPrice = defaultPrice,
)

fun SystemConfigDto.toDomain(): SystemConfig = SystemConfig(
    reminderHoursBefore = reminderHoursBefore,
    visitsForLoyalty = visitsForLoyalty,
    violationsForSanctions = violationsForSanctions,
    bookingCutoffMinutes = bookingCutoffMinutes,
    cancellationForbiddenMinutes = cancellationForbiddenMinutes,
)

// --- Bookings ---

fun BookingRentalLineDto.toDomain(): BookingRentalLine = BookingRentalLine(
    id = id,
    bookingId = bookingId,
    equipmentTypeId = equipmentTypeId,
    quantity = quantity,
    unitPrice = unitPrice,
    equipmentType = equipmentType.toDomain(),
)

fun PaymentInfoDto.toDomain(): PaymentInfo = PaymentInfo(
    id = id,
    bookingId = bookingId,
    trainingAmount = trainingAmount,
    rentalAmount = rentalAmount,
    discountAmount = discountAmount,
    totalAmount = totalAmount,
    paymentStatus = paymentStatus.toPaymentStatus(),
)

fun CancellationReasonDto.toDomain(): CancellationReason = CancellationReason(
    id = id,
    code = code,
    title = title,
    apologyText = apologyText,
)

fun CancellationPolicyDto.toDomain(): CancellationPolicy = CancellationPolicy(
    canCancel = canCancel,
    minutesUntilStart = minutesUntilStart,
    warningLevel = warningLevel.toWarningLevel(),
)

fun BookingDto.toDomain(): Booking = Booking(
    id = id,
    slotId = slotId,
    bookingStatus = bookingStatus.toBookingStatus(),
    createdAt = Instant.parse(createdAt),
    cancelledAt = cancelledAt?.let { Instant.parse(it) },
    usesOwnEquipment = usesOwnEquipment,
    rebookingForbidden = rebookingForbidden,
    slot = slot.toDomain(),
    payment = payment.toDomain(),
    cancellationPolicy = cancellationPolicy?.toDomain(),
    rentalLines = rentalLines.map { it.toDomain() },
    cancellationReason = cancellationReason?.toDomain(),
)

fun RentalLineInput.toDto(): BookingRentalLineInputDto = BookingRentalLineInputDto(
    equipmentTypeId = equipmentTypeId,
    quantity = quantity,
)

fun CreateBookingCommand.toDto(): CreateBookingRequestDto = CreateBookingRequestDto(
    slotId = slotId,
    usesOwnEquipment = usesOwnEquipment,
    rentalLines = rentalLines.map { it.toDto() },
)

fun UpdateRentalCommand.toDto(): UpdateBookingRentalRequestDto = UpdateBookingRentalRequestDto(
    usesOwnEquipment = usesOwnEquipment,
    rentalLines = rentalLines.map { it.toDto() },
)

fun BookingDraft.toDto(): BookingDraftDto = BookingDraftDto(
    slotId = slotId,
    usesOwnEquipment = usesOwnEquipment,
    rentalLines = rentalLines.map { it.toDto() },
)

fun BookingDraftDto.toDomain(): BookingDraft = BookingDraft(
    slotId = slotId,
    usesOwnEquipment = usesOwnEquipment,
    rentalLines = rentalLines.map { RentalLineInput(it.equipmentTypeId, it.quantity) },
)

// --- Devices / Ratings ---

fun PushTokenRegistration.toDto(): PushTokenRequestDto = PushTokenRequestDto(
    token = token,
    platform = platform.toApiValue(),
)

fun CreateRatingCommand.toDto(): CreateRatingRequestDto = CreateRatingRequestDto(
    bookingId = bookingId,
    stars = stars,
)

fun InstructorRatingDto.toDomain(): InstructorRating = InstructorRating(
    id = id,
    clientId = clientId,
    instructorId = instructorId,
    bookingId = bookingId,
    stars = stars,
    ratedAt = Instant.parse(ratedAt),
)
