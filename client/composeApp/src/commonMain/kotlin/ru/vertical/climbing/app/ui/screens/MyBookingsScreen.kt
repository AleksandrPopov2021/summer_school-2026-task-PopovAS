@file:OptIn(ExperimentalMaterial3Api::class)

package ru.vertical.climbing.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import ru.vertical.climbing.app.navigation.MyBookingsListComponent
import ru.vertical.climbing.app.ui.ErrorView
import ru.vertical.climbing.app.ui.LoadingView
import ru.vertical.climbing.app.ui.OfflineBanner
import ru.vertical.climbing.app.ui.format.heroDateLabel
import ru.vertical.climbing.app.ui.format.timeLabel
import ru.vertical.climbing.domain.model.Booking
import ru.vertical.climbing.domain.model.PaymentStatus
import ru.vertical.climbing.presentation.Async
import ru.vertical.climbing.resources.Res
import ru.vertical.climbing.resources.action_book
import ru.vertical.climbing.resources.bookings_rate_prompt
import ru.vertical.climbing.resources.bookings_rate_action
import ru.vertical.climbing.resources.bookings_empty_subtitle
import ru.vertical.climbing.resources.empty_bookings
import ru.vertical.climbing.resources.payment_status_paid
import ru.vertical.climbing.resources.payment_status_refund
import ru.vertical.climbing.resources.payment_status_unpaid
import ru.vertical.climbing.resources.title_my_bookings

/** SCR-006 — список активных записей (FR-016, FR-023). */
@Composable
fun MyBookingsScreen(component: MyBookingsListComponent) {
    val model by component.model.subscribeAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.title_my_bookings)) }) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (model.isOffline) OfflineBanner()
            PullToRefreshBox(
                isRefreshing = model.isRefreshing,
                onRefresh = component::onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val content = model.content) {
                    Async.Idle, Async.Loading -> LoadingView()
                    Async.Empty -> EmptyBookingsView(onGoToSchedule = component::onGoToSchedule)
                    is Async.Error -> ErrorView(error = content.error, onRetry = component::onRetry)
                    is Async.Content -> BookingsList(
                        bookings = content.data,
                        rateableBookings = model.rateableBookings,
                        onBookingClicked = component::onBookingClicked,
                        onRateBooking = component::onRateBooking,
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingsList(
    bookings: List<Booking>,
    rateableBookings: List<Booking>,
    onBookingClicked: (String) -> Unit,
    onRateBooking: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (rateableBookings.isNotEmpty()) {
            item(key = "rateable_header") {
                Text(
                    text = stringResource(Res.string.bookings_rate_prompt),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            items(rateableBookings, key = { "rate-${it.id}" }) { booking ->
                RateableBookingCard(booking = booking, onRate = { onRateBooking(booking.id) })
            }
        }
        items(bookings, key = { it.id }) { booking ->
            BookingCard(booking = booking, onClick = { onBookingClicked(booking.id) })
        }
    }
}

@Composable
private fun RateableBookingCard(booking: Booking, onRate: () -> Unit) {
    val slot = booking.slot
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(slot.startsAt.heroDateLabel(), style = MaterialTheme.typography.labelMedium)
            Text(
                text = "${slot.startsAt.timeLabel()} · ${slot.instructor.fullName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedButton(onClick = onRate, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Star, contentDescription = null)
                Text(
                    stringResource(Res.string.bookings_rate_action),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun BookingCard(booking: Booking, onClick: () -> Unit) {
    val slot = booking.slot
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(slot.startsAt.heroDateLabel(), style = MaterialTheme.typography.labelMedium)
            Text(
                text = "${slot.startsAt.timeLabel()} · ${slot.zone.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(slot.instructor.fullName, style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${booking.payment.totalAmount.toInt()} ₽",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                PaymentStatusChip(status = booking.payment.paymentStatus)
            }
        }
    }
}

@Composable
fun PaymentStatusChip(status: PaymentStatus) {
    val (label, color) = when (status) {
        PaymentStatus.UNPAID -> stringResource(Res.string.payment_status_unpaid) to MaterialTheme.colorScheme.error
        PaymentStatus.PAID -> stringResource(Res.string.payment_status_paid) to MaterialTheme.colorScheme.primary
        PaymentStatus.REFUND -> stringResource(Res.string.payment_status_refund) to MaterialTheme.colorScheme.secondary
        PaymentStatus.UNKNOWN -> stringResource(Res.string.payment_status_unpaid) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun EmptyBookingsView(onGoToSchedule: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = stringResource(Res.string.empty_bookings),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.bookings_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onGoToSchedule) {
                Text(stringResource(Res.string.action_book))
            }
        }
    }
}
