package ru.vertical.climbing.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import ru.vertical.climbing.data.local.LocalCache
import ru.vertical.climbing.data.mapper.toApiValue
import ru.vertical.climbing.data.mapper.toDomain
import ru.vertical.climbing.data.mapper.toDto
import ru.vertical.climbing.data.remote.apiCall
import ru.vertical.climbing.data.remote.dto.BookingConflictResponseDto
import ru.vertical.climbing.data.remote.dto.BookingDto
import ru.vertical.climbing.data.remote.dto.BookingListDto
import ru.vertical.climbing.data.remote.readResult
import ru.vertical.climbing.domain.model.Booking
import ru.vertical.climbing.domain.model.BookingDraft
import ru.vertical.climbing.domain.model.BookingStatus
import ru.vertical.climbing.domain.model.CreateBookingCommand
import ru.vertical.climbing.domain.model.UpdateRentalCommand
import ru.vertical.climbing.domain.repository.BookingRepository
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.ErrorCode
import ru.vertical.climbing.domain.util.map
import ru.vertical.climbing.domain.util.onSuccess

class BookingRepositoryImpl(
    private val client: HttpClient,
    private val cache: LocalCache,
    private val json: Json,
) : BookingRepository {

    override suspend fun listBookings(status: BookingStatus?): AppResult<List<Booking>> = apiCall {
        client.get("bookings") {
            status?.let { parameter("status", it.toApiValue()) }
        }
            .readResult<BookingListDto>(json)
            .onSuccess { cache.saveBookings(it.items) }
            .map { dto -> dto.items.map { it.toDomain() } }
    }

    override suspend fun getBooking(bookingId: String): AppResult<Booking> = apiCall {
        client.get("bookings/$bookingId")
            .readResult<BookingDto>(json)
            .map { it.toDomain() }
    }

    override suspend fun createBooking(command: CreateBookingCommand): AppResult<Booking> = apiCall {
        val response = client.post("bookings") { setBody(command.toDto()) }
        when (response.status.value) {
            409 -> {
                val raw = response.bodyAsText()
                val conflict = runCatching {
                    json.decodeFromString(BookingConflictResponseDto.serializer(), raw)
                }.getOrNull()
                if (conflict != null) {
                    AppResult.Failure(
                        ru.vertical.climbing.domain.util.AppError(
                            code = ErrorCode.BOOKING_CONFLICT,
                            message = conflict.message,
                            httpStatus = 409,
                            conflictSlot = conflict.slot.toDomain(),
                        ),
                    )
                } else {
                    response.readResult<BookingDto>(json).map { it.toDomain() }
                }
            }
            else -> response.readResult<BookingDto>(json).map { it.toDomain() }
        }
    }

    override suspend fun cancelBooking(bookingId: String): AppResult<Booking> = apiCall {
        client.delete("bookings/$bookingId")
            .readResult<BookingDto>(json)
            .map { it.toDomain() }
    }

    override suspend fun updateRental(bookingId: String, command: UpdateRentalCommand): AppResult<Booking> = apiCall {
        client.patch("bookings/$bookingId/rental") { setBody(command.toDto()) }
            .readResult<BookingDto>(json)
            .map { it.toDomain() }
    }

    override suspend fun saveDraft(draft: BookingDraft) = cache.saveDraft(draft.toDto())
    override suspend fun readDraft(): BookingDraft? = cache.readDraft()?.toDomain()
    override suspend fun clearDraft() = cache.clearDraft()

    override suspend fun cachedBookings(): List<Booking> = cache.readBookings().map { it.toDomain() }
}
