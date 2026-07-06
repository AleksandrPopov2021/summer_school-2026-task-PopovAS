package ru.vertical.climbing.app.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import ru.vertical.climbing.domain.model.SlotAvailabilityState
import ru.vertical.climbing.resources.Res
import ru.vertical.climbing.resources.schedule_status_available
import ru.vertical.climbing.resources.schedule_status_booking_closed
import ru.vertical.climbing.resources.schedule_status_cancelled
import ru.vertical.climbing.resources.schedule_status_clearance
import ru.vertical.climbing.resources.schedule_status_few_spots
import ru.vertical.climbing.resources.schedule_status_no_spots

/** Текст статуса доступности слота (LOGIC-004). */
@Composable
fun SlotAvailabilityState.statusLabel(): String = when (this) {
    SlotAvailabilityState.AVAILABLE -> stringResource(Res.string.schedule_status_available)
    SlotAvailabilityState.FEW_SPOTS -> stringResource(Res.string.schedule_status_few_spots)
    SlotAvailabilityState.NO_SPOTS -> stringResource(Res.string.schedule_status_no_spots)
    SlotAvailabilityState.BOOKING_CLOSED -> stringResource(Res.string.schedule_status_booking_closed)
    SlotAvailabilityState.CLEARANCE_REQUIRED -> stringResource(Res.string.schedule_status_clearance)
    SlotAvailabilityState.CANCELLED -> stringResource(Res.string.schedule_status_cancelled)
}

/** Цвет индикатора статуса доступности. */
@Composable
fun SlotAvailabilityState.statusColor(): Color = when (this) {
    SlotAvailabilityState.AVAILABLE -> MaterialTheme.colorScheme.primary
    SlotAvailabilityState.FEW_SPOTS, SlotAvailabilityState.CLEARANCE_REQUIRED -> MaterialTheme.colorScheme.secondary
    SlotAvailabilityState.NO_SPOTS -> MaterialTheme.colorScheme.error
    SlotAvailabilityState.BOOKING_CLOSED, SlotAvailabilityState.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
}
