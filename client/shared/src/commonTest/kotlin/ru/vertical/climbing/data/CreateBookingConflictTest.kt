package ru.vertical.climbing.data

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import ru.vertical.climbing.data.local.LocalCache
import ru.vertical.climbing.data.remote.appJson
import ru.vertical.climbing.data.repository.BookingRepositoryImpl
import ru.vertical.climbing.domain.model.CreateBookingCommand
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.ErrorCode

class CreateBookingConflictTest {

    private val json = appJson

    @Test
    fun maps_409_conflict_with_slot() = runTest {
        val conflictJson = """
            {
              "code": "BOOKING_CONFLICT",
              "message": "Места заняты",
              "slot": {
                "id": "slot-1",
                "starts_at": "2026-07-10T10:00:00Z",
                "duration_minutes": 90,
                "capacity": 8,
                "free_spots": 0,
                "training_price": 1200.0,
                "slot_status": "active",
                "address": "addr",
                "zone": {
                  "id": "z1",
                  "name": "Болдеринг",
                  "format_type": "bouldering_instruction",
                  "difficulty": "beginner",
                  "max_group_size": 8
                },
                "instructor": {
                  "id": "i1",
                  "full_name": "Инструктор"
                },
                "availability": {
                  "can_book": false,
                  "has_free_spots": false,
                  "free_spots": 0,
                  "within_booking_window": true,
                  "clearance_required": false,
                  "clearance_granted": false
                }
              }
            }
        """.trimIndent()

        val engine = MockEngine {
            respond(
                content = ByteReadChannel(conflictJson),
                status = HttpStatusCode.Conflict,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
            install(DefaultRequest) {
                url("https://api.local/v1/")
                contentType(ContentType.Application.Json)
            }
        }
        val cache = LocalCache(MapSettings(), json)
        val repository = BookingRepositoryImpl(client, cache, json)

        val result = repository.createBooking(
            CreateBookingCommand(
                slotId = "slot-1",
                usesOwnEquipment = true,
                rentalLines = emptyList(),
            ),
        )

        assertTrue(result is AppResult.Failure)
        assertEquals(ErrorCode.BOOKING_CONFLICT, result.error.code)
        assertEquals("Места заняты", result.error.message)
        assertNotNull(result.error.conflictSlot)
        assertEquals(0, result.error.conflictSlot!!.freeSpots)
    }
}
