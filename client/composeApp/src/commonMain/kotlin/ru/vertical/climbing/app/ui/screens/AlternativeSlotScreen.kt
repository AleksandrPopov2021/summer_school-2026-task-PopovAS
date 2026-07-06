@file:OptIn(ExperimentalMaterial3Api::class)

package ru.vertical.climbing.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import ru.vertical.climbing.app.navigation.AlternativeSlotComponent
import ru.vertical.climbing.app.ui.ErrorView
import ru.vertical.climbing.app.ui.LoadingView
import ru.vertical.climbing.app.ui.errorMessageFor
import ru.vertical.climbing.app.ui.format.endTimeLabel
import ru.vertical.climbing.app.ui.format.heroDateLabel
import ru.vertical.climbing.app.ui.format.timeLabel
import ru.vertical.climbing.domain.model.Booking
import ru.vertical.climbing.domain.model.TrainingSlot
import ru.vertical.climbing.domain.model.availabilityState
import ru.vertical.climbing.presentation.Async
import ru.vertical.climbing.resources.Res
import ru.vertical.climbing.resources.action_book_alternative
import ru.vertical.climbing.resources.action_choose_other_slot
import ru.vertical.climbing.resources.action_later
import ru.vertical.climbing.resources.alternative_badge_recommended
import ru.vertical.climbing.resources.alternative_book_unavailable_hint
import ru.vertical.climbing.resources.alternative_not_found_message
import ru.vertical.climbing.resources.gym_cancel_apology
import ru.vertical.climbing.resources.gym_cancel_banner_title
import ru.vertical.climbing.resources.schedule_rental_from
import ru.vertical.climbing.resources.schedule_spots_left
import ru.vertical.climbing.resources.slot_time_range
import ru.vertical.climbing.resources.title_alternative_slot

/** SCR-009 — предложение альтернативного слота (LOGIC-009). */
@Composable
fun AlternativeSlotScreen(component: AlternativeSlotComponent) {
    val model by component.model.subscribeAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    model.snackbarError?.let { error ->
        val errorMessage = errorMessageFor(error)
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(errorMessage)
            component.onDismissSnackbar()
        }
    }

    val bookingAsync = model.booking
    val alternativeAsync = model.alternative
    val booking = bookingAsync as? Async.Content
    val alternative = alternativeAsync

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_alternative_slot)) },
                navigationIcon = {
                    IconButton(onClick = component::onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (booking != null && alternative is Async.Content) {
                AlternativeActions(
                    alternative = alternative.data,
                    onBook = component::onBookAlternative,
                    onChooseOther = component::onChooseOtherSlot,
                    onLater = component::onLater,
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                bookingAsync is Async.Loading || alternativeAsync is Async.Loading -> LoadingView()
                bookingAsync is Async.Error -> ErrorView(
                    error = bookingAsync.error,
                    onRetry = component::onRetry,
                )
                alternativeAsync is Async.Error -> {
                    if (booking != null) {
                        AlternativeContent(
                            booking = booking.data,
                            alternative = null,
                            error = alternativeAsync.error,
                            onRetry = component::onRetry,
                        )
                    } else {
                        ErrorView(error = alternativeAsync.error, onRetry = component::onRetry)
                    }
                }
                booking != null && alternative is Async.Content -> AlternativeContent(
                    booking = booking.data,
                    alternative = alternative.data,
                    error = null,
                    onRetry = component::onRetry,
                )
            }
        }
    }
}

@Composable
private fun AlternativeContent(
    booking: Booking,
    alternative: AlternativeSlotComponent.AlternativeState?,
    error: ru.vertical.climbing.domain.util.AppError?,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CancelledBookingBanner(booking)

        when (alternative) {
            is AlternativeSlotComponent.AlternativeState.Found -> AlternativeSlotCard(alternative.slot)
            AlternativeSlotComponent.AlternativeState.NotFound, null -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(Res.string.alternative_not_found_message),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }

        error?.let {
            ErrorView(error = it, onRetry = onRetry)
        }
    }
}

@Composable
private fun CancelledBookingBanner(booking: Booking) {
    val slot = booking.slot
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(Res.string.gym_cancel_banner_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${slot.startsAt.heroDateLabel()} · ${slot.startsAt.timeLabel()} · ${slot.zone.name}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = booking.cancellationReason?.title ?: stringResource(Res.string.gym_cancel_apology),
                style = MaterialTheme.typography.bodySmall,
            )
            booking.cancellationReason?.apologyText?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AlternativeSlotCard(slot: TrainingSlot) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(Res.string.alternative_badge_recommended),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(slot.startsAt.heroDateLabel(), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(
                    Res.string.slot_time_range,
                    slot.startsAt.timeLabel(),
                    endTimeLabel(slot.startsAt, slot.durationMinutes),
                ),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text("${slot.zone.name} · ${slot.instructor.fullName}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(Res.string.schedule_spots_left, slot.freeSpots, slot.capacity),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            slot.rentalTariff?.let { tariff ->
                Text(
                    text = stringResource(Res.string.schedule_rental_from, tariff.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = slot.availabilityState().statusLabel(),
                style = MaterialTheme.typography.labelMedium,
                color = slot.availabilityState().statusColor(),
            )
        }
    }
}

@Composable
private fun AlternativeActions(
    alternative: AlternativeSlotComponent.AlternativeState,
    onBook: () -> Unit,
    onChooseOther: () -> Unit,
    onLater: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (alternative is AlternativeSlotComponent.AlternativeState.Found) {
            if (!alternative.canBook) {
                Text(
                    text = stringResource(Res.string.alternative_book_unavailable_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = onBook,
                enabled = alternative.canBook,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.action_book_alternative))
            }
        }
        OutlinedButton(onClick = onChooseOther, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.action_choose_other_slot))
        }
        TextButton(onClick = onLater, modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)) {
            Text(stringResource(Res.string.action_later))
        }
    }
}
