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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import ru.vertical.climbing.app.navigation.BookingDetailComponent
import ru.vertical.climbing.app.ui.ErrorView
import ru.vertical.climbing.app.ui.LoadingView
import ru.vertical.climbing.app.ui.OfflineBanner
import ru.vertical.climbing.app.ui.UiTestTags
import ru.vertical.climbing.app.ui.errorMessageFor
import ru.vertical.climbing.app.ui.format.endTimeLabel
import ru.vertical.climbing.app.ui.format.heroDateLabel
import ru.vertical.climbing.app.ui.format.timeLabel
import ru.vertical.climbing.domain.model.Booking
import ru.vertical.climbing.domain.model.BookingStatus
import ru.vertical.climbing.domain.model.canFindAlternative
import ru.vertical.climbing.domain.model.canModifyRental
import ru.vertical.climbing.domain.model.canOpenCancellationScreen
import ru.vertical.climbing.domain.model.isCancellationForbidden
import ru.vertical.climbing.domain.model.showCancelAction
import ru.vertical.climbing.presentation.Async
import ru.vertical.climbing.resources.Res
import ru.vertical.climbing.resources.action_cancel_booking
import ru.vertical.climbing.resources.action_find_alternative
import ru.vertical.climbing.resources.action_modify_rental
import ru.vertical.climbing.resources.booking_discount
import ru.vertical.climbing.resources.booking_own_equipment
import ru.vertical.climbing.resources.booking_rental_price
import ru.vertical.climbing.resources.booking_total
import ru.vertical.climbing.resources.booking_training_price
import ru.vertical.climbing.resources.cancel_forbidden_hint
import ru.vertical.climbing.resources.gym_cancel_apology
import ru.vertical.climbing.resources.gym_cancel_title
import ru.vertical.climbing.resources.label_payment
import ru.vertical.climbing.resources.label_rental
import ru.vertical.climbing.resources.rental_updated_success
import ru.vertical.climbing.resources.slot_time_range
import ru.vertical.climbing.resources.title_booking_detail

/** SCR-007 — детали записи. */
@Composable
fun BookingDetailScreen(component: BookingDetailComponent) {
    val model by component.model.subscribeAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    model.snackbarError?.let { error ->
        val errorMessage = errorMessageFor(error)
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(errorMessage)
            component.onDismissSnackbar()
        }
    }

    val rentalUpdatedMessage = stringResource(Res.string.rental_updated_success)
    if (model.rentalUpdated) {
        LaunchedEffect(model.rentalUpdated) {
            snackbarHostState.showSnackbar(rentalUpdatedMessage)
            component.onDismissSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_booking_detail)) },
                navigationIcon = {
                    IconButton(onClick = component::onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val content = model.content
            if (content is Async.Content) {
                DetailActions(
                    booking = content.data,
                    onCancel = component::onCancelBooking,
                    onFindAlternative = component::onFindAlternative,
                )
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (model.isOffline) OfflineBanner()
            Box(modifier = Modifier.fillMaxSize()) {
                when (val content = model.content) {
                Async.Idle, Async.Loading -> LoadingView()
                Async.Empty -> Unit
                is Async.Error -> ErrorView(error = content.error, onRetry = component::onRetry)
                is Async.Content -> BookingDetailContent(
                    booking = content.data,
                    onModifyRental = component::onModifyRental,
                )
            }
            }
        }
    }
}

@Composable
private fun BookingDetailContent(
    booking: Booking,
    onModifyRental: () -> Unit,
) {
    val slot = booking.slot
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (booking.bookingStatus == BookingStatus.CANCELLED_BY_GYM) {
            GymCancellationBanner(booking)
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(slot.startsAt.heroDateLabel(), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(
                    Res.string.slot_time_range,
                    slot.startsAt.timeLabel(),
                    endTimeLabel(slot.startsAt, slot.durationMinutes),
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text("${slot.zone.name} · ${slot.instructor.fullName}", style = MaterialTheme.typography.bodyMedium)
            Text(slot.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SectionCard(title = stringResource(Res.string.label_rental)) {
            if (booking.usesOwnEquipment) {
                Text("✓ ${stringResource(Res.string.booking_own_equipment)}", style = MaterialTheme.typography.bodyMedium)
            }
            booking.rentalLines.forEach { line ->
                Text("✓ ${line.equipmentType.name}", style = MaterialTheme.typography.bodyMedium)
            }
            if (booking.canModifyRental()) {
                OutlinedButton(
                    onClick = onModifyRental,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.action_modify_rental))
                }
            }
        }

        SectionCard(title = stringResource(Res.string.booking_total)) {
            PriceRow(stringResource(Res.string.booking_training_price), booking.payment.trainingAmount)
            if (booking.payment.rentalAmount > 0) {
                PriceRow(stringResource(Res.string.booking_rental_price), booking.payment.rentalAmount)
            }
            booking.payment.discountAmount?.takeIf { it > 0 }?.let {
                PriceRow(stringResource(Res.string.booking_discount), -it)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            PriceRow(
                label = stringResource(Res.string.booking_total),
                amount = booking.payment.totalAmount,
                bold = true,
            )
        }

        SectionCard(title = stringResource(Res.string.label_payment)) {
            PaymentStatusChip(status = booking.payment.paymentStatus)
        }
    }
}

@Composable
private fun GymCancellationBanner(booking: Booking) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = booking.cancellationReason?.title ?: stringResource(Res.string.gym_cancel_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            booking.cancellationReason?.apologyText?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            } ?: Text(stringResource(Res.string.gym_cancel_apology), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DetailActions(
    booking: Booking,
    onCancel: () -> Unit,
    onFindAlternative: () -> Unit,
) {
    if (booking.canFindAlternative()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onFindAlternative, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.action_find_alternative))
            }
        }
        return
    }

    if (!booking.showCancelAction()) return

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (booking.isCancellationForbidden()) {
            Text(
                text = stringResource(Res.string.cancel_forbidden_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = onCancel,
            enabled = booking.canOpenCancellationScreen(),
            modifier = Modifier.fillMaxWidth().testTag(UiTestTags.CANCEL_BOOKING),
        ) {
            Text(stringResource(Res.string.action_cancel_booking))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun PriceRow(label: String, amount: Double, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            "${amount.toInt()} ₽",
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
