package ru.vertical.climbing.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import ru.vertical.climbing.domain.model.Client
import ru.vertical.climbing.domain.model.EditableNotificationToggles
import ru.vertical.climbing.domain.model.NotificationPreferences
import ru.vertical.climbing.domain.model.buildProfileLoyaltyState
import ru.vertical.climbing.domain.model.shouldShowLoyaltyBadge
import ru.vertical.climbing.domain.repository.NotificationPreferencesRepository
import ru.vertical.climbing.domain.usecase.UpdateNotificationPreferencesUseCase
import ru.vertical.climbing.domain.util.AppResult

class ProfileLoyaltyTest {

    @Test
    fun loyalty_badge_visible_only_for_loyal_client() {
        val regular = sampleClient.copy(isLoyalClient = false)
        val loyal = sampleClient.copy(isLoyalClient = true)

        assertFalse(regular.shouldShowLoyaltyBadge())
        assertTrue(loyal.shouldShowLoyaltyBadge())
    }

    @Test
    fun loyalty_progress_and_remaining_visits() {
        val client = sampleClient.copy(completedVisitsCount = 7, isLoyalClient = false)
        val state = buildProfileLoyaltyState(client, visitsForLoyalty = 10)

        assertFalse(state.showBadge)
        assertEquals(7, state.completedVisits)
        assertEquals(3, state.remainingVisits)
        assertEquals(0.7f, state.progress)
    }

    @Test
    fun loyal_client_has_full_progress_and_badge() {
        val client = sampleClient.copy(
            completedVisitsCount = 12,
            isLoyalClient = true,
            loyaltyDiscount = 15.0,
        )
        val state = buildProfileLoyaltyState(client, visitsForLoyalty = 10)

        assertTrue(state.showBadge)
        assertEquals(1f, state.progress)
        assertEquals(0, state.remainingVisits)
        assertEquals(15, state.loyaltyDiscountPercent)
    }

    private val sampleClient = Client(
        id = "c1",
        fullName = "Test User",
        phone = "+79001234567",
        birthDate = kotlinx.datetime.LocalDate.parse("1995-03-15"),
        riskConsentAccepted = true,
        completedVisitsCount = 0,
        isLoyalClient = false,
        loyaltyDiscount = null,
        lateCancellationCount = 0,
        noShowCount = 0,
    )
}

class NotificationPreferencesUpdateTest {

    @Test
    fun update_sends_only_editable_fields() = runTest {
        val repository = TrackingNotificationPreferencesRepository()
        val useCase = UpdateNotificationPreferencesUseCase(repository)

        useCase(
            EditableNotificationToggles(
                bookingConfirmationEnabled = false,
                ratingInvitationEnabled = true,
            ),
        )

        assertEquals(false, repository.lastBookingConfirmation)
        assertEquals(true, repository.lastRatingInvitation)
        assertEquals(1, repository.updateCallCount)
    }

    private class TrackingNotificationPreferencesRepository : NotificationPreferencesRepository {
        var lastBookingConfirmation: Boolean? = null
            private set
        var lastRatingInvitation: Boolean? = null
            private set
        var updateCallCount = 0
            private set

        override suspend fun get(): AppResult<NotificationPreferences> = AppResult.Success(samplePreferences())

        override suspend fun update(
            bookingConfirmationEnabled: Boolean?,
            ratingInvitationEnabled: Boolean?,
        ): AppResult<NotificationPreferences> {
            updateCallCount++
            lastBookingConfirmation = bookingConfirmationEnabled
            lastRatingInvitation = ratingInvitationEnabled
            return AppResult.Success(
                samplePreferences().copy(
                    bookingConfirmationEnabled = bookingConfirmationEnabled ?: true,
                    ratingInvitationEnabled = ratingInvitationEnabled ?: true,
                ),
            )
        }

        private fun samplePreferences() = NotificationPreferences(
            id = "np-1",
            clientId = "c1",
            bookingConfirmationEnabled = true,
            ratingInvitationEnabled = true,
            remindersEnabled = false,
            gymCancellationEnabled = false,
        )
    }
}
