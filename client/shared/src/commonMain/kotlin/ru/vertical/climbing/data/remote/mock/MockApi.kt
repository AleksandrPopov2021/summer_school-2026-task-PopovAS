package ru.vertical.climbing.data.remote.mock

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import ru.vertical.climbing.data.remote.dto.AlternativeSlotResponseDto
import ru.vertical.climbing.data.remote.dto.BookingAvailabilityDto
import ru.vertical.climbing.data.remote.dto.BookingDto
import ru.vertical.climbing.data.remote.dto.BookingListDto
import ru.vertical.climbing.data.remote.dto.BookingRentalLineDto
import ru.vertical.climbing.data.remote.dto.CancellationPolicyDto
import ru.vertical.climbing.data.remote.dto.CancellationReasonDto
import ru.vertical.climbing.data.remote.dto.CreateRatingRequestDto
import ru.vertical.climbing.data.remote.dto.InstructorRatingDto
import ru.vertical.climbing.data.remote.dto.ClientDto
import ru.vertical.climbing.data.remote.dto.ClientRegistrationResponseDto
import ru.vertical.climbing.data.remote.dto.ClientUpdateRequestDto
import ru.vertical.climbing.data.remote.dto.ErrorResponseDto
import ru.vertical.climbing.data.remote.dto.GymVenueDto
import ru.vertical.climbing.data.remote.dto.InstructorDto
import ru.vertical.climbing.data.remote.dto.NotificationPreferencesDto
import ru.vertical.climbing.data.remote.dto.NotificationPreferencesUpdateRequestDto
import ru.vertical.climbing.data.remote.dto.PaymentInfoDto
import ru.vertical.climbing.data.remote.dto.RentalEquipmentTypeDto
import ru.vertical.climbing.data.remote.dto.RentalEquipmentTypeListDto
import ru.vertical.climbing.data.remote.dto.SlotListDto
import ru.vertical.climbing.data.remote.dto.SlotRentalAvailabilityDto
import ru.vertical.climbing.data.remote.dto.SlotRentalAvailabilityListDto
import ru.vertical.climbing.data.remote.dto.SystemConfigDto
import ru.vertical.climbing.data.remote.dto.TrainingSlotDto
import ru.vertical.climbing.data.remote.dto.TrainingZoneDto
import ru.vertical.climbing.data.remote.dto.UpdateBookingRentalRequestDto

/**
 * Mock-движок Ktor с фикстурами (LOGIC-003/004, стратегия «до готовности backend»).
 * Отдаёт валидные по OpenAPI ответы для ключевых MVP-эндпоинтов.
 */
object MockApi {

