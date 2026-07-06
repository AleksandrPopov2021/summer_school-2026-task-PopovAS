package ru.vertical.climbing.data.local

import com.russhwolf.settings.Settings
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import ru.vertical.climbing.data.remote.dto.BookingDraftDto
import ru.vertical.climbing.data.remote.dto.BookingListDto
import ru.vertical.climbing.data.remote.dto.SlotListDto
import ru.vertical.climbing.data.remote.dto.SystemConfigDto
import ru.vertical.climbing.data.remote.dto.TrainingSlotDto

/**
 * Кэш нечувствительных данных (cached_config, cached_slots, booking_draft).
 * Хранит DTO как JSON-строки в [Settings].
 */
class LocalCache(
    private val settings: Settings,
    private val json: Json,
) {
    private val ratingsSerializer = MapSerializer(String.serializer(), Int.serializer())

    fun saveConfig(dto: SystemConfigDto) {
        settings.putString(StorageKeys.CACHED_CONFIG, json.encodeToString(SystemConfigDto.serializer(), dto))
    }

    fun readConfig(): SystemConfigDto? = settings.getStringOrNull(StorageKeys.CACHED_CONFIG)
        ?.let { runCatching { json.decodeFromString(SystemConfigDto.serializer(), it) }.getOrNull() }

    fun saveSlots(slots: List<TrainingSlotDto>) {
        settings.putString(StorageKeys.CACHED_SLOTS, json.encodeToString(SlotListDto.serializer(), SlotListDto(slots)))
    }

    fun readSlots(): List<TrainingSlotDto> = settings.getStringOrNull(StorageKeys.CACHED_SLOTS)
        ?.let { runCatching { json.decodeFromString(SlotListDto.serializer(), it).items }.getOrNull() }
        ?: emptyList()

    fun saveDraft(dto: BookingDraftDto) {
        settings.putString(StorageKeys.BOOKING_DRAFT, json.encodeToString(BookingDraftDto.serializer(), dto))
    }

    fun readDraft(): BookingDraftDto? = settings.getStringOrNull(StorageKeys.BOOKING_DRAFT)
        ?.let { runCatching { json.decodeFromString(BookingDraftDto.serializer(), it) }.getOrNull() }

    fun clearDraft() {
        settings.remove(StorageKeys.BOOKING_DRAFT)
    }

    fun saveBookings(items: List<ru.vertical.climbing.data.remote.dto.BookingDto>) {
        settings.putString(
            StorageKeys.CACHED_BOOKINGS,
            json.encodeToString(BookingListDto.serializer(), BookingListDto(items)),
        )
    }

    fun readBookings(): List<ru.vertical.climbing.data.remote.dto.BookingDto> =
        settings.getStringOrNull(StorageKeys.CACHED_BOOKINGS)
            ?.let { runCatching { json.decodeFromString(BookingListDto.serializer(), it).items }.getOrNull() }
            ?: emptyList()

    fun readSubmittedRatings(): Map<String, Int> =
        settings.getStringOrNull(StorageKeys.SUBMITTED_RATINGS)
            ?.let { runCatching { json.decodeFromString(ratingsSerializer, it) }.getOrNull() }
            ?: emptyMap()

    fun markRatingSubmitted(bookingId: String, stars: Int) {
        val updated: Map<String, Int> = readSubmittedRatings() + (bookingId to stars)
        settings.putString(StorageKeys.SUBMITTED_RATINGS, json.encodeToString(ratingsSerializer, updated))
    }

    fun submittedStarsFor(bookingId: String): Int? = readSubmittedRatings()[bookingId]
}
