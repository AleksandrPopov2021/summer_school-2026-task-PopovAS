@file:OptIn(ExperimentalMaterial3Api::class)

package ru.vertical.climbing.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import ru.vertical.climbing.app.navigation.PushNotificationComponent
import ru.vertical.climbing.app.navigation.PushNotificationVariant
import ru.vertical.climbing.app.navigation.toVariant
import ru.vertical.climbing.domain.model.PushNotificationPayload
import ru.vertical.climbing.resources.Res
import ru.vertical.climbing.resources.action_later
import ru.vertical.climbing.resources.gym_cancel_apology
import ru.vertical.climbing.resources.push_action_book_alternative
import ru.vertical.climbing.resources.push_action_booking_details
import ru.vertical.climbing.resources.push_action_my_bookings
import ru.vertical.climbing.resources.push_action_rate
import ru.vertical.climbing.resources.push_invalid_message
import ru.vertical.climbing.resources.push_label_address
import ru.vertical.climbing.resources.push_label_instructor
import ru.vertical.climbing.resources.push_label_reason
import ru.vertical.climbing.resources.push_label_zone
import ru.vertical.climbing.resources.push_title_booking_confirmed
import ru.vertical.climbing.resources.push_title_gym_cancel
import ru.vertical.climbing.resources.push_title_invalid
import ru.vertical.climbing.resources.push_title_rating
import ru.vertical.climbing.resources.push_title_reminder

/** SCR-014 — landing push-уведомления (LOGIC-013). */
@Composable
fun PushNotificationScreen(component: PushNotificationComponent) {
    val model by component.model.subscribeAsState()
    val payload = model.payload
    val variant = if (model.isInvalid) PushNotificationVariant.INVALID else payload.toVariant()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes(variant))) },
                actions = {
                    TextButton(onClick = component::onDismiss) {
                        Text(stringResource(Res.string.action_later))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = iconFor(variant),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(64.dp),
            )
            if (model.isInvalid) {
                Text(
                    text = stringResource(Res.string.push_invalid_message),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            } else {
                PayloadDetails(payload = payload, variant = variant)
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = component::onPrimaryAction,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(primaryActionRes(variant)))
            }
            OutlinedButton(
                onClick = component::onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.action_later))
            }
        }
    }
}

@Composable
private fun PayloadDetails(payload: PushNotificationPayload, variant: PushNotificationVariant) {
    val slot = payload.slot
    slot?.startsAt?.let {
        Text(text = it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
    slot?.zoneFormat?.let {
        InfoLine(label = stringResource(Res.string.push_label_zone), value = it)
    }
    if (variant == PushNotificationVariant.REMINDER) {
        slot?.address?.let {
            InfoLine(label = stringResource(Res.string.push_label_address), value = it)
        }
    }
    slot?.instructorName?.let {
        InfoLine(label = stringResource(Res.string.push_label_instructor), value = it)
    }
    if (variant == PushNotificationVariant.GYM_CANCELLATION) {
        payload.cancellationReason?.let {
            InfoLine(label = stringResource(Res.string.push_label_reason), value = it)
        }
        Text(
            text = stringResource(Res.string.gym_cancel_apology),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun titleRes(variant: PushNotificationVariant) = when (variant) {
    PushNotificationVariant.BOOKING_CONFIRMED -> Res.string.push_title_booking_confirmed
    PushNotificationVariant.REMINDER -> Res.string.push_title_reminder
    PushNotificationVariant.GYM_CANCELLATION -> Res.string.push_title_gym_cancel
    PushNotificationVariant.RATING_INVITATION -> Res.string.push_title_rating
    PushNotificationVariant.INVALID -> Res.string.push_title_invalid
}

private fun primaryActionRes(variant: PushNotificationVariant) = when (variant) {
    PushNotificationVariant.BOOKING_CONFIRMED -> Res.string.push_action_my_bookings
    PushNotificationVariant.REMINDER -> Res.string.push_action_booking_details
    PushNotificationVariant.GYM_CANCELLATION -> Res.string.push_action_book_alternative
    PushNotificationVariant.RATING_INVITATION -> Res.string.push_action_rate
    PushNotificationVariant.INVALID -> Res.string.push_action_my_bookings
}

private fun iconFor(variant: PushNotificationVariant): ImageVector = when (variant) {
    PushNotificationVariant.BOOKING_CONFIRMED -> Icons.Filled.CheckCircle
    PushNotificationVariant.REMINDER -> Icons.Filled.Notifications
    PushNotificationVariant.GYM_CANCELLATION -> Icons.Filled.Warning
    PushNotificationVariant.RATING_INVITATION -> Icons.Filled.Star
    PushNotificationVariant.INVALID -> Icons.Filled.Warning
}
