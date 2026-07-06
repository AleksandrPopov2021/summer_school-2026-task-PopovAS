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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import ru.vertical.climbing.app.navigation.BookingComponent
import ru.vertical.climbing.app.ui.ErrorView
import ru.vertical.climbing.app.ui.UiTestTags
import ru.vertical.climbing.app.ui.LoadingView
import ru.vertical.climbing.app.ui.errorMessageFor
import ru.vertical.climbing.app.ui.format.heroDateLabel
import ru.vertical.climbing.app.ui.format.timeLabel
import ru.vertical.climbing.domain.model.SlotRentalAvailability
import ru.vertical.climbing.domain.usecase.CalculateBookingPriceUseCase
import ru.vertical.climbing.presentation.Async
import ru.vertical.climbing.resources.Res
import ru.vertical.climbing.resources.action_cancel
import ru.vertical.climbing.resources.action_confirm
import ru.vertical.climbing.resources.action_ok
import ru.vertical.climbing.resources.booking_discount
import ru.vertical.climbing.resources.booking_rental_price
import ru.vertical.climbing.resources.error_booking_conflict
import ru.vertical.climbing.resources.booking_own_equipment
import ru.vertical.climbing.resources.booking_rental_available
import ru.vertical.climbing.resources.booking_rental_unavailable
import ru.vertical.climbing.resources.action_confirm_booking
import ru.vertical.climbing.resources.action_save_rental
import ru.vertical.climbing.resources.booking_title
import ru.vertical.climbing.resources.edit_rental_title
import ru.vertical.climbing.resources.booking_total
import ru.vertical.climbing.resources.booking_training_price
import ru.vertical.climbing.resources.slot_time_range

/** SCR-005 Booking Screen — оформление записи (LOGIC-005, LOGIC-007). */
@Composable
fun BookingScreen(component: BookingComponent) {
    val model by component.model.subscribeAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    model.snackbarError?.let { error ->
        val errorMessage = errorMessageFor(error)
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(errorMessage)
            component.onDismissSnackbar()
        }
    }

    model.conflictMessage?.let { message ->
        AlertDialog(
            onDismissRequest = component::onDismissConflictDialog,
            title = { Text(stringResource(Res.string.error_booking_conflict)) },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = component::onDismissConflictDialog) {
                    Text(stringResource(Res.string.action_ok))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (component.isEditMode) Res.string.edit_rental_title else Res.string.booking_title,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = component::onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (model.content is Async.Content) {
                BookingBottomBar(
                    isEditMode = component.isEditMode,
                    enabled = model.canConfirm,
                    isSubmitting = model.isSubmitting,
                    onConfirm = component::onConfirm,
                    onCancel = component::onBack,
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val content = model.content) {
                Async.Idle, Async.Loading -> LoadingView()
                Async.Empty -> Unit
                is Async.Error -> ErrorView(error = content.error, onRetry = component::onRetry)
                is Async.Content -> BookingContent(
                    content = content.data,
                    usesOwnEquipment = model.usesOwnEquipment,
                    selectedRentalIds = model.selectedRentalIds,
                    heldRentalIds = model.heldRentalIds,
                    priceBreakdown = model.priceBreakdown,
                    onOwnEquipmentToggled = component::onOwnEquipmentToggled,
                    onRentalToggled = component::onRentalToggled,
                )
            }
        }
    }
}

@Composable
private fun BookingContent(
    content: BookingComponent.Content,
    usesOwnEquipment: Boolean,
    selectedRentalIds: Set<String>,
    heldRentalIds: Set<String>,
    priceBreakdown: CalculateBookingPriceUseCase.PriceBreakdown?,
    onOwnEquipmentToggled: (Boolean) -> Unit,
    onRentalToggled: (String, Boolean) -> Unit,
) {
    val slot = content.slot
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(slot.startsAt.heroDateLabel(), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(
                        Res.string.slot_time_range,
                        slot.startsAt.timeLabel(),
                        ru.vertical.climbing.app.ui.format.endTimeLabel(slot.startsAt, slot.durationMinutes),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text("${slot.zone.name} · ${slot.instructor.fullName}", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.booking_own_equipment),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Switch(checked = usesOwnEquipment, onCheckedChange = onOwnEquipmentToggled)
                }
                content.equipmentTypes.forEach { type ->
                    val availability = content.rentalAvailability.find { it.equipmentTypeId == type.id }
                    RentalRow(
                        name = type.name,
                        price = type.defaultPrice,
                        availability = availability,
                        equipmentTypeId = type.id,
                        heldRentalIds = heldRentalIds,
                        checked = selectedRentalIds.contains(type.id),
                        onCheckedChange = { onRentalToggled(type.id, it) },
                    )
                }
            }
        }

        priceBreakdown?.let { PriceBreakdownCard(it) }
    }
}

@Composable
private fun RentalRow(
    name: String,
    price: Double,
    availability: SlotRentalAvailability?,
    equipmentTypeId: String,
    heldRentalIds: Set<String>,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val available = availability?.availableQuantity ?: 0
    val enabled = available > 0 || heldRentalIds.contains(equipmentTypeId) || checked
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
            Column {
                Text(name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (enabled) {
                        stringResource(Res.string.booking_rental_available, available)
                    } else {
                        stringResource(Res.string.booking_rental_unavailable)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text("${price.toInt()} ₽", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PriceBreakdownCard(breakdown: CalculateBookingPriceUseCase.PriceBreakdown) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PriceRow(stringResource(Res.string.booking_training_price), breakdown.trainingAmount)
            if (breakdown.rentalAmount > 0) {
                PriceRow(stringResource(Res.string.booking_rental_price), breakdown.rentalAmount)
            }
            if (breakdown.discountAmount > 0) {
                PriceRow(stringResource(Res.string.booking_discount), -breakdown.discountAmount)
            }
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(Res.string.booking_total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${breakdown.total.toInt()} ₽",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun PriceRow(label: String, amount: Double) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text("${amount.toInt()} ₽", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BookingBottomBar(
    isEditMode: Boolean,
    enabled: Boolean,
    isSubmitting: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onConfirm,
            enabled = enabled && !isSubmitting,
            modifier = Modifier.fillMaxWidth().testTag(UiTestTags.BOOKING_CONFIRM),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
            }
            Text(
                stringResource(
                    if (isEditMode) Res.string.action_save_rental else Res.string.action_confirm_booking,
                ),
            )
        }
        TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(Res.string.action_cancel))
        }
    }
}