    fun createEngine(json: Json): MockEngine = MockEngine { request ->
        val path = request.url.encodedPath.substringAfter("/v1", request.url.encodedPath)
        val method = request.method

        when {
            path.endsWith("/config") && method == HttpMethod.Get ->
                ok(json, SystemConfigDto.serializer(), Fixtures.config)

            path.endsWith("/rental-equipment-types") && method == HttpMethod.Get ->
                ok(json, RentalEquipmentTypeListDto.serializer(), RentalEquipmentTypeListDto(Fixtures.equipment))

            path.endsWith("/slots") && method == HttpMethod.Get -> {
                val from = request.url.parameters["from"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                val to = request.url.parameters["to"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ok(json, SlotListDto.serializer(), SlotListDto(Fixtures.slotsBetween(from, to)))
            }

            path.endsWith("/rental-availability") && method == HttpMethod.Get -> {
                val slotId = path.removeSuffix("/rental-availability").substringAfterLast('/')
                val slot = Fixtures.slots.firstOrNull { it.id == slotId }
                ok(
                    json,
                    SlotRentalAvailabilityListDto.serializer(),
                    SlotRentalAvailabilityListDto(slotId = slotId, items = slot?.rentalAvailability ?: emptyList()),
                )
            }

            path.endsWith("/slots/alternatives") && method == HttpMethod.Get -> {
                val cancelledSlotId = request.url.parameters["cancelled_slot_id"].orEmpty()
                val bookingId = request.url.parameters["booking_id"]
                val response = Fixtures.findAlternative(cancelledSlotId, bookingId)
                ok(json, AlternativeSlotResponseDto.serializer(), response)
            }

            path.contains("/slots/") && method == HttpMethod.Get -> {
                val slotId = path.substringAfterLast('/')
                val slot = Fixtures.slots.firstOrNull { it.id == slotId }
                if (slot != null) {
                    ok(json, TrainingSlotDto.serializer(), slot)
                } else {
                    notFound(json)
                }
            }

            path.endsWith("/clients") && method == HttpMethod.Post ->
                created(json, ClientRegistrationResponseDto.serializer(), Fixtures.registrationResponse)

            path.endsWith("/clients/me") && method == HttpMethod.Get ->
                ok(json, ClientDto.serializer(), Fixtures.client)

            path.endsWith("/clients/me") && method == HttpMethod.Patch -> {
                val body = request.body.toByteArray().decodeToString()
                val update = runCatching {
                    json.decodeFromString(ClientUpdateRequestDto.serializer(), body)
                }.getOrNull()
                val updated = if (update?.riskConsentAccepted == true) {
                    Fixtures.client.copy(riskConsentAccepted = true)
                } else {
                    Fixtures.client
                }
                Fixtures.client = updated
                ok(json, ClientDto.serializer(), updated)
            }

            path.endsWith("/notification-preferences") && method == HttpMethod.Get ->
                ok(json, NotificationPreferencesDto.serializer(), Fixtures.notificationPreferences)

            path.endsWith("/notification-preferences") && method == HttpMethod.Patch -> {
                val body = request.body.toByteArray().decodeToString()
                val update = runCatching {
                    json.decodeFromString(NotificationPreferencesUpdateRequestDto.serializer(), body)
                }.getOrNull()
                if (update == null) {
                    notFound(json)
                } else {
                    val updated = Fixtures.notificationPreferences.copy(
                        bookingConfirmationEnabled = update.bookingConfirmationEnabled
                            ?: Fixtures.notificationPreferences.bookingConfirmationEnabled,
                        ratingInvitationEnabled = update.ratingInvitationEnabled
                            ?: Fixtures.notificationPreferences.ratingInvitationEnabled,
                    )
                    Fixtures.notificationPreferences = updated
                    ok(json, NotificationPreferencesDto.serializer(), updated)
                }
            }

            path.endsWith("/push-token") && method == HttpMethod.Put ->
                respond("", status = HttpStatusCode.NoContent, headers = jsonHeaders)

            path.endsWith("/bookings") && method == HttpMethod.Post -> {
                val booking = Fixtures.createBookingResponse()
                Fixtures.addBooking(booking)
                created(json, BookingDto.serializer(), booking)
            }

            path.endsWith("/bookings") && method == HttpMethod.Get -> {
                val status = request.url.parameters["status"]
                val items = Fixtures.listBookings(status)
                ok(json, BookingListDto.serializer(), BookingListDto(items))
            }

            path.contains("/bookings/") && method == HttpMethod.Get -> {
                val bookingId = path.substringAfterLast('/')
                val booking = Fixtures.findBooking(bookingId)
                if (booking != null) {
                    ok(json, BookingDto.serializer(), booking)
                } else {
                    notFound(json)
                }
            }

            path.endsWith("/rental") && method == HttpMethod.Patch -> {
                val bookingId = path.removeSuffix("/rental").substringAfterLast('/')
                val body = request.body.toByteArray().decodeToString()
                val update = runCatching {
                    json.decodeFromString(UpdateBookingRentalRequestDto.serializer(), body)
                }.getOrNull()
                if (update == null) {
                    respond(
                        content = ByteReadChannel(
                            json.encodeToString(
                                ErrorResponseDto.serializer(),
                                ErrorResponseDto(code = "BAD_REQUEST", message = "Некорректный запрос"),
                            ),
                        ),
                        status = HttpStatusCode.BadRequest,
                        headers = jsonHeaders,
                    )
                } else {
                    when (val result = Fixtures.updateRental(bookingId, update)) {
                        is RentalUpdateResult.Success -> ok(json, BookingDto.serializer(), result.booking)
                        RentalUpdateResult.NotFound -> notFound(json)
                        RentalUpdateResult.Forbidden -> respond(
                            content = ByteReadChannel(
                                json.encodeToString(
                                    ErrorResponseDto.serializer(),
                                    ErrorResponseDto(code = "FORBIDDEN", message = "Изменение проката недоступно"),
                                ),
                            ),
                            status = HttpStatusCode.Forbidden,
                            headers = jsonHeaders,
                        )
                        RentalUpdateResult.Unavailable -> respond(
                            content = ByteReadChannel(
                                json.encodeToString(
                                    ErrorResponseDto.serializer(),
                                    ErrorResponseDto(
                                        code = "RENTAL_UNAVAILABLE",
                                        message = "Выбранные позиции проката недоступны на данный слот",
                                    ),
                                ),
                            ),
                            status = HttpStatusCode.UnprocessableEntity,
                            headers = jsonHeaders,
                        )
                    }
                }
            }

            path.contains("/bookings/") && method == HttpMethod.Delete -> {
                val bookingId = path.substringAfterLast('/')
                val booking = Fixtures.cancelBooking(bookingId)
                if (booking != null) {
                    ok(json, BookingDto.serializer(), booking)
                } else {
                    notFound(json)
                }
            }

            path.endsWith("/ratings") && method == HttpMethod.Post -> {
                val body = request.body.toByteArray().decodeToString()
                val createRequest = runCatching {
                    json.decodeFromString(CreateRatingRequestDto.serializer(), body)
                }.getOrNull()
                if (createRequest == null) {
                    respond(
                        content = ByteReadChannel(
                            json.encodeToString(
                                ErrorResponseDto.serializer(),
                                ErrorResponseDto(code = "BAD_REQUEST", message = "Некорректный запрос"),
                            ),
                        ),
                        status = HttpStatusCode.BadRequest,
                        headers = jsonHeaders,
                    )
                } else {
                    when (val result = Fixtures.submitRating(createRequest)) {
                        is RatingSubmitResult.Success ->
                            created(json, InstructorRatingDto.serializer(), result.rating)
                        RatingSubmitResult.NotFound -> notFound(json)
                        RatingSubmitResult.GymCancelled -> respond(
                            content = ByteReadChannel(
                                json.encodeToString(
                                    ErrorResponseDto.serializer(),
                                    ErrorResponseDto(
                                        code = "RATING_NOT_ALLOWED_GYM_CANCELLED",
                                        message = "Оценка недоступна — тренировка была отменена скалодромом",
                                    ),
                                ),
                            ),
                            status = HttpStatusCode.Forbidden,
                            headers = jsonHeaders,
                        )
                        RatingSubmitResult.WindowExpired -> respond(
                            content = ByteReadChannel(
                                json.encodeToString(
                                    ErrorResponseDto.serializer(),
                                    ErrorResponseDto(
                                        code = "RATING_WINDOW_EXPIRED",
                                        message = "Срок оценки истёк (1–2 суток после тренировки)",
                                    ),
                                ),
                            ),
                            status = HttpStatusCode.Forbidden,
                            headers = jsonHeaders,
                        )
                        RatingSubmitResult.AlreadySubmitted -> respond(
                            content = ByteReadChannel(
                                json.encodeToString(
                                    ErrorResponseDto.serializer(),
                                    ErrorResponseDto(
                                        code = "RATING_ALREADY_SUBMITTED",
                                        message = "Оценка уже была отправлена",
                                    ),
                                ),
                            ),
                            status = HttpStatusCode.Conflict,
                            headers = jsonHeaders,
                        )
                    }
                }
            }

            else -> notFound(json)
        }
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun io.ktor.client.engine.mock.MockRequestHandleScope.notFound(json: Json) = respond(
        content = ByteReadChannel(
            json.encodeToString(
                ErrorResponseDto.serializer(),
                ErrorResponseDto(code = "NOT_FOUND", message = "Mock: маршрут не найден"),
            ),
        ),
        status = HttpStatusCode.NotFound,
        headers = jsonHeaders,
    )

    private fun <T> io.ktor.client.engine.mock.MockRequestHandleScope.ok(
        json: Json,
        serializer: KSerializer<T>,
        value: T,
    ) = respond(
        content = ByteReadChannel(json.encodeToString(serializer, value)),
        status = HttpStatusCode.OK,
        headers = jsonHeaders,
    )

    private fun <T> io.ktor.client.engine.mock.MockRequestHandleScope.created(
        json: Json,
        serializer: KSerializer<T>,
        value: T,
    ) = respond(
        content = ByteReadChannel(json.encodeToString(serializer, value)),
        status = HttpStatusCode.Created,
        headers = jsonHeaders,
    )
}

/** Фикстуры данных для mock-режима. Слоты генерируются относительно текущей даты. */
private object Fixtures {
    val config = SystemConfigDto(
        reminderHoursBefore = 3,
        visitsForLoyalty = 10,
        violationsForSanctions = 3,
        bookingCutoffMinutes = 30,
        cancellationForbiddenMinutes = 60,
    )

    val equipment = listOf(
        RentalEquipmentTypeDto(id = "eq-shoes", code = "shoes", name = "Скальные туфли", defaultPrice = 300.0),
        RentalEquipmentTypeDto(id = "eq-harness", code = "harness", name = "Страховочная система", defaultPrice = 200.0),
        RentalEquipmentTypeDto(id = "eq-helmet", code = "helmet", name = "Каска", defaultPrice = 150.0),
    )

    private val boulderingZone = TrainingZoneDto(
        id = "zone-1",
        name = "Болдеринг с инструктажем",
        formatType = "bouldering_instruction",
        difficulty = "beginner",
        maxGroupSize = 8,
    )

    private val ropeZone = TrainingZoneDto(
        id = "zone-2",
        name = "Трассы с верёвкой",
        formatType = "rope_routes",
        difficulty = "experienced",
        maxGroupSize = 4,
    )

    private val instructor = InstructorDto(id = "ins-1", fullName = "Петров Алексей Сергеевич", averageRating = 4.8)
    private val venue = GymVenueDto(id = "venue-1", name = "Вертикаль", address = "г. Москва, ул. Скалолазная, д. 1")

    private fun rental(slotId: String) = listOf(
        SlotRentalAvailabilityDto("ra-$slotId-1", slotId, "eq-shoes", 5, equipment[0]),
        SlotRentalAvailabilityDto("ra-$slotId-2", slotId, "eq-harness", 3, equipment[1]),
    )

    private fun available(freeSpots: Int) = BookingAvailabilityDto(
        canBook = true,
        hasFreeSpots = true,
        freeSpots = freeSpots,
        withinBookingWindow = true,
        clearanceRequired = false,
        clearanceGranted = false,
    )

    private val today: LocalDate get() = Clock.System.todayIn(TimeZone.currentSystemDefault())

    /** Дата тренировок относительно «сегодня», чтобы попадать в 7-дневное окно расписания. */
    private fun dayString(offset: Int): String = today.plus(offset, DateTimeUnit.DAY).toString()

    val slots: List<TrainingSlotDto>
        get() = listOf(
            TrainingSlotDto(
                id = "slot-today",
                startsAt = "${dayString(0)}T20:00:00Z",
                durationMinutes = 90,
                capacity = 8,
                freeSpots = 5,
                trainingPrice = 1200.0,
                rentalTariff = 500.0,
                slotStatus = "active",
                address = venue.address,
                zone = boulderingZone,
                instructor = instructor,
                venue = venue,
                availability = available(5),
                rentalAvailability = rental("slot-today"),
            ),
            TrainingSlotDto(
                id = "slot-1",
                startsAt = "${dayString(2)}T10:00:00Z",
                durationMinutes = 90,
                capacity = 8,
                freeSpots = 5,
                trainingPrice = 1200.0,
                rentalTariff = 500.0,
                slotStatus = "active",
                address = venue.address,
                zone = boulderingZone,
                instructor = instructor,
                venue = venue,
                availability = available(5),
                rentalAvailability = rental("slot-1"),
            ),
            TrainingSlotDto(
                id = "slot-2",
                startsAt = "${dayString(2)}T14:00:00Z",
                durationMinutes = 90,
                capacity = 6,
                freeSpots = 0,
                trainingPrice = 1400.0,
                rentalTariff = 500.0,
                slotStatus = "active",
                address = venue.address,
                zone = boulderingZone,
                instructor = instructor,
                venue = venue,
                availability = available(0).copy(canBook = false, hasFreeSpots = false, freeSpots = 0),
            ),
            TrainingSlotDto(
                id = "slot-3",
                startsAt = "${dayString(2)}T17:00:00Z",
                durationMinutes = 120,
                capacity = 4,
                freeSpots = 2,
                trainingPrice = 1600.0,
                rentalTariff = 700.0,
                slotStatus = "active",
                address = venue.address,
                zone = ropeZone,
                instructor = instructor,
                venue = venue,
                availability = available(2).copy(
                    canBook = false,
                    clearanceRequired = true,
                    clearanceGranted = false,
                ),
                rentalAvailability = rental("slot-3"),
            ),
            TrainingSlotDto(
                id = "slot-4",
                startsAt = "${dayString(2)}T19:00:00Z",
                durationMinutes = 90,
                capacity = 8,
                freeSpots = 4,
                trainingPrice = 1200.0,
                rentalTariff = 500.0,
                slotStatus = "cancelled_by_gym",
                address = venue.address,
                zone = boulderingZone,
                instructor = instructor,
                venue = venue,
                availability = available(4).copy(canBook = false),
            ),
            TrainingSlotDto(
                id = "slot-5",
                startsAt = "${dayString(4)}T11:00:00Z",
                durationMinutes = 90,
                capacity = 8,
                freeSpots = 2,
                trainingPrice = 1200.0,
                rentalTariff = 500.0,
                slotStatus = "active",
                address = venue.address,
                zone = boulderingZone,
                instructor = instructor,
                venue = venue,
                availability = available(2),
                rentalAvailability = rental("slot-5"),
            ),
        )

    fun slotsBetween(from: LocalDate?, to: LocalDate?): List<TrainingSlotDto> {
        if (from == null && to == null) return slots
        return slots.filter { slot ->
            val date = LocalDate.parse(slot.startsAt.substring(0, 10))
            (from == null || date >= from) && (to == null || date <= to)
        }
    }

    var client = ClientDto(
        id = "client-1",
        fullName = "Иванов Иван Иванович",
        phone = "+79001234567",
        birthDate = "1995-03-15",
        riskConsentAccepted = false,
        completedVisitsCount = 3,
        isLoyalClient = false,
        loyaltyDiscount = null,
        lateCancellationCount = 0,
        noShowCount = 0,
    )

    var notificationPreferences = NotificationPreferencesDto(
        id = "np-1",
        clientId = "client-1",
        bookingConfirmationEnabled = true,
        ratingInvitationEnabled = true,
        remindersEnabled = true,
        gymCancellationEnabled = true,
    )

    private val bookings = mutableListOf<BookingDto>()

    init {
        bookings += createBookingResponse(warningLevel = "none", id = "booking-mock-1")
        bookings += createBookingResponse(
            warningLevel = "late_cancellation",
            id = "booking-mock-2",
            slotIndex = 4,
            minutesUntilStart = 90,
        )
        bookings += createGymCancelledBooking()
        bookings += createCompletedBooking()
    }

    fun findAlternative(cancelledSlotId: String, bookingId: String?): AlternativeSlotResponseDto {
        val alternative = slots.firstOrNull { it.id == "slot-5" && it.id != cancelledSlotId }
            ?: slots.firstOrNull { it.id != cancelledSlotId && it.slotStatus == "active" && it.availability.canBook }
        return if (alternative != null) {
            AlternativeSlotResponseDto(found = true, alternativeSlot = alternative)
        } else {
            AlternativeSlotResponseDto(found = false, alternativeSlot = null)
        }
    }

    fun createCompletedBooking(
        id: String = "booking-completed-1",
    ): BookingDto {
        val slot = TrainingSlotDto(
            id = "slot-completed-1",
            startsAt = "${dayString(-1)}T10:00:00Z",
            durationMinutes = 90,
            capacity = 8,
            freeSpots = 0,
            trainingPrice = 1200.0,
            rentalTariff = 500.0,
            slotStatus = "active",
            address = venue.address,
            zone = boulderingZone,
            instructor = instructor,
            venue = venue,
            availability = available(0).copy(canBook = false),
            rentalAvailability = rental("slot-completed-1"),
        )
        return BookingDto(
            id = id,
            slotId = slot.id,
            bookingStatus = "completed",
            createdAt = "${dayString(-2)}T09:00:00Z",
            cancelledAt = null,
            usesOwnEquipment = true,
            rebookingForbidden = false,
            slot = slot,
            payment = PaymentInfoDto(
                id = "pay-$id",
                bookingId = id,
                trainingAmount = slot.trainingPrice,
                rentalAmount = 0.0,
                discountAmount = null,
                totalAmount = slot.trainingPrice,
                paymentStatus = "paid",
            ),
            cancellationPolicy = null,
        )
    }

    private val submittedRatings = mutableMapOf<String, InstructorRatingDto>()

    fun submitRating(request: CreateRatingRequestDto): RatingSubmitResult {
        val booking = findBooking(request.bookingId) ?: return RatingSubmitResult.NotFound
        if (booking.bookingStatus == "cancelled_by_gym") return RatingSubmitResult.GymCancelled
        if (booking.bookingStatus != "completed") return RatingSubmitResult.WindowExpired
        if (submittedRatings.containsKey(request.bookingId)) return RatingSubmitResult.AlreadySubmitted

        val rating = InstructorRatingDto(
            id = "rating-${request.bookingId}",
            clientId = client.id,
            instructorId = booking.slot.instructor.id,
            bookingId = request.bookingId,
            stars = request.stars,
            ratedAt = "${dayString(0)}T12:00:00Z",
        )
        submittedRatings[request.bookingId] = rating
        return RatingSubmitResult.Success(rating)
    }

    fun createGymCancelledBooking(
        id: String = "booking-gym-cancel-1",
        slotIndex: Int = 0,
    ): BookingDto {
        val slot = slots[slotIndex.coerceIn(slots.indices)]
        return BookingDto(
            id = id,
            slotId = slot.id,
            bookingStatus = "cancelled_by_gym",
            createdAt = "${dayString(2)}T09:00:00Z",
            cancelledAt = "${dayString(1)}T18:00:00Z",
            usesOwnEquipment = false,
            rebookingForbidden = true,
            slot = slot,
            payment = PaymentInfoDto(
                id = "pay-$id",
                bookingId = id,
                trainingAmount = slot.trainingPrice,
                rentalAmount = 300.0,
                discountAmount = null,
                totalAmount = slot.trainingPrice + 300.0,
                paymentStatus = "refund",
            ),
            cancellationPolicy = null,
            rentalLines = listOf(
                BookingRentalLineDto(
                    id = "brl-$id-1",
                    bookingId = id,
                    equipmentTypeId = "eq-shoes",
                    quantity = 1,
                    unitPrice = 300.0,
                    equipmentType = equipment[0],
                ),
            ),
            cancellationReason = CancellationReasonDto(
                id = "reason-1",
                code = "instructor_unavailable",
                title = "Инструктор недоступен",
                apologyText = "Приносим извинения за неудобства. Мы подобрали для вас альтернативу.",
            ),
        )
    }

    fun addBooking(booking: BookingDto) {
        bookings.removeAll { it.id == booking.id }
        bookings += booking
    }

    fun listBookings(status: String?): List<BookingDto> {
        val filtered = if (status != null) bookings.filter { it.bookingStatus == status } else bookings
        return filtered
    }

    fun findBooking(id: String): BookingDto? = bookings.firstOrNull { it.id == id }

    fun cancelBooking(id: String): BookingDto? {
        val index = bookings.indexOfFirst { it.id == id }
        if (index < 0) return null
        val cancelled = bookings[index].copy(
            bookingStatus = "cancelled_by_client",
            cancelledAt = "${dayString(2)}T12:00:00Z",
        )
        bookings[index] = cancelled
        return cancelled
    }

    fun updateRental(bookingId: String, request: UpdateBookingRentalRequestDto): RentalUpdateResult {
        val index = bookings.indexOfFirst { it.id == bookingId }
        if (index < 0) return RentalUpdateResult.NotFound
        val booking = bookings[index]
        if (booking.bookingStatus != "booked") return RentalUpdateResult.Forbidden

        val slot = booking.slot
        for (line in request.rentalLines) {
            val existingQty = booking.rentalLines
                .find { it.equipmentTypeId == line.equipmentTypeId }
                ?.quantity ?: 0
            val neededExtra = (line.quantity - existingQty).coerceAtLeast(0)
            if (neededExtra > 0) {
                val availability = slot.rentalAvailability
                    .find { it.equipmentTypeId == line.equipmentTypeId }
                    ?.availableQuantity ?: 0
                if (availability < neededExtra) {
                    return RentalUpdateResult.Unavailable
                }
            }
        }

        val rentalLines = request.rentalLines.map { line ->
            val equipmentType = equipment.first { it.id == line.equipmentTypeId }
            BookingRentalLineDto(
                id = "brl-$bookingId-${line.equipmentTypeId}",
                bookingId = bookingId,
                equipmentTypeId = line.equipmentTypeId,
                quantity = line.quantity,
                unitPrice = equipmentType.defaultPrice,
                equipmentType = equipmentType,
            )
        }
        val rentalAmount = rentalLines.sumOf { it.unitPrice * it.quantity }
        val trainingAmount = booking.payment.trainingAmount
        val discount = booking.payment.discountAmount ?: 0.0
        val totalAmount = (trainingAmount + rentalAmount - discount).coerceAtLeast(0.0)

        val updated = booking.copy(
            usesOwnEquipment = request.usesOwnEquipment,
            rentalLines = rentalLines,
            payment = booking.payment.copy(
                rentalAmount = rentalAmount,
                totalAmount = totalAmount,
            ),
        )
        bookings[index] = updated
        return RentalUpdateResult.Success(updated)
    }

    fun createBookingResponse(
        warningLevel: String = "none",
        id: String = "booking-mock-new",
        slotIndex: Int = 0,
        minutesUntilStart: Int = 120,
    ): BookingDto {
        val slot = slots[slotIndex.coerceIn(slots.indices)]
        return BookingDto(
            id = id,
            slotId = slot.id,
            bookingStatus = "booked",
            createdAt = "${dayString(2)}T09:00:00Z",
            cancelledAt = null,
            usesOwnEquipment = true,
            rebookingForbidden = false,
            slot = slot,
            payment = PaymentInfoDto(
                id = "pay-$id",
                bookingId = id,
                trainingAmount = slot.trainingPrice,
                rentalAmount = 0.0,
                discountAmount = null,
                totalAmount = slot.trainingPrice,
                paymentStatus = "unpaid",
            ),
            cancellationPolicy = CancellationPolicyDto(
                canCancel = warningLevel != "forbidden",
                minutesUntilStart = minutesUntilStart,
                warningLevel = warningLevel,
            ),
        )
    }

    val registrationResponse: ClientRegistrationResponseDto
        get() = ClientRegistrationResponseDto(
            accessToken = "mock-jwt-token",
            tokenType = "Bearer",
            client = client,
        )
}

private sealed interface RentalUpdateResult {
    data class Success(val booking: BookingDto) : RentalUpdateResult
    data object NotFound : RentalUpdateResult
    data object Forbidden : RentalUpdateResult
    data object Unavailable : RentalUpdateResult
}

private sealed interface RatingSubmitResult {
    data class Success(val rating: InstructorRatingDto) : RatingSubmitResult
    data object NotFound : RatingSubmitResult
    data object GymCancelled : RatingSubmitResult
    data object WindowExpired : RatingSubmitResult
    data object AlreadySubmitted : RatingSubmitResult
}
