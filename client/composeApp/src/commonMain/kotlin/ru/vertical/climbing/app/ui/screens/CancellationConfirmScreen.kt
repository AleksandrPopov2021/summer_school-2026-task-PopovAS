package ru.vertical.climbing.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import ru.vertical.climbing.app.navigation.CancellationConfirmComponent
import ru.vertical.climbing.app.ui.LoadingView
import ru.vertical.climbing.app.ui.UiTestTags
import ru.vertical.climbing.app.ui.errorMessageFor
import ru.vertical.climbing.app.ui.format.heroDateLabel
import ru.vertical.climbing.app.ui.format.timeLabel
import ru.vertical.climbing.domain.model.Booking
import ru.vertical.climbing.domain.model.CancellationWarningLevel
import ru.vertical.climbing.presentation.Async
import ru.vertical.climbing.resources.Res
import ru.vertical.climbing.resources.action_cancel
import ru.vertical.climbing.resources.action_confirm_cancel
import ru.vertical.climbing.resources.cancel_confirm_none
import ru.vertical.climbing.resources.cancel_forbidden_hint
import ru.vertical.climbing.resources.cancel_late_warning
import ru.vertical.climbing.resources.title_cancel_booking

/** SCR-008 — подтверждение отмены записи (LOGIC-008). */
@Composable
fun CancellationConfirmScreen(component: CancellationConfirmComponent) {
    val model by component.model.subscribeAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    model.snackbarError?.let { error ->
        val errorMessage = errorMessageFor(error)
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(errorMessage)
            component.onDismissSnackbar()
        }
    }

    Dialog(onDismissRequest = component::onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.title_cancel_booking),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                when (val content = model.content) {
                    Async.Idle, Async.Loading, Async.Empty -> LoadingView()
                    is Async.Error -> {
                        Text(errorMessageFor(content.error), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = component::onDismiss, modifier = Modifier.align(Alignment.End)) {
                            Text(stringResource(Res.string.action_cancel))
                        }
                    }
                    is Async.Content -> CancellationContent(
                        booking = content.data,
                        canConfirm = model.canConfirm,
                        isSubmitting = model.isSubmitting,
                        onConfirm = component::onConfirm,
                        onDismiss = component::onDismiss,
                    )
                }
            }
        }
    }

    SnackbarHost(hostState = snackbarHostState)
}

@Composable
private fun CancellationContent(
    booking: Booking,
    canConfirm: Boolean,
    isSubmitting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val slot = booking.slot
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "${slot.startsAt.heroDateLabel()} · ${slot.startsAt.timeLabel()}",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(slot.zone.name, style = MaterialTheme.typography.bodyMedium)

        CancellationWarning(policy = booking.cancellationPolicy?.warningLevel)

        Button(
            onClick = onConfirm,
            enabled = canConfirm && !isSubmitting,
            modifier = Modifier.fillMaxWidth().testTag(UiTestTags.CONFIRM_CANCEL),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
            }
            Text(stringResource(Res.string.action_confirm_cancel))
        }
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(Res.string.action_cancel))
        }
    }
}

@Composable
private fun CancellationWarning(policy: CancellationWarningLevel?) {
    val text = when (policy) {
        CancellationWarningLevel.LATE_CANCELLATION -> stringResource(Res.string.cancel_late_warning)
        CancellationWarningLevel.FORBIDDEN -> stringResource(Res.string.cancel_forbidden_hint)
        CancellationWarningLevel.NONE, CancellationWarningLevel.UNKNOWN, null ->
            stringResource(Res.string.cancel_confirm_none)
    }
    val showIcon = policy == CancellationWarningLevel.LATE_CANCELLATION
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (showIcon) {
            Icon(
                Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
