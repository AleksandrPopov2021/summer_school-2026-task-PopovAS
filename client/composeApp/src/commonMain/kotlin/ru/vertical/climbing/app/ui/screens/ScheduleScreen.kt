@file:OptIn(ExperimentalMaterial3Api::class)

package ru.vertical.climbing.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import ru.vertical.climbing.app.navigation.ScheduleListComponent
import ru.vertical.climbing.app.ui.EmptyScheduleView
import ru.vertical.climbing.app.ui.UiTestTags
import ru.vertical.climbing.app.ui.ErrorView
import ru.vertical.climbing.app.ui.OfflineBanner
import ru.vertical.climbing.app.ui.format.chipLabel
import ru.vertical.climbing.app.ui.format.durationLabel
import ru.vertical.climbing.app.ui.format.timeLabel
import ru.vertical.climbing.domain.model.SlotAvailabilityState
import ru.vertical.climbing.domain.model.TrainingSlot
import ru.vertical.climbing.domain.model.availabilityState
import ru.vertical.climbing.domain.model.hidesBookButton
import ru.vertical.climbing.domain.model.isRebookingBlocked
import ru.vertical.climbing.presentation.Async
import ru.vertical.climbing.resources.Res
import ru.vertical.climbing.resources.action_book
import ru.vertical.climbing.resources.rebooking_forbidden_hint
import ru.vertical.climbing.resources.schedule_instructor_rating
import ru.vertical.climbing.resources.schedule_rental_from
import ru.vertical.climbing.resources.schedule_spots_left
import ru.vertical.climbing.resources.schedule_today
import ru.vertical.climbing.resources.title_schedule

/** SCR-003 Schedule Screen — расписание на 7 дней (LOGIC-003, LOGIC-004). */
@Composable
fun ScheduleScreen(component: ScheduleListComponent) {
    val model by component.model.subscribeAsState()
    val today = model.dates.firstOrNull()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.title_schedule)) }) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (model.isOffline) OfflineBanner()

            DateSwitcher(
                dates = model.dates,
                selectedDate = model.selectedDate,
                today = today,
                onDateSelected = component::onDateSelected,
            )

            when (val content = model.content) {
                Async.Idle, Async.Loading -> ScheduleSkeleton()
                is Async.Error -> ErrorView(error = content.error, onRetry = component::onRetry)
                Async.Empty -> PullToRefreshBox(
                    isRefreshing = model.isRefreshing,
                    onRefresh = component::onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    EmptyScheduleView(modifier = Modifier.fillMaxSize())
                }
                is Async.Content -> PullToRefreshBox(
                    isRefreshing = model.isRefreshing,
                    onRefresh = component::onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    SlotList(
                        slots = content.data,
                        rebookingForbiddenSlotIds = model.rebookingForbiddenSlotIds,
                        onSlotClicked = component::onSlotClicked,
                    )
                }
            }
        }
    }
}

@Composable
private fun DateSwitcher(
    dates: List<kotlinx.datetime.LocalDate>,
    selectedDate: kotlinx.datetime.LocalDate,
    today: kotlinx.datetime.LocalDate?,
    onDateSelected: (kotlinx.datetime.LocalDate) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(dates) { date ->
            FilterChip(
                selected = date == selectedDate,
                onClick = { onDateSelected(date) },
                label = {
                    Text(if (date == today) stringResource(Res.string.schedule_today) else date.chipLabel())
                },
            )
        }
    }
}

@Composable
private fun SlotList(
    slots: List<TrainingSlot>,
    rebookingForbiddenSlotIds: Set<String>,
    onSlotClicked: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(slots, key = { it.id }) { slot ->
            SlotCard(
                slot = slot,
                rebookingBlocked = slot.isRebookingBlocked(rebookingForbiddenSlotIds),
                onClick = { onSlotClicked(slot.id) },
            )
        }
    }
}

@Composable
private fun SlotCard(
    slot: TrainingSlot,
    rebookingBlocked: Boolean,
    onClick: () -> Unit,
) {
    val state = slot.availabilityState()
    val isCancelled = state == SlotAvailabilityState.CANCELLED

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().alpha(if (isCancelled) 0.6f else 1f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = slot.startsAt.timeLabel(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "~${durationLabel(slot.durationMinutes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconLabel(Icons.Filled.Terrain, slot.zone.name)
            InstructorRow(slot)

            Text(
                text = stringResource(Res.string.schedule_spots_left, slot.freeSpots, slot.capacity),
                style = MaterialTheme.typography.bodyMedium,
                color = state.statusColor(),
                fontWeight = FontWeight.Medium,
            )

            slot.rentalTariff?.let { tariff ->
                Text(
                    text = stringResource(Res.string.schedule_rental_from, tariff.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconLabel(Icons.Filled.LocationOn, slot.address, muted = true)

            SlotCardCta(state = state, rebookingBlocked = rebookingBlocked, onBook = onClick)
        }
    }
}

@Composable
private fun SlotCardCta(
    state: SlotAvailabilityState,
    rebookingBlocked: Boolean,
    onBook: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            rebookingBlocked -> Text(
                text = stringResource(Res.string.rebooking_forbidden_hint),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            state.hidesBookButton -> Text(
                text = state.statusLabel(),
                style = MaterialTheme.typography.labelLarge,
                color = state.statusColor(),
                fontWeight = FontWeight.Medium,
            )
            else -> Button(
                onClick = onBook,
                enabled = state == SlotAvailabilityState.AVAILABLE || state == SlotAvailabilityState.FEW_SPOTS,
                modifier = Modifier
                    .testTag(UiTestTags.SLOT_BOOK_BUTTON)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Записаться"
                    },
            ) {
                Text(stringResource(Res.string.action_book))
            }
        }
    }
}

@Composable
private fun InstructorRow(slot: TrainingSlot) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconLabel(Icons.Filled.Person, slot.instructor.fullName)
        slot.instructor.averageRating?.let { rating ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(Res.string.schedule_instructor_rating, rating),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun IconLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, muted: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = if (muted) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Skeleton-загрузка: несколько карточек-плейсхолдеров. */
@Composable
private fun ScheduleSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}
