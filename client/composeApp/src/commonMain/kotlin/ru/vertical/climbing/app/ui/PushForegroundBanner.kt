package ru.vertical.climbing.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource
import ru.vertical.climbing.domain.model.PushNotificationPayload
import ru.vertical.climbing.push.PushDeliverySource
import ru.vertical.climbing.push.PushNavigationEvent
import ru.vertical.climbing.push.PushNotificationCenter
import ru.vertical.climbing.resources.Res
import ru.vertical.climbing.resources.push_foreground_banner

/** Foreground in-app banner при получении push (LOGIC-013). */
@Composable
fun PushForegroundBannerHost(
    onBannerTapped: (PushNotificationPayload) -> Unit,
) {
    var bannerPayload by remember { mutableStateOf<PushNotificationPayload?>(null) }

    LaunchedEffect(Unit) {
        PushNotificationCenter.events.collectLatest { event ->
            if (event.source == PushDeliverySource.FOREGROUND && event.showPreview && event.payload != null) {
                bannerPayload = event.payload
            }
        }
    }

    bannerPayload?.let { payload ->
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        bannerPayload = null
                        onBannerTapped(payload)
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Text(
                    text = stringResource(Res.string.push_foreground_banner),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

/** Публикует cold start событие до подписки MainComponent. */
fun publishInitialPushEvent(event: PushNavigationEvent?) {
    if (event != null) {
        PushNotificationCenter.setColdStart(event)
    }
}
