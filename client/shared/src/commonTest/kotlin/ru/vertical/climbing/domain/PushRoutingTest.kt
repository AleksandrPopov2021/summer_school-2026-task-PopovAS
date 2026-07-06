package ru.vertical.climbing.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import ru.vertical.climbing.domain.model.PushNavigationTarget
import ru.vertical.climbing.domain.model.PushNotificationType
import ru.vertical.climbing.domain.model.directPushNavigationTarget
import ru.vertical.climbing.domain.model.parseDeepLink
import ru.vertical.climbing.domain.model.parsePushData
import ru.vertical.climbing.domain.model.primaryPushNavigationTarget

class PushRoutingTest {

    @Test
    fun booking_confirmed_primary_goes_to_bookings_list() {
        val payload = payload(type = "booking_confirmed", bookingId = "b1")
        assertEquals(PushNavigationTarget.BookingsList, primaryPushNavigationTarget(payload))
    }

    @Test
    fun reminder_routes_to_booking_detail() {
        val payload = payload(type = "reminder", bookingId = "b2")
        assertEquals(PushNavigationTarget.BookingDetail("b2"), directPushNavigationTarget(payload))
        assertEquals(PushNavigationTarget.BookingDetail("b2"), primaryPushNavigationTarget(payload))
    }

    @Test
    fun gym_cancellation_routes_to_alternative() {
        val payload = payload(type = "gym_cancellation", bookingId = "b3", slotId = "s3")
        val target = directPushNavigationTarget(payload)
        assertEquals(PushNavigationTarget.AlternativeSlot("b3", "s3"), target)
    }

    @Test
    fun rating_invitation_routes_to_rating_stub() {
        val payload = payload(type = "rating_invitation", bookingId = "b4")
        assertEquals(PushNavigationTarget.RatingStub("b4"), primaryPushNavigationTarget(payload))
    }

    @Test
    fun deep_link_booking_detail() {
        assertEquals(
            PushNavigationTarget.BookingDetail("abc-123"),
            parseDeepLink("vertical://bookings/abc-123"),
        )
    }

    @Test
    fun deep_link_alternative() {
        assertEquals(
            PushNavigationTarget.AlternativeSlot("b1", "b1"),
            parseDeepLink("vertical://bookings/b1/alternative"),
        )
        assertEquals(
            PushNavigationTarget.AlternativeSlot("b1", "s9"),
            parseDeepLink("vertical://bookings/b1/alternative/s9"),
        )
    }

    @Test
    fun deep_link_schedule() {
        assertEquals(PushNavigationTarget.Schedule, parseDeepLink("vertical://schedule"))
    }

    @Test
    fun parse_push_data_map() {
        val parsed = parsePushData(
            mapOf(
                "type" to "reminder",
                "booking_id" to "x1",
            ),
        )
        assertNotNull(parsed)
        assertEquals(PushNotificationType.REMINDER, parsed.notificationType)
        assertTrue(parsed.isValid)
    }

    @Test
    fun invalid_gym_cancel_missing_slot() {
        val payload = payload(type = "gym_cancellation", bookingId = "b1", slotId = null)
        assertNull(directPushNavigationTarget(payload))
    }

    private fun payload(
        type: String,
        bookingId: String?,
        slotId: String? = null,
    ) = ru.vertical.climbing.domain.model.PushNotificationPayload(
        type = type,
        bookingId = bookingId,
        slotId = slotId,
    )
}
