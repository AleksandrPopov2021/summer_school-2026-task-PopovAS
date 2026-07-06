package ru.vertical.climbing.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlinx.serialization.json.Json
import ru.vertical.climbing.data.mapper.toDomain
import ru.vertical.climbing.data.mapper.toDto
import ru.vertical.climbing.data.remote.apiCall
import ru.vertical.climbing.data.remote.dto.ClearanceListDto
import ru.vertical.climbing.data.remote.dto.InstructorRatingDto
import ru.vertical.climbing.data.remote.dto.NotificationPreferencesDto
import ru.vertical.climbing.data.remote.dto.NotificationPreferencesUpdateRequestDto
import ru.vertical.climbing.data.remote.dto.RentalEquipmentTypeListDto
import ru.vertical.climbing.data.remote.readResult
import ru.vertical.climbing.data.remote.readUnit
import ru.vertical.climbing.domain.model.CreateRatingCommand
import ru.vertical.climbing.domain.model.InstructorClearance
import ru.vertical.climbing.domain.model.InstructorRating
import ru.vertical.climbing.domain.model.NotificationPreferences
import ru.vertical.climbing.domain.model.PushTokenRegistration
import ru.vertical.climbing.domain.model.RentalEquipmentType
import ru.vertical.climbing.domain.repository.ClearanceRepository
import ru.vertical.climbing.domain.repository.DeviceRepository
import ru.vertical.climbing.domain.repository.NotificationPreferencesRepository
import ru.vertical.climbing.domain.repository.RatingRepository
import ru.vertical.climbing.domain.repository.ReferenceRepository
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.map

class ReferenceRepositoryImpl(
    private val client: HttpClient,
    private val json: Json,
) : ReferenceRepository {
    override suspend fun listRentalEquipmentTypes(): AppResult<List<RentalEquipmentType>> = apiCall {
        client.get("rental-equipment-types")
            .readResult<RentalEquipmentTypeListDto>(json)
            .map { dto -> dto.items.map { it.toDomain() } }
    }
}

class NotificationPreferencesRepositoryImpl(
    private val client: HttpClient,
    private val json: Json,
) : NotificationPreferencesRepository {
    override suspend fun get(): AppResult<NotificationPreferences> = apiCall {
        client.get("clients/me/notification-preferences")
            .readResult<NotificationPreferencesDto>(json)
            .map { it.toDomain() }
    }

    override suspend fun update(
        bookingConfirmationEnabled: Boolean?,
        ratingInvitationEnabled: Boolean?,
    ): AppResult<NotificationPreferences> = apiCall {
        client.patch("clients/me/notification-preferences") {
            setBody(
                NotificationPreferencesUpdateRequestDto(
                    bookingConfirmationEnabled = bookingConfirmationEnabled,
                    ratingInvitationEnabled = ratingInvitationEnabled,
                ),
            )
        }
            .readResult<NotificationPreferencesDto>(json)
            .map { it.toDomain() }
    }
}

class ClearanceRepositoryImpl(
    private val client: HttpClient,
    private val json: Json,
) : ClearanceRepository {
    override suspend fun getClearances(): AppResult<List<InstructorClearance>> = apiCall {
        client.get("clients/me/clearances")
            .readResult<ClearanceListDto>(json)
            .map { dto -> dto.items.map { it.toDomain() } }
    }
}

class DeviceRepositoryImpl(
    private val client: HttpClient,
    private val json: Json,
) : DeviceRepository {
    override suspend fun registerPushToken(registration: PushTokenRegistration): AppResult<Unit> = apiCall {
        client.put("devices/push-token") { setBody(registration.toDto()) }
            .readUnit(json)
    }
}

class RatingRepositoryImpl(
    private val client: HttpClient,
    private val json: Json,
    private val cache: ru.vertical.climbing.data.local.LocalCache,
) : RatingRepository {
    override suspend fun createRating(command: CreateRatingCommand): AppResult<InstructorRating> = apiCall {
        client.post("ratings") { setBody(command.toDto()) }
            .readResult<InstructorRatingDto>(json)
            .map { it.toDomain() }
    }

    override fun submittedStarsFor(bookingId: String): Int? = cache.submittedStarsFor(bookingId)

    override fun markSubmitted(bookingId: String, stars: Int) {
        cache.markRatingSubmitted(bookingId, stars)
    }
}
