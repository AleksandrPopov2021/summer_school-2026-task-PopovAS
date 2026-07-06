package ru.vertical.climbing.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import ru.vertical.climbing.data.local.LocalCache
import ru.vertical.climbing.data.mapper.toDomain
import ru.vertical.climbing.data.mapper.toDto
import ru.vertical.climbing.data.remote.apiCall
import ru.vertical.climbing.data.remote.dto.AlternativeSlotResponseDto
import ru.vertical.climbing.data.remote.dto.SlotListDto
import ru.vertical.climbing.data.remote.dto.SlotRentalAvailabilityListDto
import ru.vertical.climbing.data.remote.dto.TrainingSlotDto
import ru.vertical.climbing.data.remote.readResult
import ru.vertical.climbing.domain.model.AlternativeSlotResult
import ru.vertical.climbing.domain.model.SlotRentalAvailability
import ru.vertical.climbing.domain.model.TrainingSlot
import ru.vertical.climbing.domain.repository.SlotRepository
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.map
import ru.vertical.climbing.domain.util.onSuccess

class SlotRepositoryImpl(
    private val client: HttpClient,
    private val cache: LocalCache,
    private val json: Json,
) : SlotRepository {

    override suspend fun listSlots(from: LocalDate?, to: LocalDate?): AppResult<List<TrainingSlot>> = apiCall {
        client.get("slots") {
            from?.let { parameter("from", it.toString()) }
            to?.let { parameter("to", it.toString()) }
        }
            .readResult<SlotListDto>(json)
            .onSuccess { cache.saveSlots(it.items) }
            .map { dto -> dto.items.map { it.toDomain() } }
    }

    override suspend fun getSlot(slotId: String): AppResult<TrainingSlot> = apiCall {
        client.get("slots/$slotId")
            .readResult<TrainingSlotDto>(json)
            .map { it.toDomain() }
    }

    override suspend fun getRentalAvailability(slotId: String): AppResult<List<SlotRentalAvailability>> = apiCall {
        client.get("slots/$slotId/rental-availability")
            .readResult<SlotRentalAvailabilityListDto>(json)
            .map { dto -> dto.items.map { it.toDomain() } }
    }

    override suspend fun findAlternative(cancelledSlotId: String, bookingId: String?): AppResult<AlternativeSlotResult> = apiCall {
        client.get("slots/alternatives") {
            parameter("cancelled_slot_id", cancelledSlotId)
            bookingId?.let { parameter("booking_id", it) }
        }
            .readResult<AlternativeSlotResponseDto>(json)
            .map { it.toDomain() }
    }

    override suspend fun cachedSlots(): List<TrainingSlot> = cache.readSlots().map { it.toDomain() }

    override suspend fun updateCachedSlot(slot: TrainingSlot) {
        val updated = cache.readSlots().map { dto ->
            if (dto.id == slot.id) slot.toDto() else dto
        }
        cache.saveSlots(updated)
    }
}
