@file:OptIn(ExperimentalMaterial3Api::class)

package ru.vertical.climbing.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ru.vertical.climbing.app.navigation.NotificationSettingsComponent
import ru.vertical.climbing.app.ui.ErrorView
import ru.vertical.climbing.app.ui.LoadingView
import ru.vertical.climbing.app.ui.OfflineBanner
import ru.vertical.climbing.app.ui.errorMessageFor
import ru.vertical.climbing.domain.model.EditableNotificationToggles
import ru.vertical.climbing.domain.model.lockedGymCancellationEnabled
import ru.vertical.climbing.domain.model.lockedRemindersEnabled
import ru.vertical.climbing.domain.model.toEditableToggles
import ru.vertical.climbing.presentation.Async
import ru.vertical.climbing.resources.Res
import ru.vertical.climbing.resources.notification_locked_hint
import ru.vertical.climbing.resources.notification_save
import ru.vertical.climbing.resources.notification_saved
import ru.vertical.climbing.resources.notification_settings_booking_confirmation
import ru.vertical.climbing.resources.notification_settings_booking_confirmation_desc
import ru.vertical.climbing.resources.notification_settings_gym_cancel
import ru.vertical.climbing.resources.notification_settings_gym_cancel_desc
import ru.vertical.climbing.resources.notification_settings_rating
import ru.vertical.climbing.resources.notification_settings_rating_desc
import ru.vertical.climbing.resources.notification_settings_reminders
import ru.vertical.climbing.resources.notification_settings_reminders_desc
import ru.vertical.climbing.resources.notification_unsaved_discard
import ru.vertical.climbing.resources.notification_unsaved_message
import ru.vertical.climbing.resources.notification_unsaved_save
import ru.vertical.climbing.resources.notification_unsaved_title
import ru.vertical.climbing.resources.title_notification_settings

/** SCR-011 — настройки push-уведомлений (LOGIC-011, FR-032). */
@Composable
fun NotificationSettingsScreen(component: NotificationSettingsComponent) {
    val model by component.model.subscribeAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lockedHint = stringResource(Res.string.notification_locked_hint)

    val snackbarError = model.snackbarError
    val snackbarErrorMessage = snackbarError?.let { errorMessageFor(it) }
    LaunchedEffect(snackbarError) {
        if (snackbarError != null && snackbarErrorMessage != null) {
            snackbarHostState.showSnackbar(snackbarErrorMessage)
            component.onDismissSnackbar()
        }
    }

    val savedMessage = stringResource(Res.string.notification_saved)
    LaunchedEffect(model.showSuccessSnackbar) {
        if (model.showSuccessSnackbar) {
            snackbarHostState.showSnackbar(savedMessage)
            component.onDismissSuccessSnackbar()
        }
    }

    if (model.showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = component::onBackDialogDismissed,
            title = { Text(stringResource(Res.string.notification_unsaved_title)) },
            text = { Text(stringResource(Res.string.notification_unsaved_message)) },
            confirmButton = {
                TextButton(onClick = component::onBackSaveConfirmed) {
                    Text(stringResource(Res.string.notification_unsaved_save))
                }
            },
            dismissButton = {
                TextButton(onClick = component::onBackDiscardConfirmed) {
                    Text(stringResource(Res.string.notification_unsaved_discard))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_notification_settings)) },
                navigationIcon = {
                    IconButton(onClick = component::onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (model.content is Async.Content) {
                Button(
                    onClick = component::onSaveClicked,
                    enabled = model.canSave,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    if (model.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 8.dp))
                    }
                    Text(stringResource(Res.string.notification_save))
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (model.isOffline) OfflineBanner()
            when (val content = model.content) {
                Async.Idle, Async.Loading -> LoadingView()
                is Async.Error -> ErrorView(
                    error = content.error,
                    onRetry = component::onRetry,
                )
                is Async.Content -> {
                    val toggles = model.localToggles ?: content.data.toEditableToggles()
                    NotificationSettingsContent(
                        local = toggles,
                        isSaving = model.isSaving,
                        onBookingConfirmationToggled = component::onBookingConfirmationToggled,
                        onRatingInvitationToggled = component::onRatingInvitationToggled,
                        onLockedToggleClicked = {
                            scope.launch { snackbarHostState.showSnackbar(lockedHint) }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Async.Empty -> LoadingView()
            }
        }
    }
}

@Composable
private fun NotificationSettingsContent(
    local: EditableNotificationToggles,
    isSaving: Boolean,
    onBookingConfirmationToggled: (Boolean) -> Unit,
    onRatingInvitationToggled: (Boolean) -> Unit,
    onLockedToggleClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        EditableToggleRow(
            title = stringResource(Res.string.notification_settings_booking_confirmation),
            description = stringResource(Res.string.notification_settings_booking_confirmation_desc),
            checked = local.bookingConfirmationEnabled,
            enabled = !isSaving,
            onCheckedChange = onBookingConfirmationToggled,
        )
        HorizontalDivider()
        EditableToggleRow(
            title = stringResource(Res.string.notification_settings_rating),
            description = stringResource(Res.string.notification_settings_rating_desc),
            checked = local.ratingInvitationEnabled,
            enabled = !isSaving,
            onCheckedChange = onRatingInvitationToggled,
        )
        HorizontalDivider()
        LockedToggleRow(
            title = stringResource(Res.string.notification_settings_reminders),
            description = stringResource(Res.string.notification_settings_reminders_desc),
            checked = lockedRemindersEnabled(),
            onLockedToggleClicked = onLockedToggleClicked,
        )
        HorizontalDivider()
        LockedToggleRow(
            title = stringResource(Res.string.notification_settings_gym_cancel),
            description = stringResource(Res.string.notification_settings_gym_cancel_desc),
            checked = lockedGymCancellationEnabled(),
            onLockedToggleClicked = onLockedToggleClicked,
        )
    }
}

@Composable
private fun EditableToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    badge: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                badge?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun LockedToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onLockedToggleClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLockedToggleClicked)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = {}, enabled = false)
    }
}
