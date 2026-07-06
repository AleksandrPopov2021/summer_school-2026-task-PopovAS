package ru.vertical.climbing.data

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import ru.vertical.climbing.data.local.LocalCache
import ru.vertical.climbing.data.remote.appJson
import ru.vertical.climbing.data.remote.mock.MockApi
import ru.vertical.climbing.data.repository.BookingRepositoryImpl
import ru.vertical.climbing.domain.model.RentalLineInput
import ru.vertical.climbing.domain.model.UpdateRentalCommand
import ru.vertical.climbing.domain.usecase.GetBookingDetailUseCase
import ru.vertical.climbing.domain.usecase.UpdateBookingRentalUseCase
import ru.vertical.climbing.domain.util.AppResult

class UpdateBookingRentalIntegrationTest {

    private val json = appJson

    @Test
    fun update_rental_refreshes_booking_detail() = runTest {
        val client = HttpClient(MockApi.createEngine(json)) {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
            install(DefaultRequest) {
                url("https://api.local/v1/")
                contentType(ContentType.Application.Json)
            }
        }
        val cache = LocalCache(MapSettings(), json)
        val repository = BookingRepositoryImpl(client, cache, json)
        val updateRental = UpdateBookingRentalUseCase(repository)
        val getDetail = GetBookingDetailUseCase(repository)

        val before = getDetail("booking-mock-1")
        assertTrue(before is AppResult.Success)
        assertEquals(0.0, before.value.payment.rentalAmount)

        val updateResult = updateRental(
            bookingId = "booking-mock-1",
            command = UpdateRentalCommand(
                usesOwnEquipment = false,
                rentalLines = listOf(
                    RentalLineInput("eq-shoes", 1),
                    RentalLineInput("eq-harness", 1),
                ),
            ),
        )
        assertTrue(updateResult is AppResult.Success)
        assertEquals(500.0, updateResult.value.payment.rentalAmount)
        assertEquals(2, updateResult.value.rentalLines.size)

        val after = getDetail("booking-mock-1")
        assertTrue(after is AppResult.Success)
        assertEquals(updateResult.value.payment.totalAmount, after.value.payment.totalAmount)
        assertEquals(updateResult.value.rentalLines.size, after.value.rentalLines.size)
        assertEquals("eq-shoes", after.value.rentalLines.first().equipmentTypeId)
    }
}
