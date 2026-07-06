@file:OptIn(ExperimentalMaterial3Api::class)

package ru.vertical.climbing.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import ru.vertical.climbing.app.navigation.SlotDetailComponent
import ru.vertical.climbing.app.ui.ErrorView
import ru.vertical.climbing.app.ui.LoadingView
import ru.vertical.climbing.app.ui.OfflineBanner
import ru.vertical.climbing.app.ui.format.durationLabel
import ru.vertical.climbing.app.ui.format.endTimeLabel
import ru.vertical.climbing.app.ui.format.heroDateLabel
import ru.vertical.climbing.app.ui.format.timeLabel
import ru.vertical.climbing.domain.model.Difficulty
import ru.vertical.climbing.domain.model.SlotAvailabilityState
import ru.vertical.climbing.domain.model.TrainingSlot
import ru.vertical.climbing.domain.model.availabilityState
import ru.vertical.climbing.domain.model.hidesBookButton
import ru.vertical.climbing.domain.model.isBookable
import ru.vertical.climbing.domain.util.ErrorCode
import ru.vertical.climbing.presentation.Async
import ru.vertical.climbing.resources.Res
import ru.vertical.climbing.resources.action_book
import ru.vertical.climbing.resources.action_to_schedule
import ru.vertical.climbing.resources.rebooking_forbidden_hint
import ru.vertical.climbing.resources.schedule_rental_from
import ru.vertical.climbing.resources.schedule_spots_left
import ru.vertical.climbing.resources.slot_difficulty_beginner
import ru.vertical.climbing.resources.slot_difficulty_experienced
import ru.vertical.climbing.resources.slot_label_availability
import ru.vertical.climbing.resources.slot_label_instructor
import ru.vertical.climbing.resources.slot_label_rental
import ru.vertical.climbing.resources.slot_not_found_title
import ru.vertical.climbing.resources.slot_rental_empty
import ru.vertical.climbing.resources.slot_rental_price
import ru.vertical.climbing.resources.slot_status_cancelled_full
import ru.vertical.climbing.resources.slot_time_range
import ru.vertical.climbing.resources.title_slot_detail

/** SCR-004 Slot Detail Screen — полная информация о слоте (LOGIC-004). */
@Composable
fun SlotDetailScreen(component: SlotDetailComponent) {
    val model by component.model.subscribeAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_slot_detail)) },
                navigationIcon = {
                    IconButton(onClick = component::onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        bottomBar = {
            val content = model.content
            if (content is Async.Content) {
                DetailBottomBar(
                    slot = content.data,
                    rebookingBlocked = content.data.id in model.rebookingForbiddenSlotIds,
                    onBook = component::onBook,
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
                is Async.Error -> if (content.error.code == ErrorCode.NOT_FOUND) {
                    NotFoundView(onBackToSchedule = component::onBack)
                } else {
                    ErrorView(error = content.error, onRetry = component::onRetry)
                }
                is Async.Content -> SlotDetailContent(content.data)
            }
            }
        }
    }
}

@Composable
private fun SlotDetailContent(slot: TrainingSlot) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = slot.startsAt.heroDateLabel(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    Res.string.slot_time_range,
                    slot.startsAt.timeLabel(),
                    endTimeLabel(slot.startsAt, slot.durationMinutes),
                ),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        DetailRow(Icons.Filled.Terrain, slot.zone.name, slot.zone.difficulty.label())
        Text(
            text = "~${durationLabel(slot.durationMinutes)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        InstructorBlock(slot)
        AvailabilityBlock(slot)
        RentalBlock(slot)

        DetailRow(Icons.Filled.LocationOn, slot.venue?.name ?: slot.address, slot.address)
    }
}

@Composable
private fun InstructorBlock(slot: TrainingSlot) {
    SectionCard(title = stringResource(Res.string.slot_label_instructor)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(slot.instructor.fullName, style = MaterialTheme.typography.bodyLarge)
        }
        slot.instructor.averageRating?.let { rating ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp),
                )
                Text(rating.toString(), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun AvailabilityBlock(slot: TrainingSlot) {
    val state = slot.availabilityState()
    SectionCard(title = stringResource(Res.string.slot_label_availability)) {
        val progress = if (slot.capacity > 0) {
            (slot.capacity - slot.freeSpots).toFloat() / slot.capacity.toFloat()
        } else {
            0f
        }
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text(
            text = stringResource(Res.string.schedule_spots_left, slot.freeSpots, slot.capacity),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = if (state == SlotAvailabilityState.CANCELLED) {
                stringResource(Res.string.slot_status_cancelled_full)
            } else {
                state.statusLabel()
            },
            style = MaterialTheme.typography.labelLarge,
            color = state.statusColor(),
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun RentalBlock(slot: TrainingSlot) {
    SectionCard(title = stringResource(Res.string.slot_label_rental)) {
        slot.rentalTariff?.let { tariff ->
            Text(
                text = stringResource(Res.string.schedule_rental_from, tariff.toInt()),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
        val available = slot.rentalAvailability.filter { it.availableQuantity > 0 }
        if (available.isEmpty()) {
            Text(
                text = stringResource(Res.string.slot_rental_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            available.forEach { item ->
                Text(
                    text = stringResource(
                        Res.string.slot_rental_price,
                        item.equipmentType.name,
                        item.equipmentType.defaultPrice.toInt(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailBottomBar(slot: TrainingSlot, rebookingBlocked: Boolean, onBook: () -> Unit) {
    val state = slot.availabilityState()
    if (state.hidesBookButton && !rebookingBlocked) return

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        if (rebookingBlocked) {
            Text(
                text = stringResource(Res.string.rebooking_forbidden_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        if (!rebookingBlocked) {
            Button(
                onClick = onBook,
                enabled = state.isBookable,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isBookable) stringResource(Res.string.action_book) else state.statusLabel())
            }
        }
    }
}

@Composable
private fun NotFoundView(onBackToSchedule: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = stringResource(Res.string.slot_not_found_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Button(onClick = onBackToSchedule) {
                Text(stringResource(Res.string.action_to_schedule))
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, primary: String, secondary: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Column {
            Text(primary, style = MaterialTheme.typography.bodyLarge)
            if (secondary != null && secondary != primary) {
                Text(
                    secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Difficulty.label(): String? = when (this) {
    Difficulty.BEGINNER -> stringResource(Res.string.slot_difficulty_beginner)
    Difficulty.EXPERIENCED -> stringResource(Res.string.slot_difficulty_experienced)
    Difficulty.UNKNOWN -> null
}
