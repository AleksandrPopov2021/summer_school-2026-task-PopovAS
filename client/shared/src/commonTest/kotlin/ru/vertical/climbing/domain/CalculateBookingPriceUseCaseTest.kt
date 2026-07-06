package ru.vertical.climbing.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import ru.vertical.climbing.domain.model.RentalEquipmentCode
import ru.vertical.climbing.domain.model.RentalEquipmentType
import ru.vertical.climbing.domain.model.RentalLineInput
import ru.vertical.climbing.domain.usecase.CalculateBookingPriceUseCase

class CalculateBookingPriceUseCaseTest {

    private val useCase = CalculateBookingPriceUseCase()

    private val equipment = listOf(
        RentalEquipmentType("eq-shoes", RentalEquipmentCode.SHOES, "Туфли", 300.0),
        RentalEquipmentType("eq-harness", RentalEquipmentCode.HARNESS, "Система", 200.0),
    )

    @Test
    fun training_plus_rental_without_discount() {
        val result = useCase(
            trainingPrice = 1200.0,
            rentalLines = listOf(RentalLineInput("eq-shoes", 1), RentalLineInput("eq-harness", 2)),
            equipmentTypes = equipment,
            loyaltyDiscount = null,
        )
        assertEquals(1200.0, result.trainingAmount)
        assertEquals(700.0, result.rentalAmount)
        assertEquals(0.0, result.discountAmount)
        assertEquals(1900.0, result.total)
    }

    @Test
    fun loyalty_discount_applied_and_clamped() {
        val result = useCase(
            trainingPrice = 1000.0,
            rentalLines = emptyList(),
            equipmentTypes = equipment,
            loyaltyDiscount = 150.0,
        )
        assertEquals(850.0, result.total)
    }
}
