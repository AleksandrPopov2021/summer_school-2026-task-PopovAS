package ru.vertical.climbing.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ru.vertical.climbing.domain.model.RentalLineInput
import ru.vertical.climbing.domain.model.hasRentalSelectionChanged
import ru.vertical.climbing.domain.model.rentalSelectionEquals

class RentalSelectionDiffTest {

    @Test
    fun equal_selections_are_not_changed() {
        val lines = listOf(RentalLineInput("eq-shoes", 1))
        assertTrue(
            rentalSelectionEquals(
                usesOwnA = true,
                linesA = lines,
                usesOwnB = true,
                linesB = lines,
            ),
        )
        assertFalse(
            hasRentalSelectionChanged(
                originalUsesOwn = true,
                originalLines = lines,
                newUsesOwn = true,
                newLines = lines,
            ),
        )
    }

    @Test
    fun added_rental_line_is_changed() {
        val original = listOf(RentalLineInput("eq-shoes", 1))
        val updated = listOf(
            RentalLineInput("eq-shoes", 1),
            RentalLineInput("eq-harness", 1),
        )
        assertFalse(
            rentalSelectionEquals(
                usesOwnA = false,
                linesA = original,
                usesOwnB = false,
                linesB = updated,
            ),
        )
        assertTrue(
            hasRentalSelectionChanged(
                originalUsesOwn = false,
                originalLines = original,
                newUsesOwn = false,
                newLines = updated,
            ),
        )
    }

    @Test
    fun own_equipment_toggle_is_changed() {
        assertTrue(
            hasRentalSelectionChanged(
                originalUsesOwn = true,
                originalLines = emptyList(),
                newUsesOwn = false,
                newLines = listOf(RentalLineInput("eq-shoes", 1)),
            ),
        )
    }
}
